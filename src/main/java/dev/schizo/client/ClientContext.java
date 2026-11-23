/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client;

import dev.schizo.client.gui.screens.ClickGuiScreen;
import dev.schizo.client.gui.screens.HudEditorScreen;
import dev.schizo.client.integrations.baritone.BaritoneBridge;
import dev.schizo.client.integrations.xaero.WaypointAdapter;
import dev.schizo.client.managers.AliasRegistry;
import dev.schizo.client.managers.AutoPresetRegistry;
import dev.schizo.client.managers.BlockColorRegistry;
import dev.schizo.client.managers.ChatFilterRegistry;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.managers.CrystalInteractionManager;
import dev.schizo.client.managers.GlintRegistry;
import dev.schizo.client.managers.HoleManager;
import dev.schizo.client.managers.HudModuleRegistry;
import dev.schizo.client.managers.InputOverrideManager;
import dev.schizo.client.managers.InventoryStateManager;
import dev.schizo.client.managers.KitRegistry;
import dev.schizo.client.managers.MacroRegistry;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.managers.MovementPredictionManager;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.managers.ServerStateManager;
import dev.schizo.client.managers.ShaderRegistry;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.managers.SoundManager;
import dev.schizo.client.managers.StashRegistry;
import dev.schizo.client.managers.SwingInteractionManager;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.managers.TickRateController;
import dev.schizo.client.managers.TradeRegistry;
import dev.schizo.client.managers.WaypointRegistry;
import dev.schizo.client.managers.WebhookManager;
import dev.schizo.client.managers.WorldStateManager;
import dev.schizo.client.modules.Module;
import dev.schizo.client.render.FeatureBoxRenderer;
import dev.schizo.client.render.text.LanguageTable;
import dev.schizo.client.utils.FrameTiming;
import dev.schizo.client.utils.combat.TotemPopTracker;
import dev.schizo.client.utils.entity.PlayerStateTracker;
import dev.schizo.client.utils.movement.MovementSpeedTracker;
import dev.schizo.client.utils.world.BlockBreakTracker;
import dev.schizo.client.utils.world.MapDataCache;
import java.util.Objects;

public final class ClientContext {
   public static TickRateController tickRateController;
   public static ConfigurationManager configurationManager;
   public static RotationManager rotationManager;
   public static volatile ModuleRegistry moduleRegistry;
   public static CommandRegistry commandRegistry;
   public static FrameTiming frameTiming;
   public static ServerStateManager serverStateManager;
   public static SocialRegistry socialRegistry;
   public static WaypointRegistry waypointRegistry;
   public static HoleManager holeManager;
   public static SoundManager soundManager;
   public static MacroRegistry macroRegistry;
   public static InventoryStateManager inventoryStateManager;
   public static MovementPredictionManager movementPredictionManager;
   public static TotemPopTracker totemPopTracker;
   public static MovementSpeedTracker movementSpeedTracker;
   public static PlayerStateTracker playerStateTracker;
   public static FeatureBoxRenderer featureBoxRenderer;
   public static TickRateController.PlayerTicks playerTickController;
   public static StashRegistry stashRegistry;
   public static TaskScheduler taskScheduler;
   public static HudModuleRegistry hudModuleRegistry;
   public static WebhookManager webhookManager;
   public static WorldStateManager worldStateManager;
   public static CrystalInteractionManager crystalInteractionManager;
   public static MapDataCache mapDataCache;
   public static KitRegistry kitRegistry;
   public static AliasRegistry aliasRegistry;
   public static ChatFilterRegistry chatFilterRegistry;
   public static GlintRegistry glintRegistry;
   public static BlockBreakTracker blockBreakTracker;
   public static ShaderRegistry shaderRegistry;
   public static TradeRegistry tradeRegistry;
   public static BlockColorRegistry blockColorRegistry;
   public static AutoPresetRegistry autoPresetRegistry;
   public static InputOverrideManager inputOverrides;
   public static LanguageTable languageTable;
   public static SwingInteractionManager swingManager;
   public static BaritoneBridge baritoneBridge;
   public static WaypointAdapter waypointAdapter;
   public static ClickGuiScreen clickGui;
   private static HudEditorScreen hudEditor;

   private ClientContext() {
   }

   public static <T extends Module> T getModule(Class<T> moduleType) {
      Objects.requireNonNull(moduleType, "Module type");
      ModuleRegistry registry = moduleRegistry;
      return registry == null ? null : moduleType.cast(registry.getModule(moduleType));
   }

   public static ClickGuiScreen getClickGui() {
      if (clickGui == null) {
         clickGui = new ClickGuiScreen();
      }

      return clickGui;
   }

   public static HudEditorScreen getHudEditor() {
      if (hudEditor == null) {
         hudEditor = new HudEditorScreen();
      }

      return hudEditor;
   }
}
