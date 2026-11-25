/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.bootstrap;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.config.SafeModeManager;
import dev.schizo.client.integrations.MultiplayerCompatibilityWarnings;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.SchizoClient;
import dev.schizo.client.events.ClientInitializationEvent;
import dev.schizo.client.integrations.baritone.BaritoneAdapter;
import dev.schizo.client.integrations.baritone.DisabledBaritoneBridge;
import dev.schizo.client.integrations.xaero.DisabledWaypointAdapter;
import dev.schizo.client.managers.AliasRegistry;
import dev.schizo.client.managers.AutoPresetRegistry;
import dev.schizo.client.managers.BlockColorRegistry;
import dev.schizo.client.managers.ChatFilterRegistry;
import dev.schizo.client.managers.CommandRegistry;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.managers.ContextProfileRegistry;
import dev.schizo.client.managers.CrystalInteractionManager;
import dev.schizo.client.managers.GlintRegistry;
import dev.schizo.client.managers.HoleManager;
import dev.schizo.client.managers.HudModuleRegistry;
import dev.schizo.client.managers.InputOverrideManager;
import dev.schizo.client.managers.InventoryStateManager;
import dev.schizo.client.managers.KitRegistry;
import dev.schizo.client.managers.MacroRegistry;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.managers.ModulePreferencesRegistry;
import dev.schizo.client.managers.MovementPredictionManager;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.managers.ServerStateManager;
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
import dev.schizo.client.render.FeatureBoxRenderer;
import dev.schizo.client.render.FontSupport;
import dev.schizo.client.render.text.LanguageTable;
import dev.schizo.client.utils.FrameTiming;
import dev.schizo.client.utils.combat.TotemPopTracker;
import dev.schizo.client.utils.entity.PlayerStateTracker;
import dev.schizo.client.utils.inventory.CustomItemRegistries;
import dev.schizo.client.utils.movement.MovementSpeedTracker;
import dev.schizo.client.utils.world.BlockBreakTracker;
import dev.schizo.client.utils.world.MapDataCache;
import java.util.Locale;
import java.io.IOException;
import java.util.Locale.Category;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientInitializer {
   private boolean managersInitialized;
   private boolean clientInitialized;
   private MultiplayerCompatibilityWarnings compatibilityWarnings;

   public void initialize() {
      requireEnvironment();
      if (this.managersInitialized) {
         return;
      }
      // Constructors publish services in dependency order before modules bind to them.
      ClientContext.baritoneBridge = FabricLoader.getInstance().isModLoaded("baritone-meteor")
         ? new BaritoneAdapter() : new DisabledBaritoneBridge();
      ClientContext.waypointAdapter = new DisabledWaypointAdapter();
      FontSupport.initialize();
      ClientContext.languageTable = new LanguageTable();
      ClientContext.inputOverrides = new InputOverrideManager();
      ClientContext.blockColorRegistry = new BlockColorRegistry();
      ClientContext.playerStateTracker = new PlayerStateTracker();
      ClientContext.serverStateManager = new ServerStateManager();
      ClientContext.blockBreakTracker = new BlockBreakTracker();
      ClientContext.tickRateController = new TickRateController();
      ClientContext.playerTickController = new TickRateController.PlayerTicks();
      ClientContext.movementPredictionManager = new MovementPredictionManager();
      ClientContext.holeManager = new HoleManager();
      ClientContext.totemPopTracker = new TotemPopTracker();
      ClientContext.movementSpeedTracker = new MovementSpeedTracker();
      ClientContext.rotationManager = new RotationManager();
      ClientContext.waypointRegistry = new WaypointRegistry();
      ClientContext.featureBoxRenderer = new FeatureBoxRenderer();
      ClientContext.taskScheduler = new TaskScheduler();
      ClientContext.moduleRegistry = new ModuleRegistry();
      ModuleDependencies.bind(ClientContext.moduleRegistry);
      MixinModuleRegistry.bind(ClientContext.moduleRegistry);
      this.compatibilityWarnings = new MultiplayerCompatibilityWarnings();
      ClientContext.modulePreferencesRegistry = new ModulePreferencesRegistry();
      ClientContext.hudModuleRegistry = new HudModuleRegistry();
      ClientContext.tradeRegistry = new TradeRegistry();
      ClientContext.commandRegistry = new CommandRegistry();
      ClientContext.frameTiming = new FrameTiming();
      ClientContext.macroRegistry = new MacroRegistry();
      ClientContext.socialRegistry = new SocialRegistry();
      ClientContext.soundManager = new SoundManager();
      ClientContext.inventoryStateManager = new InventoryStateManager();
      ClientContext.stashRegistry = new StashRegistry();
      ClientContext.webhookManager = new WebhookManager();
      ClientContext.worldStateManager = new WorldStateManager();
      ClientContext.crystalInteractionManager = new CrystalInteractionManager();
      ClientContext.mapDataCache = new MapDataCache();
      ClientContext.kitRegistry = new KitRegistry();
      ClientContext.aliasRegistry = new AliasRegistry();
      ClientContext.chatFilterRegistry = new ChatFilterRegistry();
      ClientContext.glintRegistry = new GlintRegistry();
      ClientContext.autoPresetRegistry = new AutoPresetRegistry();
      ClientContext.contextProfileRegistry = new ContextProfileRegistry();
      ClientContext.swingManager = new SwingInteractionManager();
      Locale.setDefault(Category.FORMAT, Locale.US);
      this.managersInitialized = true;
      SchizoClient.LOGGER.info("Schizo main initialization completed.");
   }

   public void initializeClient() {
      requireEnvironment();
      if (!this.managersInitialized) {
         throw new IllegalStateException("Schizo client initialization requires the main phase.");
      }
      if (this.clientInitialized) {
         return;
      }
      try {
         SafeModeManager.GLOBAL.initialize(ConfigurationManager.ROOT_DIRECTORY);
      } catch (IOException safeModeInitializationFailure) {
         SafeModeManager.GLOBAL.activateFailSafe();
         SchizoClient.LOGGER.error("Could not initialize configuration safety state; starting with modules disabled.", safeModeInitializationFailure);
      }
      if (SafeModeManager.GLOBAL.isActive()) {
         for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
            Module module = (Module)entry;
            if (module.isToggled()) {
               module.disable();
            }
         }
      }
      ClientAccess.eventBus.post(new ClientInitializationEvent());
      ClientContext.soundManager.reload();
      ClientContext.configurationManager = new ConfigurationManager();
      ConfigurationManager configuration = ClientContext.configurationManager;
      CustomItemRegistries.initialize();
      configuration.load();
      this.clientInitialized = true;
      SchizoClient.LOGGER.info("Schizo client initialization completed.");
   }

   private static void requireEnvironment() {
      FabricLoader fabric = FabricLoader.getInstance();
      if (fabric.getEnvironmentType() != EnvType.CLIENT || Runtime.version().feature() < 21) {
         throw new IllegalStateException("Schizo requires the Fabric client environment and Java 21 or newer.");
      }
      String minecraft = fabric.getModContainer("minecraft")
         .orElseThrow(() -> new IllegalStateException("Minecraft mod container is missing."))
         .getMetadata().getVersion().getFriendlyString();
      if (!"1.21.11".equals(minecraft)) {
         throw new IllegalStateException("Schizo requires Minecraft 1.21.11; found " + minecraft);
      }
   }
}
