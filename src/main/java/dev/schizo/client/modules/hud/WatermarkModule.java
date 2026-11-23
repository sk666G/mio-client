/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;
import dev.schizo.client.settings.options.HudAnchor;
import dev.schizo.client.SchizoClient;

import java.time.LocalDate;
import java.time.Month;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class WatermarkModule extends HudWidgetModule {
   private static boolean aprilFoolsDay;
   public final Setting<Boolean> versionColorSetting;
   private final Setting<String> textSetting;

   private String getVersionSuffix() {
      return " v" + ClientAccess.getVersion();
   }

   static {
      aprilFoolsDay = false;
      LocalDate today = LocalDate.now();
      aprilFoolsDay = today.getMonth() == Month.APRIL && today.getDayOfMonth() == 1;
   }

   public String getVersionText() {
      String version = ClientAccess.getVersion();
      return "v" + version;
   }

   private Component buildWatermarkText() {
      if (aprilFoolsDay) {
         String aprilFoolsText = SchizoClient.NAME + " v" + SchizoClient.VERSION;
         return Component.literal(aprilFoolsText);
      }

      String versionSuffix = this.getVersionSuffix();
      MutableComponent versionComponent = Component.literal(versionSuffix);
      Setting<Boolean> versionColorOption = this.versionColorSetting;
      Boolean useWhiteVersion = versionColorOption.getValue();
      if (useWhiteVersion) {
         ChatFormatting versionColor = ChatFormatting.WHITE;
         versionComponent.withStyle(versionColor);
      }

      String configuredText = this.textSetting.getValue();
      String trimmedText = configuredText.trim();
      MutableComponent watermark = Component.literal(trimmedText);
      return watermark.append(versionComponent);
   }

   public WatermarkModule() {
      super("Watermark", new String[0]);
      this.versionColorSetting = this.add(new BooleanSetting("VersionColor", true));
      this.textSetting = this.add(new StringSetting("Text", "Schizo"));
      HudTextRow textRow = new HudTextRow(this::buildWatermarkText, () -> true);
      this.setRenderer(new HudTextListRenderer(this, textRow));
      this.getRenderer().setAnchor(HudAnchor.TOP_LEFT);
      this.setToggled(true);
   }
}
