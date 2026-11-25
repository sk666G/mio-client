/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.utils.math.MathConstants;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.HudRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.render.RenderTargetUtils;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.utils.combat.DamageUtils;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.PostProcessShaderPass;
import dev.schizo.client.utils.inventory.EquipmentUtils;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.client.ColorsModule;
import dev.schizo.client.modules.client.FontsModule;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.ColorsSchemeChoice;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ArmorModule extends HudWidgetModule {
   private static HUDModule hudModule() {
      return ClientContext.requireModule(HUDModule.class);
   }
   private static ColorsModule colorsModule() {
      return ClientContext.requireModule(ColorsModule.class);
   }
   private final Setting<Boolean> barSetting;
   private final Setting<Boolean> durabilitySetting;
   private final Setting<Boolean> percentageSetting;
   private final Setting<Boolean> barColorSetting;
   private final Setting<Boolean> animationSetting;
   private final FloatAnimation[] durabilityAnimations = {
      new FloatAnimation(), new FloatAnimation(), new FloatAnimation(), new FloatAnimation()
   };
   private final boolean[] durabilityInitialized = new boolean[4];
   private final List<ItemStack> armorStacks = new ArrayList<>(4);

   

   private int getArmorBottomOffset() {
      int bottomOffset;
      offsetSelection: {
         airBarCheck: {
            LocalPlayer playerForSubmersion = ClientAccess.schizoMinecraft.player;
            if (!playerForSubmersion.isUnderWater()) {
               int currentAir = ClientAccess.schizoMinecraft.player.getAirSupply();
               LocalPlayer playerForMaximumAir = ClientAccess.schizoMinecraft.player;
               if (currentAir == playerForMaximumAir.getMaxAirSupply()) {
                  break airBarCheck;
               }
            }

            LocalPlayer playerForUnderwaterCreative = ClientAccess.schizoMinecraft.player;
            if (!playerForUnderwaterCreative.isCreative()) {

               bottomOffset = 65;
               break offsetSelection;
            }
         }

         LocalPlayer playerForPassenger = ClientAccess.schizoMinecraft.player;
         if (playerForPassenger.isPassenger()) {
            LocalPlayer playerForVehicle = ClientAccess.schizoMinecraft.player;
            if (playerForVehicle.getVehicle() instanceof LivingEntity mount) {
               double mountOffsetBase = MathConstants.FORTY_FIVE_DEGREES;
               float adjustedMaximumHealth = mount.getMaxHealth();

               adjustedMaximumHealth -= 1.0F;

               double healthRows = adjustedMaximumHealth / 20.0F;
               double mountHealthRows = Math.ceil(healthRows);

               bottomOffset = (int)(mountOffsetBase + mountHealthRows * 10.0D);
               break offsetSelection;
            }
         }

         LocalPlayer playerForCreativeOffset = ClientAccess.schizoMinecraft.player;
         int creativeOffset;
         if (playerForCreativeOffset.isCreative()) {

            creativeOffset = 17;
         } else {
            creativeOffset = 0;
         }

         bottomOffset = 55 - creativeOffset;
      }

      return bottomOffset;
   }

   @Override
   public float[] calculateDimensions() {
      return this.dimensions(0.0F, 0.0F);
   }

   @EventHandler(priority = 1)
   private void onHudRender(HudRenderEvent event) {
      if (ClientAccess.schizoMinecraft.player == null) {
         java.util.Arrays.fill(this.durabilityInitialized, false);
         return;
      }
      HUDModule hudForEnabledCheck = hudModule();
      if (hudForEnabledCheck.isToggled() || this.isInHudEditor()) {
         GuiGraphics graphicsForWidth = event.getGuiGraphics();
         int screenWidth = graphicsForWidth.guiWidth();
         GuiGraphics graphicsForHeight = event.getGuiGraphics();
         int screenHeight = graphicsForHeight.guiHeight();

         int xOffset = 15;
         RenderUtils.clearColor(
            PostProcessShaderPass.POST_TARGET
         );
         RenderUtils.clearDepth(
            PostProcessShaderPass.POST_TARGET
         );
         RenderTargetUtils.overrideRenderTargets(
            PostProcessShaderPass.POST_TARGET
         );
         try {
            LocalPlayer playerForArmor = ClientAccess.schizoMinecraft.player;
            EquipmentUtils.fillArmorStacks(playerForArmor, this.armorStacks);
            List<ItemStack> armorStacks = this.armorStacks;

         for (int armorIndex = 3; armorIndex >= 0; armorIndex--) {
            ItemStack stack = armorStacks.get(armorIndex);
            if (!stack.isEmpty()) {

               int bottomOffset = this.getArmorBottomOffset();
               PoseStack itemPoses = event.getPoseStack();

               int itemXPosition = screenWidth / 2 + xOffset;
               int itemY = screenHeight - bottomOffset;
               int itemX = itemXPosition;
               this.renderArmorItem(itemPoses, stack, itemX, itemY);
               if (stack.isDamageableItem()) {
                  Setting<Boolean> durabilityOption = this.durabilitySetting;
                  Boolean showDurability = durabilityOption.getValue();
                  if (showDurability) {

                     int durabilityPercent = DamageUtils.getDurabilityPercent(stack);
                     int displayedDurability = this.getDisplayedDurability(armorIndex, durabilityPercent);
                     boolean showPercentage = this.percentageSetting.getValue();
                     String durabilityText = formatDurabilityText(displayedDurability, showPercentage);
                     FontsModule fontsForEnabledCheck = FontsModule.INSTANCE;
                     float percentageTextScale;
                     if (fontsForEnabledCheck.isToggled()) {

                        percentageTextScale = 0.7F;
                     } else {

                        percentageTextScale = 0.65F;
                     }

                     float textScale = percentageTextScale;
                     if (!showPercentage) {
                        textScale = 1.0F;
                     }

                     Setting<ColorsSchemeChoice> colorSchemeOption = colorsModule().schemeSetting;
                     ColorsSchemeChoice colorScheme = colorSchemeOption.getValue();
                     Color itemBarColor = new Color(stack.getBarColor(), false);
                     Color durabilityColor = colorScheme.transform(itemBarColor);
                     float itemTop = screenHeight - bottomOffset;

                     float textYBeforePercentageOffset = itemTop - 3.8F;
                     int plainTextYOffset;
                     if (showPercentage) {
                        plainTextYOffset = 0;
                     } else {

                        plainTextYOffset = 2;
                     }

                     float textY = textYBeforePercentageOffset - plainTextYOffset;
                     FontRenderer textRenderer = FontRenderer.INSTANCE;
                     PoseStack textPoses = event.getPoseStack();
                     float screenWidthForText = screenWidth;

                     float centeredItemX = screenWidthForText / 2.0F + xOffset;

                     float textCenterX = centeredItemX + 9.0F;
                     float textWidth = FontRenderer.INSTANCE
                        .getWidth(durabilityText);

                     float textX = textCenterX - textWidth * 0.5F * textScale - textScale;
                     Setting<Boolean> barColorOption = this.barColorSetting;
                     Boolean useBarColor = barColorOption.getValue();
                     textRenderer.drawWithShadow(
                        textPoses,
                        durabilityText,
                        textX,
                        textY,
                        textScale,
                        useBarColor ? durabilityColor : this.getHudColor(textY)
                     );
                  }
               }

               xOffset += 18;
            } else {
               this.durabilityInitialized[armorIndex] = false;
            }
         }

            RenderUtils.INSTANCE.flushEntities();
         } finally {
            RenderTargetUtils.restoreRenderTargets();
         }
         RenderUtils.clearDepth(
            PostProcessShaderPass.POST_TARGET
         );
         RenderUtils.blitToMain(
            PostProcessShaderPass.POST_TARGET
         );
      }
   }

   private void renderArmorItem(
      PoseStack poses, ItemStack stack, int x, int y
   ) {
      GuiDrawUtils.drawItem(poses, stack, x, y, 0.0F, false);
      Setting<Boolean> barOption = this.barSetting;
      Boolean showBar = barOption.getValue();
      if (showBar) {
         GuiDrawUtils.drawDurabilityBar(poses, stack, x, y);
      }
   }

   static String formatDurabilityText(int durability, boolean percentage) {
      String value = Integer.toString(durability);
      return percentage ? value + "%" : value;
   }

   private int getDisplayedDurability(int slot, int percentage) {
      FloatAnimation animation = this.durabilityAnimations[slot];
      if (!this.animationSetting.getValue() || !this.durabilityInitialized[slot]) {
         animation.setValue(percentage);
         this.durabilityInitialized[slot] = true;
         return percentage;
      }
      animation.setTarget(percentage, 150L);
      return Math.round(animation.getValue());
   }

   public ArmorModule() {
      super("Armor", new String[0]);

      this.barSetting = this.add(new BooleanSetting("Bar", true));
      this.durabilitySetting = this.add(new BooleanSetting("Durability", true).markVisibilityDependency());
      this.animationSetting = this.add(new BooleanSetting("Animation", false).dependsOn(this.durabilitySetting));
      this.percentageSetting = this.add(new BooleanSetting("Percentage", true).dependsOn(new Setting<?>[]{this.durabilitySetting}));
      this.barColorSetting = this.add(new BooleanSetting("BarColor", true).dependsOn(new Setting<?>[]{this.durabilitySetting}));
      this.setRenderer(new ResizableHudWidgetRenderer(this) {
         @Override
         public boolean isEnabled() {
            return false;
         }
      });
   }

}
