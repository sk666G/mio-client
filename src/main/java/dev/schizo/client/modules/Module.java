/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.config.ModuleConfigLoadReport;
import dev.schizo.client.config.SafeModeManager;
import dev.schizo.client.integrations.MultiplayerCompatibilityWarnings;
import dev.schizo.client.integrations.MultiplayerCompatibilityPolicy;
import dev.schizo.client.ClientContext;
import dev.schizo.client.NamedFeature;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.profiling.RuntimeDiagnostics;
import dev.schizo.client.utils.Toggleable;
import dev.schizo.client.render.text.LanguageTable;
import dev.schizo.client.managers.AliasRegistry;
import dev.schizo.client.utils.inventory.RegistryContainer;
import dev.schizo.client.events.ModuleToggleEvent;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.Setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.ChatFormatting;

public class Module extends NamedFeature implements Toggleable, ClientAccess, RegistryContainer, JsonConfigurable {
   private final String descriptionTranslationKey;
   private final ModuleCategory category;
   private final List<Setting<?>> settings;
   private final String[] aliases;
   private Keybind keybind;
   private boolean enabled;
   private boolean drawn;
   private boolean workInProgress;
   private boolean ignoreEnabledState;
   private boolean compatibilityPolicySuppressed;
   private final List<ModuleConfigLoadReport.Issue> lastConfigLoadIssues = new ArrayList<>();

   public Module(String name, String description, ModuleCategory category, String... aliases) {
      super(name);
      this.descriptionTranslationKey = "descriptions.modules." + this.getName();
      this.settings = new ArrayList<>();
      this.keybind = Keybind.UNBOUND;
      this.drawn = true;
      this.category = category;
      this.setDescription(description);
      String[] suppliedAliases = Objects.requireNonNullElseGet(aliases, () -> new String[0]);
      this.aliases = Arrays.copyOf(suppliedAliases, suppliedAliases.length + 1);
      this.aliases[suppliedAliases.length] = name;
   }

   public Module(String name, ModuleCategory category, String... aliases) {
      this(name, "", category, aliases);
   }

   public <S extends Setting<?>> S add(S setting) {
      this.register(setting);
      return setting;
   }

   public <S extends Setting<?>> S add(S setting, Setting<?> precedingSetting) {
      int insertionIndex = this.settings.indexOf(precedingSetting) + 1;
      this.settings.add(insertionIndex, setting);
      return setting;
   }

   public boolean register(Setting<?> setting) {
      return this.settings.add(setting);
   }

   @Override
   public void enable() {
      if (SafeModeManager.GLOBAL.isActive()) {
         return;
      }
      if (this.isToggled()) {
         return;
      }
      MultiplayerCompatibilityWarnings.warnIfNeeded(this);
      if (MultiplayerCompatibilityPolicy.GLOBAL.shouldBlock(this)) {
         this.compatibilityPolicySuppressed = true;
         return;
      }
      this.compatibilityPolicySuppressed = false;
      this.enabled = true;
      ClientAccess.eventBus.subscribe(this);
      try {
         EventBus eventBus = ClientAccess.eventBus;
         eventBus.post(new ModuleToggleEvent(this));
         this.onToggle();
         this.onEnable();
      } catch (RuntimeException | Error enableFailure) {
         this.rollbackFailedEnable(enableFailure);
         RuntimeDiagnostics.GLOBAL.record(
            RuntimeDiagnostics.Area.MODULE_LIFECYCLE,
            this.getName() + " enable",
            enableFailure
         );
         ClientAccess.logger.error("Module {} failed while enabling; state was rolled back", this.getName(), enableFailure);
         throw enableFailure;
      }
   }

   public boolean unregister(Setting<?> setting) {
      return this.settings.remove(setting);
   }

   public String[] getAliases() {
      return this.aliases;
   }

   public String getInfo() {
      return null;
   }

   @Override
   public void setToggled(boolean toggled) {
      if (!toggled && !this.enabled) {
         this.compatibilityPolicySuppressed = false;
         return;
      }
      Toggleable.super.setToggled(toggled);
   }

