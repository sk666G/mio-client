/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.gui.screens.PresetScreen;
import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.utils.collections.NamedIdentifier;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.PermanentModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.IdentifierSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;

public class SettingsModule extends PermanentModule {
   public final Setting<String> prefixSetting;
   private final Setting<NamedIdentifier> startupSetting;
   private final Setting<Boolean> presetsSetting;

   private static void updateCommandPrefix(SettingsModule owner, String prefixValue) {
      if (!prefixValue.equals(CommandRegistry.getPrefix())) {
         String updatedPrefix = owner.prefixSetting.getValue();
         CommandRegistry.setPrefix(updatedPrefix);
      }
   }

   public NamedIdentifier getStartupSound() {
      return this.startupSetting.getValue();
   }

   public SettingsModule() {
      super("Settings", "Client's global settings.", ModuleCategory.CLIENT, new String[0]);
      this.prefixSetting = this.add(new StringSetting(("Prefix"), ";"));
      this.startupSetting = this.add(new IdentifierSetting(("Startup"), BuiltinSound.BLIP.getIdentifier()));
      this.presetsSetting = this.add(new BooleanSetting(("Presets"), true));
      this.prefixSetting.setChangeCallback(() -> {
         String prefixValue = this.prefixSetting.getValue();
         updateCommandPrefix(this, prefixValue);
      });
      this.presetsSetting.setChangeCallback(() -> {
         if (!this.presetsSetting.getValue()) {
            (new PresetScreen()).open();
            this.presetsSetting.setValue(true);
         }
      });
      this.prefixSetting.setExcludedFromConfig(true);
   }

   public enum MultiplierOption implements NamedEntry {
      NONE("None", 0),
      X2("X2", 2),
      X4("X4", 4),
      X8("X8", 8);

      private final String displayName;
      private final int multiplier;

      MultiplierOption(String displayName, int multiplier) {
         this.displayName = displayName;
         this.multiplier = multiplier;
      }

      @Override
      public String getName() {
         return this.displayName;
      }

      public int getMultiplier() {
         return this.multiplier;
      }
   }
}
