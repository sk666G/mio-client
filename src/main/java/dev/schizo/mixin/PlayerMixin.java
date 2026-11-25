/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.bootstrap.MixinModuleRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.misc.SocialEntry;
import dev.schizo.client.managers.PlayerTickExecutor;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.utils.entity.PlayerState;
import dev.schizo.client.utils.MonotonicTime;
import java.util.concurrent.TimeUnit;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.settings.options.SocialRole;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.commands.FakeplayerCommand;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.movement.NoSlowModule;
import dev.schizo.client.events.PlayerAttackEvent;
import dev.schizo.client.events.PlayerIsStayingOnGroundSurfaceEvent;
import dev.schizo.client.modules.exploit.ReachModule;
import dev.schizo.client.modules.player.ScaffoldModule;
import dev.schizo.client.modules.movement.SprintModule;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerState {
   @Unique
   private long lastCrawlingAtNanos;
   @Unique
   private long lastEatingAtNanos;
   @Unique
   private SocialRole socialRole;
   @Unique
   private boolean nextToWall;
   @Unique
   private int lastAttackedTicksServer;
   @Unique
   private FloatAnimation healthAnimation;

   @Shadow
   public abstract Abilities getAbilities();

   protected PlayerMixin(EntityType entityType, Level level) {
      super(entityType, level);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void initializePlayerState(
      Level level, GameProfile profile, CallbackInfo callback
   ) {
      this.healthAnimation = new FloatAnimation();
      this.healthAnimation
         .setValue(
            EntityUtils.getHealthFraction(this)
         );

      for (Object entry : (List)ClientContext.socialRegistry.getRegistry()) {
         SocialEntry socialEntry = (SocialEntry)entry;
         if (profile.name().equalsIgnoreCase(socialEntry.getName())) {
            this.schizo$setRole(socialEntry.role());
            break;
         }
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void updatePlayerState(CallbackInfo callback) {
      this.healthAnimation
         .setTarget(
            EntityUtils.getHealthFraction(this), 100L
         );
      if (this.getBoundingBox().getYsize() < 1.0) {
         this.lastCrawlingAtNanos = System.nanoTime();
      }

      this.nextToWall = !this.level()
         .noCollision(this.getBoundingBox().inflate(1.0E-4, 0.0, 1.0E-4));
      if (!this.isUsingItem()
         || this.getItemUseProgress(this.getUseItem(), this) > 1.0F) {
         this.lastEatingAtNanos = System.nanoTime();
      }
   }

   @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
   private void onIsStayingOnGroundSurface(CallbackInfoReturnable<Boolean> callback) {
      if (ClientAccess.schizoMinecraft.player != null) {
         PlayerIsStayingOnGroundSurfaceEvent event = new PlayerIsStayingOnGroundSurfaceEvent();
         ClientAccess.eventBus
            .post(event);
         if (event.isCancelled()) {
            callback.setReturnValue(true);
         }
      }
   }

   @ModifyExpressionValue(method = "maybeBackOffFromEdge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;maxUpStep()F"))
   private float modifySafeWalkStepHeight(float originalHeight) {
      if (ClientAccess.schizoMinecraft.player == null) {
         return originalHeight;
      }

      if (!MixinModuleRegistry.playerScaffoldModule.isToggled() || !MixinModuleRegistry.playerScaffoldModule.safeWalkSetting.getValue()) {
         return originalHeight;
      }
      Item heldItem = ClientAccess.schizoMinecraft.player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
      if (!(heldItem instanceof BlockItem blockItem)) {
         return originalHeight;
      }
      VoxelShape collisionShape = blockItem.getBlock().defaultBlockState().getCollisionShape(
         ClientAccess.schizoMinecraft.level, BlockPos.ZERO
      );
      return collisionShape != null && !collisionShape.isEmpty() ? (float)collisionShape.bounds().getYsize() : originalHeight;
   }

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   public void onAttackPre(Entity target, CallbackInfo callback) {
      PlayerAttackEvent event = new PlayerAttackEvent(EventStage.Pre, target);
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   @Inject(method = "attack", at = @At("TAIL"))
   private void onAttackPost(Entity attackedEntity, CallbackInfo callback) {
      PlayerAttackEvent event = new PlayerAttackEvent(EventStage.Post, attackedEntity);
      ClientAccess.eventBus
         .post(event);
   }

   @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
   private boolean allowAttackMovementSlowdown(Player player, Vec3 movement) {
      return !MixinModuleRegistry.playerSprintModule.isToggled()
         || !MixinModuleRegistry.playerSprintModule.keepSetting.getValue();
   }

   @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
   private boolean allowAttackSprintReset(Player player, boolean sprinting) {
      return !MixinModuleRegistry.playerSprintModule.isToggled()
         || !MixinModuleRegistry.playerSprintModule.keepSetting.getValue();
   }

   @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
   private boolean allowSimulatedPlayerKnockback(
      LivingEntity entity, double strength, double directionX, double directionZ
   ) {
      return !((Object)this instanceof FakeplayerCommand.SimulatedPlayer)
         || entity != ClientAccess.schizoMinecraft.player;
   }

   @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
   private void overrideEntityInteractionRange(CallbackInfoReturnable<Double> callback) {
      if (MixinModuleRegistry.playerReachModule.isToggled()) {
         callback.setReturnValue(
            (MixinModuleRegistry.playerReachModule.modifierSetting.getValue())
               .doubleValue()
         );
      }
   }

   @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
   private void overrideBlockInteractionRange(CallbackInfoReturnable<Double> callback) {
      if (MixinModuleRegistry.playerReachModule.isToggled()) {
         callback.setReturnValue(
            (MixinModuleRegistry.playerReachModule.modifierSetting.getValue())
               .doubleValue()
         );
      }
   }

   @Inject(method = "isSecondaryUseActive", at = @At("HEAD"), cancellable = true)
   private void forceSecondaryUseWhilePlacing(CallbackInfoReturnable<Boolean> callback) {
      if (BlockInteractionUtils.isPlacingBlock) {
         callback.setReturnValue(true);
      }
   }

   @Inject(method = "resetOnlyAttackStrengthTicker", at = @At("HEAD"))
   private void resetServerAttackTicks(CallbackInfo callback) {
      this.schizo$resetLastAttackedServer();
   }

   @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;attackStrengthTicker:I", ordinal = 0, opcode = 180))
   private void incrementServerAttackTicks(CallbackInfo callback) {
      if (!PlayerTickExecutor.isRunningExtraTick()) {
         this.lastAttackedTicksServer++;
      }
   }

   @ModifyReturnValue(method = "getSpeed", at = @At("RETURN"))
   private float applyNoSlowWalkingSpeed(float originalSpeed) {
      if (!this.level().isClientSide()) {
         return originalSpeed;
      }

      if (!MixinModuleRegistry.playerNoSlowModule.isToggled() || !MixinModuleRegistry.playerNoSlowModule.slownessSetting.getValue()) {
         return originalSpeed;
      }
      float walkingSpeed = this.getAbilities().getWalkingSpeed();
      if (originalSpeed >= walkingSpeed) {
         return originalSpeed;
      }
      return this.isSprinting() ? walkingSpeed * 1.3000001F : walkingSpeed;
   }

   @Override
   public long schizo$getLastEatingTimeNanos() {
      return this.lastEatingAtNanos;
   }

   @Override
   public SocialRole schizo$getRole() {
      return this.socialRole;
   }

   @Override
   public void schizo$setRole(SocialRole role) {
      this.socialRole = role;
   }

   @Override
   public float schizo$getHealthFactor() {
      return this.healthAnimation
         .getValue();
   }

   @Override
   public boolean schizo$isNextToWall() {
      return this.nextToWall;
   }

   @Override
   public boolean schizo$wasCrawling() {
      return this.lastCrawlingAtNanos != 0L
         && MonotonicTime.isWithin(
            this.lastCrawlingAtNanos,
            System.nanoTime(),
            TimeUnit.MILLISECONDS.toNanos(150L)
         );
   }

   @Override
   public int schizo$getLastAttackedTicksServer() {
      return this.lastAttackedTicksServer;
   }

   @Override
   public void schizo$resetLastAttackedServer() {
      this.lastAttackedTicksServer = 0;
   }

}