   @Override
   public boolean isToggled() {
      return this.enabled;
   }

   public void onEnable() {
   }

   public void onToggle() {
   }

   public boolean isWip() {
      return this.workInProgress;
   }

   public boolean isDrawn() {
      return this.drawn;
   }

   public void setKeybind(Keybind keybind) {
      this.keybind = keybind;
   }

   protected void setWip(boolean workInProgress) {
      this.workInProgress = workInProgress;
   }

   public Keybind getKeybind() {
      return this.keybind;
   }

   public void onDisable() {
   }

   public void setDrawn(boolean drawn) {
      this.drawn = drawn;
   }

   @Override
   public String getDescription() {
      LanguageTable languageTable = ClientContext.languageTable;
      String translationKey = this.descriptionTranslationKey;
      String defaultDescription = super.getDescription();
      return languageTable.getTranslationOrDefault(translationKey, defaultDescription);
   }

   public List<ModuleConfigLoadReport.Issue> getLastConfigLoadIssues() {
      return List.copyOf(this.lastConfigLoadIssues);
   }

   @Override
   public void fromJson(JsonElement json) {
      this.lastConfigLoadIssues.clear();
      json = dev.schizo.client.config.LegacyBranding.migrateModule(getName(), json);
      if (!(json instanceof JsonObject moduleJson)) {
         return;
      }
      if (moduleJson.has("settings") && moduleJson.get("settings") instanceof JsonObject settingsJson) {
         Iterator<Setting<?>> settingsIterator = this.getSettings().iterator();
         while (settingsIterator.hasNext()) {
            Setting<?> setting = settingsIterator.next();
            if (setting.isExcludedFromConfig() || !settingsJson.has(setting.getConfigName())) {
               continue;
            }
            try {
               setting.fromJson(settingsJson.get(setting.getConfigName()));
            } catch (Exception invalidSetting) {
               this.lastConfigLoadIssues.add(new ModuleConfigLoadReport.Issue(
                  this.getName(),
                  setting.getConfigName(),
                  invalidSetting.getClass().getSimpleName()
               ));
            }
         }
      }
      if (moduleJson.has("drawn")) {
         this.setDrawn(moduleJson.get("drawn").getAsBoolean());
      }
      if (moduleJson.has("key")) {
         Function<Keybind, Keybind> keybindModifier = keybind -> {
            int keyCode = moduleJson.get("key").getAsInt();
            Keybind codedKeybind = keybind.withCode(keyCode);
            Keybind.Mode mode = moduleJson.has("state")
               ? Keybind.Mode.fromSerializedName(moduleJson.get("state").getAsString())
               : Keybind.Mode.TOGGLE;
            return codedKeybind.withMode(mode);
         };
         this.modifyKeybind(keybindModifier);
      }
      if (moduleJson.has("mouse")) {
         Function<Keybind, Keybind> mouseModifier = keybind -> keybind.withMouse(moduleJson.get("mouse").getAsBoolean());
         this.modifyKeybind(mouseModifier);
      }
      if (moduleJson.has("enabled") && !this.ignoreEnabledState) {
         this.setToggled(moduleJson.get("enabled").getAsBoolean());
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject moduleJson = new JsonObject();
      if (!this.ignoreEnabledState) {
         moduleJson.addProperty("enabled", this.enabled || this.compatibilityPolicySuppressed);
      }
      moduleJson.addProperty("key", this.keybind.code());
      moduleJson.addProperty("state", this.keybind.mode().getSerializedName());
      moduleJson.addProperty("mouse", this.keybind.mouse());
      moduleJson.addProperty("drawn", this.drawn);
      JsonObject settingsJson = new JsonObject();
      Iterator<Setting<?>> settingsIterator = this.getSettings().iterator();
      while (settingsIterator.hasNext()) {
         Setting<?> setting = settingsIterator.next();
         if (!setting.isGroupOnly() && !setting.isExcludedFromConfig()) {
            String configName = setting.getConfigName();
            JsonElement settingJson = setting.toJson();
            settingsJson.add(configName, settingJson);
         }
      }
      moduleJson.add("settings", settingsJson);
      return moduleJson;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   @Override
   public void disable() {
      this.compatibilityPolicySuppressed = false;
      this.disableInternal();
   }

   /** Disables a running module for policy reasons without overwriting its saved enable preference. */
   public void suspendForCompatibilityPolicy() {
      if (!this.enabled) {
         return;
      }
      this.compatibilityPolicySuppressed = true;
      this.disableInternal();
   }

   /** Explicitly clears a policy-suspended module's saved enabled preference. */
   public void resetCompatibilityPolicyPreference() {
      if (this.enabled) {
         this.disable();
      } else {
         this.compatibilityPolicySuppressed = false;
      }
   }

   private void disableInternal() {
      if (!this.isToggled()) {
         return;
      }
      ClientAccess.eventBus.unsubscribe(this);
      this.enabled = false;

      Throwable failure = null;
      try {
         ClientAccess.eventBus.post(new ModuleToggleEvent(this));
      } catch (RuntimeException | Error toggleEventFailure) {
         failure = toggleEventFailure;
      }
      try {
         this.onToggle();
      } catch (RuntimeException | Error toggleFailure) {
         failure = appendFailure(failure, toggleFailure);
      }
      try {
         this.onDisable();
      } catch (RuntimeException | Error disableFailure) {
         failure = appendFailure(failure, disableFailure);
      }

      if (failure != null) {
         RuntimeDiagnostics.GLOBAL.record(
            RuntimeDiagnostics.Area.MODULE_LIFECYCLE,
            this.getName() + " disable",
            failure
         );
         ClientAccess.logger.error("Module {} failed while disabling; disabled state was preserved", this.getName(), failure);
         rethrowUnchecked(failure);
      }
   }

   private void rollbackFailedEnable(Throwable enableFailure) {
      ClientAccess.eventBus.unsubscribe(this);
      this.enabled = false;
      this.compatibilityPolicySuppressed = false;

      try {
         ClientAccess.eventBus.post(new ModuleToggleEvent(this));
      } catch (RuntimeException | Error rollbackFailure) {
         enableFailure.addSuppressed(rollbackFailure);
      }
      try {
         this.onToggle();
      } catch (RuntimeException | Error rollbackFailure) {
         enableFailure.addSuppressed(rollbackFailure);
      }
      try {
         this.onDisable();
      } catch (RuntimeException | Error rollbackFailure) {
         enableFailure.addSuppressed(rollbackFailure);
      }
   }

   private static Throwable appendFailure(Throwable current, Throwable next) {
      if (current == null) {
         return next;
      }
      if (current != next) {
         current.addSuppressed(next);
      }
      return current;
   }

   private static void rethrowUnchecked(Throwable failure) {
      if (failure instanceof Error error) {
         throw error;
      }
      throw (RuntimeException)failure;
   }

   public String getInfoString() {
      AliasRegistry aliasRegistry = ClientContext.aliasRegistry;
      String moduleAlias = AliasRegistry.getModuleAlias(aliasRegistry, this);
      String info = this.getInfo();
      String infoSuffix;
      if (info != null) {
         String openingColor = String.valueOf(ChatFormatting.GRAY);
         String infoColor = String.valueOf(ChatFormatting.WHITE);
         String closingColor = String.valueOf(ChatFormatting.GRAY);
         infoSuffix = openingColor + " [" + infoColor + info + closingColor + "]";
      } else {
         infoSuffix = "";
      }
      return moduleAlias + infoSuffix;
   }

   public void modifyKeybind(Function modifier) {
      this.keybind = (Keybind)modifier.apply(this.keybind);
   }

   @Override
   public String getConfigName() {
      return this.getName();
   }

   protected void ignoreState() {
      this.ignoreEnabledState = true;
   }

   public List<Setting<?>> getSettings() {
      return this.settings;
   }

   @Override
   public boolean unregister(Object entry) {
      return this.unregister((Setting<?>)entry);
   }

   @Override
   public boolean register(Object entry) {
      return this.register((Setting<?>)entry);
   }

   @Override
   public Collection getRegistry() {
      return this.getSettings();
   }

}
