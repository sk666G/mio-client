/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.config.ContextProfileMatcher;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.world.WorldUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.GameType;

/**
 * Applies preset rules when the active server, dimension or game mode changes.
 *
 * The registry snapshots category state around server connections so switching contexts
 * first restores the state that existed after the ordinary per-server AutoPreset was applied.
 */
public final class ContextProfileRegistry implements ClientAccess, JsonConfigurable {
   public static final int CURRENT_SCHEMA_VERSION = 1;
   public static final int MAX_RULES = 512;

   private final List<Rule> rules = new ArrayList<>();
   private final Map<PresetCategory, JsonElement> baseline = new EnumMap<>(PresetCategory.class);
   private final Set<PresetCategory> modifiedCategories = EnumSet.noneOf(PresetCategory.class);
   private volatile boolean enabled;
   private boolean dirty = true;
   private String lastAppliedContext;

   public ContextProfileRegistry() {
      ClientAccess.eventBus.subscribe(this);
   }

   public synchronized boolean isEnabled() {
      return this.enabled;
   }

   public synchronized void setEnabled(boolean enabled) {
      if (this.enabled == enabled) {
         return;
      }
      if (enabled) {
         if (!this.restoreActiveProfile()) {
            this.dirty = true;
            return;
         }
         this.baseline.clear();
         this.modifiedCategories.clear();
         this.enabled = true;
      } else {
         boolean restored = this.restoreActiveProfile();
         this.enabled = false;
         if (restored) {
            this.baseline.clear();
            this.modifiedCategories.clear();
         }
      }
      this.lastAppliedContext = null;
      this.dirty = true;
   }

   public synchronized List<Rule> getRules() {
      return List.copyOf(this.rules);
   }

   public synchronized void addRule(
      String server, String dimension, String gameMode, PresetCategory category, String presetName
   ) {
      Rule rule = Rule.create(server, dimension, gameMode, category, presetName);
      int existingIndex = this.findRuleIndex(rule.server(), rule.dimension(), rule.gameMode(), rule.category());
      if (existingIndex >= 0) {
         this.rules.set(existingIndex, rule);
      } else {
         if (this.rules.size() >= MAX_RULES) {
            throw new IllegalStateException("Context profile rule limit reached (" + MAX_RULES + ")");
         }
         this.rules.add(rule);
      }
      this.dirty = true;
   }

   public synchronized boolean removeRule(
      String server, String dimension, String gameMode, PresetCategory category
   ) {
      Rule key = Rule.create(server, dimension, gameMode, category, "placeholder");
      int index = this.findRuleIndex(key.server(), key.dimension(), key.gameMode(), key.category());
      if (index < 0) {
         return false;
      }
      this.rules.remove(index);
      this.dirty = true;
      return true;
   }

   public synchronized void clearRules() {
      this.rules.clear();
      this.dirty = true;
   }

   public synchronized String getCurrentContextDescription() {
      if (this.lastAppliedContext == null) {
         return "No context profile has been applied yet";
      }
      return this.lastAppliedContext.replace("|", " / ");
   }

   @Override
   public String getConfigName() {
      return "context-profiles.json";
   }

