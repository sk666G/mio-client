/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import com.mojang.blaze3d.platform.Window;
import dev.schizo.client.ClientAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class GuiScaleUtils implements ClientAccess {
   private static final boolean ADVANCED_UI_SCALE_LOADED = FabricLoader.getInstance().isModLoaded("advanced-ui-scale");
   private static double previousGuiScale = -1.0D;
   private static double scaleMultiplier = 1.0D;

   public static int getBaseScaledHeight() {
      Window window = schizoMinecraft.getWindow();
      return ADVANCED_UI_SCALE_LOADED ? window.getGuiScaledHeight() : (int)Math.ceil(window.getHeight() / (double)calculateGuiScale());
   }

   public static int getScaledHeight() {
      return (int)(schizoMinecraft.getWindow().getGuiScaledHeight() / scaleMultiplier + 1.0D);
   }

   public static int getScaledWidth() {
      return (int)(schizoMinecraft.getWindow().getGuiScaledWidth() / scaleMultiplier + 1.0D);
   }

   public static void restoreGuiScale() {
      if (previousGuiScale < 0.0D) {
         return;
      }
      schizoMinecraft.getWindow().setGuiScale((int)previousGuiScale);
      previousGuiScale = -1.0D;
      scaleMultiplier = 1.0D;
   }

   public static boolean isAdvancedUiScaleLoaded() {
      return ADVANCED_UI_SCALE_LOADED;
   }

   public static double getScaleMultiplier() {
      return scaleMultiplier;
   }

   public static void applyDefaultGuiScale() {
      applyGuiScale(1.0F);
   }

   public static int calculateGuiScale() {
      Minecraft minecraft = schizoMinecraft;
      return minecraft.getWindow().calculateScale(2, minecraft.isEnforceUnicode());
   }

   public static void applyGuiScale(float scale) {
      if (ADVANCED_UI_SCALE_LOADED) {
         return;
      }
      Window window = schizoMinecraft.getWindow();
      if (previousGuiScale < 0.0D) {
         previousGuiScale = window.getGuiScale();
      }
      scaleMultiplier = scale;
      window.setGuiScale(calculateGuiScale());
   }

   public static double calculateScaleMultiplier(float scale) {
      return ADVANCED_UI_SCALE_LOADED ? 1.0D : (double)calculateGuiScale() / schizoMinecraft.getWindow().getGuiScale() * scale;
   }

   private GuiScaleUtils() {
   }
}
