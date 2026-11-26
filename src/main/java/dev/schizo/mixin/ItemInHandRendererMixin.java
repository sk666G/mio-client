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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.utils.inventory.ItemUtils;
import dev.schizo.client.modules.combat.NoHitDelayModule;
import dev.schizo.client.modules.misc.SwingModule;
import dev.schizo.client.modules.render.ViewModelModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = ItemInHandRenderer.class, priority = 9999)
public abstract class ItemInHandRendererMixin {
   @Unique
   private static final Vector3f blockingRotation = new Vector3f(
      120.0F, 15.0F, 100.0F
   );
   @Unique
   private static final Vector3f blockingTranslation = new Vector3f(
      0.15F, 0.05F, 0.0F
   );
   @Unique
   private final Vector3f itemRotation = new Vector3f(1.0F);
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   private float mainHandHeight;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private ItemStack offHandItem;
   @Shadow
   private float offHandHeight;

   @Inject(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )
   )
   private void applyViewModelItemTransform(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack itemStack,
      float equipProgress,
      PoseStack pose,
      SubmitNodeCollector submitNodes,
      int light,
      CallbackInfo callback
   ) {
      boolean blockingOffsets = false;
      float offsetX = 0.0F;
      float offsetY = 0.0F;
      float offsetZ = 0.0F;
      this.itemRotation.set(0.0F);
      if (MixinModuleRegistry.minecraftNoHitDelayModule.isToggled()
         && MixinModuleRegistry.minecraftNoHitDelayModule.hasUseKeyDelayElapsed()
         && hand == InteractionHand.MAIN_HAND
         && (
            !ClientAccess.schizoMinecraft.player.isUsingItem()
               || !(
                  ClientAccess.schizoMinecraft
                     .player
                     .getOffhandItem()
                     .getItem() instanceof ShieldItem
               )
         )
         && ItemUtils.isSword(ClientAccess.schizoMinecraft.player.getMainHandItem())) {
         if (ClientAccess.schizoMinecraft.player.getMainArm()
            == HumanoidArm.LEFT) {
            this.itemRotation
               .set(
                  this.mirrorBlockingRotationInPlace(
                     blockingRotation
                  )
               );
            offsetX += blockingTranslation.x;
         } else {
            this.itemRotation
               .set(blockingRotation);
            offsetX -= blockingTranslation.x;
         }

         offsetY += blockingTranslation.y;
         blockingOffsets = true;
      }

      if (MixinModuleRegistry.gameViewModel.isToggled()) {
         boolean consumable = itemStack.has(DataComponents.FOOD) || itemStack.getItem() instanceof PotionItem;
         boolean activeItemMatches = ClientAccess.schizoMinecraft
               .player
               .getUseItem()
               .getItem()
            == itemStack.getItem();
         if (consumable
            && activeItemMatches
            && ClientAccess.schizoMinecraft.player.isUsingItem()
            && !MixinModuleRegistry.gameViewModel.eatingSetting.getValue()) {
            return;
         }

         if (hand == InteractionHand.MAIN_HAND) {
            offsetX += consumable && activeItemMatches
               ? 0.0F
               : -MixinModuleRegistry.gameViewModel.mainXSetting.getValue();
            offsetY += MixinModuleRegistry.gameViewModel.mainYSetting.getValue();
            offsetZ = consumable && activeItemMatches
               ? 0.0F
               : MixinModuleRegistry.gameViewModel.mainZSetting.getValue();
            if (!blockingOffsets) {
               this.itemRotation
                  .set(
                     MixinModuleRegistry.gameViewModel.mainRotateXSetting.getValue(),
                     MixinModuleRegistry.gameViewModel.mainRotateYSetting.getValue(),
                     MixinModuleRegistry.gameViewModel.mainRotateZSetting.getValue()
                  );
            }
         } else {
            offsetX += consumable && activeItemMatches
               ? 0.0F
               : MixinModuleRegistry.gameViewModel.offXSetting.getValue();
            offsetY = MixinModuleRegistry.gameViewModel.offYSetting.getValue();
            offsetZ = consumable && activeItemMatches
               ? 0.0F
               : MixinModuleRegistry.gameViewModel.offZSetting.getValue();
            this.itemRotation
               .set(
                  MixinModuleRegistry.gameViewModel.offRotateXSetting.getValue(),
                  MixinModuleRegistry.gameViewModel.offRotateYSetting.getValue(),
                  MixinModuleRegistry.gameViewModel.offRotateZSetting.getValue()
               );
         }

         Vector3f scale = MixinModuleRegistry.gameViewModel.getScale(hand);
         pose.scale(scale.x, scale.y, scale.z);
      }

      if (ClientAccess.schizoMinecraft.player.getMainArm()
         == HumanoidArm.LEFT) {
         offsetX = -offsetX;
      }

      pose.translate(offsetX, offsetY, offsetZ);
      pose.mulPose(
         Axis.XN.rotationDegrees(this.itemRotation.x)
      );
      pose.mulPose(
         Axis.YP.rotationDegrees(this.itemRotation.y)
      );
      pose.mulPose(
         Axis.ZP.rotationDegrees(this.itemRotation.z)
      );
   }

   @Inject(method = "renderPlayerArm", at = @At("HEAD"))
   private void applyViewModelArmTransform(
      PoseStack pose, SubmitNodeCollector submitNodes, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo callback
   ) {
      if (MixinModuleRegistry.gameViewModel.isToggled()
         && MixinModuleRegistry.gameViewModel.armSetting.getValue()) {
         float offsetX = -MixinModuleRegistry.gameViewModel.mainXSetting.getValue();
         if (ClientAccess.schizoMinecraft.player.getMainArm()
            == HumanoidArm.LEFT) {
            offsetX = -offsetX;
         }

         pose.translate(
            offsetX,
            MixinModuleRegistry.gameViewModel.mainYSetting.getValue(),
            MixinModuleRegistry.gameViewModel.mainZSetting.getValue()
         );
         pose.scale(
            MixinModuleRegistry.gameViewModel.mainScaleXSetting.getValue(),
            MixinModuleRegistry.gameViewModel.mainScaleYSetting.getValue(),
            MixinModuleRegistry.gameViewModel.mainScaleZSetting.getValue()
         );
      }
   }

   @ModifyArgs(
      method = "renderHandsWithItems",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      ),
      require = 0
   )
   private void overrideSwingProgress(Args arguments) {
      InteractionHand hand = arguments.get(3);
      float originalProgress = arguments.<Float>get(4);
      arguments.set(
         4,
         MixinModuleRegistry.gameViewModel.applySwingProgressOverride(
            hand, originalProgress
         )
      );
   }

   @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
   private void hideOffhandShieldForNoHitDelay(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      SubmitNodeCollector collector,
      int packedLight,
      CallbackInfo callback
   ) {
      if (MixinModuleRegistry.minecraftNoHitDelayModule.isToggled()
         && hand == InteractionHand.OFF_HAND
         && stack.is(Items.SHIELD)) {
         callback.cancel();
      }
   }

   @WrapWithCondition(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
   private boolean shouldApplyHandSway(PoseStack pose, Quaternionfc rotation) {
      return !MixinModuleRegistry.gameViewModel.isToggled()
         || !MixinModuleRegistry.gameViewModel.noSwaySetting.getValue();
   }

   @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2))
   private float overrideMainHandEquipStep(float originalStep) {
      if (MixinModuleRegistry.livingEntitySwing.isToggled()
         && MixinModuleRegistry.livingEntitySwing.typeSetting.getValue()
            != SwingModule.TypeChoice.VANILLA) {
         boolean legacySwing = MixinModuleRegistry.livingEntitySwing.typeSetting.getValue()
            == SwingModule.TypeChoice.ONE_EIGHT;
         float attackStrength = this.minecraft.player.getAttackStrengthScale(1.0F);
         if (legacySwing) {
            attackStrength = 1.0F;
         }

         boolean itemChanged = !ItemStack.isSameItemSameComponents(
            this.mainHandItem,
            ClientAccess.schizoMinecraft.player.getMainHandItem()
         );
         float minimumStep = legacySwing ? -0.6F : -0.4F;
         return Mth.clamp((itemChanged ? 0.0F : attackStrength * attackStrength * attackStrength) - this.mainHandHeight, minimumStep, 0.4F);
      } else {
         return originalStep;
      }
   }

   @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 3))
   private float overrideOffHandEquipStep(float originalStep) {
      if (MixinModuleRegistry.livingEntitySwing.isToggled()
         && MixinModuleRegistry.livingEntitySwing.typeSetting.getValue()
            != SwingModule.TypeChoice.VANILLA) {
         boolean itemChanged = !ItemStack.isSameItemSameComponents(
            this.offHandItem,
            ClientAccess.schizoMinecraft.player.getOffhandItem()
         );
         if (MixinModuleRegistry.livingEntitySwing.typeSetting.getValue()
               == SwingModule.TypeChoice.ONE_TWELVE
            && MixinModuleRegistry.livingEntitySwing.shakeSetting.getValue()
            && MixinModuleRegistry.livingEntitySwing.consumeShakeTick()) {
            itemChanged = true;
         }

         return Mth.clamp((itemChanged ? 0 : 1) - this.offHandHeight, -0.4F, 0.4F);
      } else {
         return originalStep;
      }
   }

   @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isAutoSpinAttack()Z"))
   private boolean overrideTridentAnimation(boolean originalSpinAttack) {
      return MixinModuleRegistry.gameViewModel.isToggled()
            && MixinModuleRegistry.gameViewModel.noTridentAnimSetting.getValue()
         ? false
         : originalSpinAttack;
   }

   @ModifyArgs(method = "applyItemArmTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
   private void overrideInstantSwapOffset(
      Args arguments, @Local(argsOnly = true) HumanoidArm arm
   ) {
      if (MixinModuleRegistry.gameViewModel.isToggled()
         && MixinModuleRegistry.gameViewModel.instantSwapSetting.getValue()
         && !ClientAccess.schizoMinecraft
            .player
            .getItemHeldByArm(arm)
            .is(Items.SHIELD)) {
         arguments.set(1, -0.52F);
      }
   }

   @ModifyArgs(method = "applyEatTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
   private void scaleEatingOffset(Args arguments) {
      if (MixinModuleRegistry.gameViewModel.isToggled()) {
         arguments.set(
            1,
            arguments.<Float>get(1)
               * MixinModuleRegistry.gameViewModel.eatMultiplierSetting.getValue()
         );
      }
   }

   @Unique
   private Vector3f mirrorBlockingRotationInPlace(Vector3f rotation) {
      return rotation.set(rotation.x, -rotation.y, -rotation.z);
   }
}