   @Override
   public synchronized JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("version", CURRENT_SCHEMA_VERSION);
      root.addProperty("enabled", this.enabled);
      JsonArray entries = new JsonArray();
      for (Rule rule : this.rules) {
         JsonObject entry = new JsonObject();
         entry.addProperty("server", rule.server());
         entry.addProperty("dimension", rule.dimension());
         entry.addProperty("gameMode", rule.gameMode());
         entry.addProperty("category", rule.category().getName());
         entry.addProperty("preset", rule.presetName());
         entries.add(entry);
      }
      root.add("profiles", entries);
      return root;
   }

   @Override
   public synchronized void fromJson(JsonElement json) {
      this.rules.clear();
      this.enabled = false;
      this.dirty = true;
      this.lastAppliedContext = null;
      this.baseline.clear();
      this.modifiedCategories.clear();

      if (json == null || !json.isJsonObject()) {
         return;
      }
      JsonObject root = json.getAsJsonObject();
      JsonElement version = root.get("version");
      if (version == null || !version.isJsonPrimitive()
         || !version.getAsJsonPrimitive().isNumber()
         || version.getAsDouble() != CURRENT_SCHEMA_VERSION) {
         ClientAccess.logger.warn("Ignoring unsupported context profile configuration");
         return;
      }

      JsonElement enabledElement = root.get("enabled");
      this.enabled = enabledElement != null
         && enabledElement.isJsonPrimitive()
         && enabledElement.getAsJsonPrimitive().isBoolean()
         && enabledElement.getAsBoolean();
      JsonElement profileElement = root.get("profiles");
      if (profileElement == null || !profileElement.isJsonArray()) {
         this.enabled = false;
         ClientAccess.logger.warn("Ignoring malformed context profile list");
         return;
      }

      JsonArray profiles = profileElement.getAsJsonArray();
      int limit = Math.min(profiles.size(), MAX_RULES);
      for (int index = 0; index < limit; index++) {
         JsonElement element = profiles.get(index);
         if (!element.isJsonObject()) {
            continue;
         }
         JsonObject object = element.getAsJsonObject();
         try {
            String server = requiredString(object, "server");
            String dimension = requiredString(object, "dimension");
            String gameMode = requiredString(object, "gameMode");
            String categoryName = requiredString(object, "category");
            String preset = requiredString(object, "preset");
            PresetCategory category = PresetCategory.findByName(categoryName);
            if (category == null) {
               throw new IllegalArgumentException("Unknown preset category: " + categoryName);
            }
            Rule rule = Rule.create(server, dimension, gameMode, category, preset);
            int existingIndex = this.findRuleIndex(
               rule.server(), rule.dimension(), rule.gameMode(), rule.category()
            );
            if (existingIndex >= 0) {
               this.rules.set(existingIndex, rule);
            } else {
               this.rules.add(rule);
            }
         } catch (RuntimeException invalidRule) {
            ClientAccess.logger.warn("Skipping invalid context profile rule at index {}", index, invalidRule);
         }
      }
      if (profiles.size() > MAX_RULES) {
         ClientAccess.logger.warn(
            "Context profile configuration contains {} rules; only the first {} were loaded",
            profiles.size(), MAX_RULES
         );
      }
   }

   @EventHandler(priority = 100)
   private void beforeServerPresetChange(ConnectScreenConnectEvent event) {
      synchronized (this) {
         boolean restored = this.restoreActiveProfile();
         if (!restored) {
            event.cancel();
            this.dirty = true;
            this.lastAppliedContext = null;
            ClientAccess.logger.error(
               "Cancelled server connection because the active context profile baseline could not be restored"
            );
            return;
         }
         if (!this.enabled) {
            this.baseline.clear();
         }
         this.lastAppliedContext = null;
      }
   }

   /** Runs after the existing AutoPresetRegistry connect listener (priority 0). */
   @EventHandler(priority = -100)
   private void captureServerBaseline(ConnectScreenConnectEvent event) {
      synchronized (this) {
         if (this.enabled) {
            if (this.modifiedCategories.isEmpty()) {
               // A successful server switch invalidates the previous server's baseline.
               this.baseline.clear();
               this.captureBaseline();
            }
         } else if (this.modifiedCategories.isEmpty()) {
            this.baseline.clear();
         }
         this.lastAppliedContext = null;
         this.dirty = true;
      }
   }

   @EventHandler(priority = 100)
   private void restoreOnDisconnect(MinecraftDisconnectEvent event) {
      synchronized (this) {
         boolean restored = this.restoreActiveProfile();
         if (restored) {
            this.baseline.clear();
            this.modifiedCategories.clear();
         } else {
            ClientAccess.logger.error("Context profile baseline remains pending after disconnect");
         }
         this.lastAppliedContext = null;
         this.dirty = true;
      }
   }

   @EventHandler
   private void applyForCurrentContext(LocalPlayerPreTickEvent event) {
      synchronized (this) {
         if (!this.enabled && !this.modifiedCategories.isEmpty()) {
            boolean restored = this.restoreActiveProfile();
            if (restored) {
               this.baseline.clear();
               this.lastAppliedContext = null;
            }
            return;
         }
         if (!this.enabled || this.isWorldUnavailable()) {
            return;
         }

         if (this.baseline.isEmpty() && !this.captureBaseline()) {
            this.dirty = true;
            return;
         }

         String server = currentServerKey();
         String dimension = WorldUtils.getDimension().getName();
         GameType gameType = ClientAccess.schizoMinecraft.gameMode.getPlayerMode();
         if (gameType == null) {
            return;
         }
         String gameMode = gameType.getName().toLowerCase(Locale.ROOT);
         String contextKey = server + "|" + dimension + "|" + gameMode;
         if (!this.dirty && contextKey.equals(this.lastAppliedContext)) {
            return;
         }

         if (!this.restoreActiveProfile()) {
            this.dirty = true;
            return;
         }
         List<Rule> matching = selectMatchingRules(this.rules, server, dimension, gameMode);
         Map<PresetCategory, Rule> selected = new EnumMap<>(PresetCategory.class);
         for (Rule rule : matching) {
            selected.put(rule.category(), rule);
         }

         List<Rule> applicationOrder = new ArrayList<>(selected.values());
         applicationOrder.sort(Comparator
            .comparingInt((Rule rule) -> rule.category() == PresetCategory.ALL ? 0 : 1)
            .thenComparingInt(rule -> rule.category().ordinal()));

         for (Rule rule : applicationOrder) {
            PresetRegistry registry = ClientContext.configurationManager == null
               ? null
               : ClientContext.configurationManager.getPresetRegistry(rule.category());
            if (registry == null) {
               ClientAccess.logger.warn(
                  "Context profile references missing preset {} ({})",
                  rule.presetName(), rule.category().getName()
               );
               continue;
            }

            // Record the category before applying: fromJson may partially mutate settings and then throw.
            this.modifiedCategories.add(rule.category());
            boolean applied;
            try {
               applied = registry.applyPreset(rule.presetName());
            } catch (RuntimeException failure) {
               ClientAccess.logger.error(
                  "Could not apply context preset {} ({})",
                  rule.presetName(), rule.category().getName(), failure
               );
               boolean restored = this.restoreActiveProfile();
               this.lastAppliedContext = restored ? contextKey : null;
               this.dirty = !restored;
               return;
            }
            if (!applied) {
               this.modifiedCategories.remove(rule.category());
               ClientAccess.logger.warn(
                  "Context profile references missing preset {} ({})",
                  rule.presetName(), rule.category().getName()
               );
               continue;
            }
            ClientAccess.logger.info(
               "Applied context preset {} for {} / {} / {}",
               rule.presetName(), server, dimension, gameMode
            );
         }

         this.lastAppliedContext = contextKey;
         this.dirty = false;
      }
   }

   private boolean captureBaseline() {
      return ContextProfileRestoreState.capture(
         this.baseline,
         this.modifiedCategories,
         orderedCategories(),
         category -> {
            JsonElement snapshot = category.toJson();
            if (snapshot == null) {
               throw new IllegalStateException("Preset category returned a null baseline");
            }
            return snapshot.deepCopy();
         },
         (category, failure) -> ClientAccess.logger.warn(
            "Could not snapshot baseline for {} context profiles",
            category.getName(),
            failure
         )
      );
   }

   private boolean restoreActiveProfile() {
      return ContextProfileRestoreState.restore(
         this.baseline,
         this.modifiedCategories,
         orderedCategories(),
         (category, snapshot) -> category.fromJson(snapshot.deepCopy()),
         (category, failure) -> ClientAccess.logger.error(
            "Could not restore baseline for {} context profile",
            category.getName(),
            failure
         )
      );
   }

   private static List<PresetCategory> orderedCategories() {
      List<PresetCategory> categories = new ArrayList<>(List.of(PresetCategory.values()));
      categories.sort(Comparator
         .comparingInt((PresetCategory category) -> category == PresetCategory.ALL ? 0 : 1)
         .thenComparingInt(category -> category.ordinal()));
      return categories;
   }

   private int findRuleIndex(String server, String dimension, String gameMode, PresetCategory category) {
      for (int index = 0; index < this.rules.size(); index++) {
         Rule rule = this.rules.get(index);
         if (rule.server().equals(server) && rule.dimension().equals(dimension)
            && rule.gameMode().equals(gameMode) && rule.category() == category) {
            return index;
         }
      }
      return -1;
   }

   private static String currentServerKey() {
      if (ClientAccess.schizoMinecraft.hasSingleplayerServer()) {
         return "singleplayer";
      }
      if (ClientContext.serverStateManager == null
         || ClientContext.serverStateManager.getServerData() == null
         || ClientContext.serverStateManager.getServerData().ip == null) {
         return "unknown";
      }
      return normalizeServer(ClientContext.serverStateManager.getServerData().ip);
   }

   private static String normalizeServer(String server) {
      String normalized = server == null ? "" : server.strip().toLowerCase(Locale.ROOT);
      if (normalized.isEmpty() || normalized.length() > 255
         || normalized.chars().anyMatch(Character::isWhitespace)
         || normalized.contains("/") || normalized.contains("\\")) {
         throw new IllegalArgumentException("Invalid server selector");
      }
      return normalized;
   }

   private static String normalizeDimension(String dimension) {
      String normalized = dimension == null ? "" : dimension.strip().toLowerCase(Locale.ROOT);
      return switch (normalized) {
         case "any", "overworld", "the_nether", "the_end" -> normalized;
         case "nether" -> "the_nether";
         case "end" -> "the_end";
         default -> throw new IllegalArgumentException("Dimension must be any, overworld, nether or end");
      };
   }

   private static String normalizeGameMode(String gameMode) {
      String normalized = gameMode == null ? "" : gameMode.strip().toLowerCase(Locale.ROOT);
      return switch (normalized) {
         case "any", "survival", "creative", "adventure", "spectator" -> normalized;
         default -> throw new IllegalArgumentException("Game mode must be any, survival, creative, adventure or spectator");
      };
   }

   private static String requiredString(JsonObject object, String key) {
      JsonElement value = object.get(key);
      if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
         throw new IllegalArgumentException("Missing string " + key);
      }
      String text = value.getAsString();
      if (text.isBlank() || text.length() > 255) {
         throw new IllegalArgumentException("Invalid string " + key);
      }
      return text;
   }

   public static List<Rule> selectMatchingRules(
      List<Rule> rules, String server, String dimension, String gameMode
   ) {
      List<ContextProfileMatcher.Entry<Rule>> entries = new ArrayList<>(rules.size());
      for (Rule rule : rules) {
         entries.add(new ContextProfileMatcher.Entry<>(
            rule.server(), rule.dimension(), rule.gameMode(), rule
         ));
      }
      return ContextProfileMatcher.select(entries, server, dimension, gameMode);
   }

   public record Rule(
      String server,
      String dimension,
      String gameMode,
      PresetCategory category,
      String presetName
   ) {
      public static Rule create(
         String server, String dimension, String gameMode, PresetCategory category, String presetName
      ) {
         if (category == null) {
            throw new IllegalArgumentException("Preset category is required");
         }
         String normalizedPreset = dev.schizo.client.config.PresetNameValidator.normalize(presetName);
         return new Rule(
            normalizeServer(server),
            normalizeDimension(dimension),
            normalizeGameMode(gameMode),
            category,
            normalizedPreset
         );
      }
   }
}
