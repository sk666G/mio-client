/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.utils.network.PingSpoofDelayHandler;
import dev.schizo.client.utils.FailureLogGate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
   private static final long FAILURE_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(2L);
   private static final FailureLogGate RECEIVE_FAILURE_LOG = new FailureLogGate(FAILURE_LOG_INTERVAL_NANOS);
   private static final FailureLogGate SEND_FAILURE_LOG = new FailureLogGate(FAILURE_LOG_INTERVAL_NANOS);
   private static final FailureLogGate POST_SEND_FAILURE_LOG = new FailureLogGate(FAILURE_LOG_INTERVAL_NANOS);

   private static void logEventFailure(String phase, Exception failure, FailureLogGate gate) {
      FailureLogGate.Decision decision = gate.acquire(System.nanoTime());
      if (!decision.shouldLog()) {
         return;
      }
      long suppressed = decision.suppressedSinceLastLog();
      if (suppressed == 0L) {
         ClientAccess.logger.warn("Packet {} event handler failed", phase, failure);
      } else {
         ClientAccess.logger.warn(
            "Packet {} event handler failed ({} similar failures suppressed since the last log)",
            phase, suppressed, failure
         );
      }
   }

   @Shadow
   private Channel channel;
   @Shadow
   @Final
   private PacketFlow receiving;

   @Inject(method = "channelActive", at = @At("TAIL"))
   private void installPingSpoofDelayHandler(ChannelHandlerContext context, CallbackInfo callback) {
      if (this.receiving != PacketFlow.CLIENTBOUND) {
         return;
      }
      ChannelPipeline pipeline = context.pipeline();
      if (pipeline.get(PingSpoofDelayHandler.NAME) == null) {
         pipeline.addFirst(PingSpoofDelayHandler.NAME, new PingSpoofDelayHandler(
            MixinModuleRegistry.connectionPingSpoofModule, () -> ((Connection)(Object)this).getPacketListener() instanceof ClientGamePacketListener
         ));
      }
   }

   @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true, require = 1)
   public void onPacketReceive(
      ChannelHandlerContext context, Packet packet, CallbackInfo callback
   ) {
      if (this.channel.isOpen() && packet != null) {
         try {
            PacketEvent.Receive event = new PacketEvent.Receive(packet);
            ClientAccess.eventBus
               .post(event);
            if (event.isCancelled()) {
               callback.cancel();
            }
         } catch (Exception eventFailure) {
            logEventFailure("receive", eventFailure, RECEIVE_FAILURE_LOG);
         }
      }
   }

   @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true, require = 1)
   private void onPacketSend(
      Packet packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback
   ) {
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         try {
            PacketEvent.Send event = new PacketEvent.Send(packet);
            ClientAccess.eventBus
               .post(event);
            if (event.isCancelled()) {
               callback.cancel();
            }
         } catch (Exception eventFailure) {
            logEventFailure("send", eventFailure, SEND_FAILURE_LOG);
         }
      }
   }

   @Inject(method = "doSendPacket", at = @At("TAIL"), require = 1)
   private void onPacketPostSend(
      Packet packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback
   ) {
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         try {
            PacketEvent.PostSend event = new PacketEvent.PostSend(packet);
            ClientAccess.eventBus
               .post(event);
         } catch (Exception eventFailure) {
            logEventFailure("post-send", eventFailure, POST_SEND_FAILURE_LOG);
         }
      }
   }

   @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
   private void onConnectionException(
      ChannelHandlerContext context, Throwable failure, CallbackInfo callback
   ) {
      if (MixinModuleRegistry.connectionNoPacketKickModule.isToggled()) {
         MixinModuleRegistry.connectionNoPacketKickModule.logInterceptedException(failure);
         callback.cancel();
      }
   }
}
