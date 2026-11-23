/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo compatibility inventory.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

import dev.schizo.client.modules.client.SettingsModule;
import dev.schizo.client.modules.client.ColorsModule;
import dev.schizo.client.modules.client.FontsModule;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.modules.movement.NoFallModule;
import dev.schizo.client.modules.exploit.ReachModule;
import dev.schizo.client.modules.exploit.PhaseModule;
import dev.schizo.client.modules.combat.AntiPhaseModule;
import dev.schizo.client.modules.movement.ElytraFlyModule;
import dev.schizo.client.modules.combat.CriticalsModule;
import dev.schizo.client.modules.movement.NoSlowModule;
import dev.schizo.client.modules.movement.FastClimbModule;
import dev.schizo.client.modules.player.FastPlaceModule;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.modules.exploit.AntiHungerModule;
import dev.schizo.client.modules.exploit.PingSpoofModule;
import dev.schizo.client.modules.movement.FakeLagModule;
import dev.schizo.client.modules.exploit.PortalGodModeModule;
import dev.schizo.client.modules.movement.WarpModule;
import java.util.Map;

/**
 * Conservative source-review labels for module behavior. Unknown modules and every setting
 * without a dedicated review stay UNVERIFIED by default.
 */
public final class ModuleCompatibility {
   private static final Assessment UNVERIFIED = new Assessment(
      Classification.UNVERIFIED,
      false,
      "This module has not completed a source-level multiplayer compatibility review."
   );

   private static final Map<Class<?>, Assessment> REVIEWED = Map.ofEntries(
      Map.entry(SettingsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local preferences and client settings only; no gameplay packet or server-state mutation was found."
      )),
      Map.entry(ColorsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local theme colors and render-dependent color settings only; no gameplay packet path was found."
      )),
      Map.entry(FontsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local font selection and text rendering only; no gameplay packet or server-state mutation was found."
      )),
      Map.entry(HUDModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local HUD rendering and editor presentation only; no outgoing gameplay packet path was found."
      )),
      Map.entry(NoFallModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Its Packet mode modifies the on-ground flag in outgoing movement packets."
      )),
      Map.entry(ReachModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes interaction reach distance and can produce interactions outside vanilla reach."
      )),
      Map.entry(PhaseModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes collision and player-position behavior beyond the vanilla movement model."
      )),
      Map.entry(AntiPhaseModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates block placement in response to pearl-phasing behavior."
      )),
      Map.entry(ElytraFlyModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Implements non-vanilla flight modes and movement behavior."
      )),
      Map.entry(CriticalsModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Offers modes that alter attack timing/outcomes through packet or rotation behavior."
      )),
      Map.entry(NoSlowModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes movement slowdown behavior; mode-specific packet and input effects require review."
      )),
      Map.entry(FastClimbModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Changes climbing movement behavior beyond ordinary player input."
      )),
      Map.entry(FastPlaceModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Changes the cadence of block/item placement interactions."
      )),
      Map.entry(SpeedMineModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Changes mining interaction cadence and progression."
      )),
      Map.entry(AntiHungerModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes movement/sprint packet behavior to influence hunger updates."
      )),
      Map.entry(PingSpoofModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Delays or alters keepalive timing."
      )),
      Map.entry(FakeLagModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Buffers or delays movement and other outbound packets."
      )),
      Map.entry(PortalGodModeModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes handling or timing of portal-related server packets."
      )),
      Map.entry(WarpModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Accumulates and replays movement packets, changing their timing/order."
      ))
   );

   private ModuleCompatibility() {}

   public static Assessment assess(Class<?> moduleType) {
      if (moduleType == null) {
         return UNVERIFIED;
      }
      return REVIEWED.getOrDefault(moduleType, UNVERIFIED);
   }

   /**
    * No setting has been individually certified yet. The parent module classification is
    * included only as inherited context and is not a setting-level compatibility claim.
    */
   public static SettingAssessment assessSetting(Class<?> moduleType, String settingName) {
      Assessment module = assess(moduleType);
      String name = settingName == null || settingName.isBlank() ? "unnamed setting" : settingName;
      return new SettingAssessment(
         Classification.UNVERIFIED,
         module.classification(),
         false,
         "Setting '" + name + "' has not been individually reviewed; parent module classification is "
            + module.classification() + "."
      );
   }

   private static Assessment assessed(Classification classification, String reason) {
      return new Assessment(classification, true, reason);
   }

   public enum Classification {
      CLIENT_ONLY,
      VISUAL_ONLY,
      INPUT_MODIFYING,
      WORLD_INTERACTION,
      PACKET_AFFECTING,
      SERVER_SENSITIVE,
      UNVERIFIED;

      public boolean requiresMultiplayerWarning() {
         return this != CLIENT_ONLY && this != VISUAL_ONLY;
      }
   }

   public record Assessment(Classification classification, boolean sourceReviewed, String reason) {
      public boolean requiresMultiplayerWarning() {
         return this.classification().requiresMultiplayerWarning();
      }
   }

   public record SettingAssessment(
      Classification classification,
      Classification inheritedModuleClassification,
      boolean individuallyReviewed,
      String reason
   ) {
      public boolean requiresWarning() {
         return this.inheritedModuleClassification().requiresMultiplayerWarning();
      }
   }
}
