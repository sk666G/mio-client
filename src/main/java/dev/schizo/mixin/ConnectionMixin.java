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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
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

   @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
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
            ClientAccess.logger.warn("Packet receive event handler failed", eventFailure);
         }
      }
   }

   @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
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
            ClientAccess.logger.warn("Packet send event handler failed", eventFailure);
         }
      }
   }

   @Inject(method = "doSendPacket", at = @At("TAIL"))
   private void onPacketPostSend(
      Packet packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback
   ) {
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         try {
            PacketEvent.PostSend event = new PacketEvent.PostSend(packet);
            ClientAccess.eventBus
               .post(event);
         } catch (Exception eventFailure) {
            ClientAccess.logger.warn("Packet post-send event handler failed", eventFailure);
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
