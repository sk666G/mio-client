/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.PermanentModule;
import dev.schizo.client.modules.render.SearchModule;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.ColorsSchemeChoice;

import java.awt.Color;

public class ColorsModule extends PermanentModule {
   private static SearchModule searchModule;
   private final Setting<Float> rainbowSpeedSetting;
   public final Setting<Color> themeColorSetting;
   public final Setting<Color> friendColorSetting;
   public final Setting<Color> enemyColorSetting;
   public final Setting<ColorsSchemeChoice> schemeSetting;

   public float getRainbowPeriod() {
      return 1.0F / this.rainbowSpeedSetting.getValue();
   }

   

   public static void bindSearchModule(ModuleRegistry registry) {
      searchModule = (SearchModule)registry.getModule(SearchModule.class);
   }

   public ColorsModule() {
      super("Colors", "Manages the client's color system.", ModuleCategory.CLIENT, new String[0]);
      this.rainbowSpeedSetting = this.add(new NumberSetting<>("RainbowSpeed", 1.0F, 0.1F, 10.0F));
      this.themeColorSetting = this.add(new ColorSetting("ThemeColor", new Color(-3697420, true)));
      this.friendColorSetting = this.add(new ColorSetting("FriendColor", new Color(-16711681, true)));
      this.enemyColorSetting = this.add(new ColorSetting("EnemyColor", new Color(-4980736, true)));
      this.schemeSetting = this.add(new EnumSetting<>("Scheme", ColorsSchemeChoice.NORMAL));
      this.schemeSetting.setChangeCallback(() -> {
         if (searchModule.isToggled()) searchModule.restartSearch();
      });
   }
}
