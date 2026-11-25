/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.events.ClientPacketListenerHandleExplosionEvent;
import dev.schizo.client.events.ClientPacketListenerHandleOpenSignEditorEvent;
import dev.schizo.client.events.ClientPacketListenerHandleSetEntityMotionEvent;
import dev.schizo.client.events.BlockUpdateAppliedEvent;
import dev.schizo.client.events.SelectedSlotChangeEvent;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

   @Inject(
      method = "handleExplosion",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   private void handleExplosionMotion(ClientboundExplodePacket packet, CallbackInfo callback) {
      ClientPacketListenerHandleExplosionEvent event = new ClientPacketListenerHandleExplosionEvent(packet);
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         callback.cancel();
      } else {
         ClientboundExplodePacketAccessor packetAccess = (ClientboundExplodePacketAccessor)(Object)packet;
         Vec3 eventVelocity = new Vec3(
            event.getVelocityX(),
            event.getVelocityY(),
            event.getVelocityZ()
         );
         packetAccess.setVelocity(Optional.of(eventVelocity));
      }
   }

   @Inject(
      method = "handleSetEntityMotion",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   private void handleEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo callback) {
      ClientPacketListenerHandleSetEntityMotionEvent event = new ClientPacketListenerHandleSetEntityMotionEvent(packet);
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         callback.cancel();
      } else if (event.isVelocityModified()) {
         ClientboundSetEntityMotionPacketAccessor packetAccess = (ClientboundSetEntityMotionPacketAccessor)packet;
         packetAccess.setVelocity(
            new Vec3(
               event.getVelocityX(),
               event.getVelocityY(),
               event.getVelocityZ()
            )
         );
      }
   }

   @Inject(
      method = "handleAddEntity",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   private void suppressRemovedEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo callback) {
      if (MixinModuleRegistry.levelNoRenderModule.isToggled()) {
         if (packet != null
            && packet.getType() != null
            && MixinModuleRegistry.levelNoRenderModule.shouldHideEntityType(
               packet.getType()
            )
            && MixinModuleRegistry.levelNoRenderModule.removalSetting.getValue()
               == dev.schizo.client.modules.render.NoRenderModule.RemovalChoice.FULL) {
            callback.cancel();
         }
      }
   }

   @Inject(
      method = "handleSetHeldSlot",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   private void onHandleSetHeldSlot(ClientboundSetHeldSlotPacket packet, CallbackInfo callback) {
      SelectedSlotChangeEvent.ClientPacketListenerHandleSetHeldSlotEvent slotEvent = new SelectedSlotChangeEvent.ClientPacketListenerHandleSetHeldSlotEvent(packet.slot());
      ClientAccess.eventBus
         .post(slotEvent);
      if (slotEvent.isCancelled()) {
         callback.cancel();
      }
   }

   @Inject(method = {"handleBlockUpdate", "handleChunkBlocksUpdate"}, at = @At("TAIL"))
   private void onBlockUpdatesApplied(CallbackInfo callback) {
      ClientAccess.eventBus
         .post(new BlockUpdateAppliedEvent());
   }

   @Inject(
      method = "handleOpenSignEditor",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;openTextEdit(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Z)V", shift = Shift.BEFORE),
      cancellable = true
   )
   private void onOpenSignEditor(
      ClientboundOpenSignEditorPacket packet, CallbackInfo callback, @Local SignBlockEntity sign
   ) {
      ClientPacketListenerHandleOpenSignEditorEvent event = new ClientPacketListenerHandleOpenSignEditorEvent(packet.getPos(), sign, packet.isFrontText());
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showNetworkCharts()Z"))
   private boolean shouldShowNetworkCharts(boolean originalVisible) {
      return MixinModuleRegistry.clientPacketFastLatencyModule.isVanillaPingEnabled()
         ? false
         : originalVisible;
   }
}
