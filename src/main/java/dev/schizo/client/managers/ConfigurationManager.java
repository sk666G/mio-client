/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.config.LegacySocialMigration;
import dev.schizo.client.config.LegacyDirectoryMigration;
import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.config.SafeModeManager;
import dev.schizo.client.config.ConfigurationBackupManager;
import dev.schizo.client.config.ModulePresetCodec;
import dev.schizo.client.config.JsonStateTransaction;
import dev.schizo.client.config.ConfigurationSaveGate;
import dev.schizo.client.config.PresetUndoState;
import dev.schizo.client.render.BundledResourceInstaller;
import dev.schizo.client.utils.FileUtils;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.commands.PanicCommand;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.options.PresetCategory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigurationManager implements ClientAccess {
   public static final Path ROOT_DIRECTORY;
   public static final Path PRESETS_DIRECTORY;
   public static final Path SPAMMER_DIRECTORY;
   public static final Path KILLSAY_DIRECTORY;
   public static final Path TEXTURES_DIRECTORY;
   public static final Path MAPS_DIRECTORY;
   public static final Path CHUNKS_DIRECTORY;
   public static final Path SOUNDS_DIRECTORY;
   public static final Path SHADERS_DIRECTORY;
   public static final Path USER_HOME_MARKER_PATH;
   private static final List<Path> directories;
   private final ConfigurationBackupManager backupManager = new ConfigurationBackupManager(ROOT_DIRECTORY);
   private final ModulePresetCodec modulePresetCodec = new ModulePresetCodec();
   private final Map<PresetCategory, PresetRegistry> presetRegistries = new HashMap<>();
   private final BundledResourceInstaller bundledResources = new BundledResourceInstaller();
   private final PresetUndoState presetUndoState = new PresetUndoState();
   private final ConfigurableRegistry configurables = new ConfigurableRegistry();
   private final ConfigurationSaveGate saveGate = new ConfigurationSaveGate();

   public ModulePresetCodec getModulePresetCodec() {
      return this.modulePresetCodec;
   }

   public ConfigurationBackupManager getBackupManager() {
      return this.backupManager;
   }

   public List<String> getConfigurableNames() {
      List<String> names = new ArrayList<>();
      for (JsonConfigurable configurable : this.configurables) {
         names.add(configurable.getConfigName());
      }
      return List.copyOf(names);
   }

   public boolean isRegisteredConfig(String configName) {
      return configName != null && this.getConfigurableNames().contains(configName);
   }

   public PresetUndoState getPresetUndoState() {
      return this.presetUndoState;
   }

   public void load() {
      directories.clear();
      directories.addAll(List.of(ROOT_DIRECTORY, PRESETS_DIRECTORY, SPAMMER_DIRECTORY, KILLSAY_DIRECTORY,
         TEXTURES_DIRECTORY, MAPS_DIRECTORY, SOUNDS_DIRECTORY, SHADERS_DIRECTORY, CHUNKS_DIRECTORY));
      for (PresetCategory category : PresetCategory.values()) directories.add(category.getDirectory());

      this.prepareDirectories();
      if (!isSafeRootDirectory()) {
         ClientAccess.logger.error("Skipping configuration load because the data root is not a real directory");
         return;
      }

      try {

         LegacySocialMigration.migrate();
      } catch (Exception migrationFailure) {
         ClientAccess.logger.error("Could not migrate legacy social data", migrationFailure);
      }

      Iterator<?> configurableIterator = this.configurables.iterator();

      while (configurableIterator.hasNext()) {
         JsonConfigurable configurable = (JsonConfigurable)configurableIterator.next();

         try {
            Path configPath = ROOT_DIRECTORY.resolve(configurable.getConfigName());
            if (Files.exists(configPath, LinkOption.NOFOLLOW_LINKS)) {
               if (Files.isSymbolicLink(configPath) || !Files.isRegularFile(configPath, LinkOption.NOFOLLOW_LINKS)) {
                  ClientAccess.logger.warn("Skipping configuration {} because it is not a regular file", configurable.getConfigName());
                  continue;
               }
               String configText = ConfigFileReader.readUtf8(configPath, (int)ConfigurationBackupManager.MAX_CONFIG_BYTES);
               JsonElement configJson = JsonParser.parseString(configText);
               JsonStateTransaction.apply(
                  configurable::toJson,
                  configurable::fromJson,
                  configJson.getAsJsonObject()
               );
            }
         } catch (Exception loadFailure) {
            ClientAccess.logger.error("Could not load configuration {}", configurable.getConfigName(), loadFailure);
         }
      }

      for (PresetCategory presetCategory : PresetCategory.values()) {
         PresetRegistry presetRegistry = new PresetRegistry(presetCategory);
         this.presetRegistries.put(presetCategory, presetRegistry);

         presetRegistry.loadPresets();
      }
   }

   private static void createDirectoryTreeNoFollow(Path root, Path destination) throws IOException {
      Path normalizedRoot = root.toAbsolutePath().normalize();
      Path normalizedDestination = destination.toAbsolutePath().normalize();
      if (!normalizedDestination.startsWith(normalizedRoot)) {
         throw new IOException("Configuration directory escapes the data root: " + destination);
      }
      if (Files.isSymbolicLink(normalizedRoot)
         || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration data root is not a real directory: " + normalizedRoot);
      }

      Path current = normalizedRoot;
      for (Path component : normalizedRoot.relativize(normalizedDestination)) {
         current = current.resolve(component);
         if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(current);
         }
         if (Files.isSymbolicLink(current)
            || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Configuration directory component is not a real directory: " + current);
         }
      }
   }

   public void prepareDirectories() {
      try {
         if (Files.isSymbolicLink(ROOT_DIRECTORY)) throw new IOException("Configuration root must not be a symbolic link");
         Files.createDirectories(ROOT_DIRECTORY);
         if (!Files.isDirectory(ROOT_DIRECTORY, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Configuration root is not a real directory");
      } catch (IOException failure) {
         ClientAccess.logger.error("Could not prepare configuration root {}", ROOT_DIRECTORY, failure);
         return;
      }
      try {
         LegacyDirectoryMigration.Result result = LegacyDirectoryMigration.moveDirectory(ROOT_DIRECTORY, "autoez", "killsay");
         if (result == LegacyDirectoryMigration.Result.MOVED) ClientAccess.logger.info("Migrated legacy autoez directory to killsay");
         else if (result == LegacyDirectoryMigration.Result.TARGET_EXISTS) ClientAccess.logger.warn("Leaving legacy autoez directory in place because killsay already exists");
         else if (result == LegacyDirectoryMigration.Result.UNSAFE_SOURCE) ClientAccess.logger.warn("Leaving legacy autoez directory in place because it contains unsafe entries");
      } catch (IOException | IllegalArgumentException failure) {
         ClientAccess.logger.warn("Could not migrate legacy autoez directory", failure);
      }
      for (Path path : directories) {
         try {
            createDirectoryTreeNoFollow(ROOT_DIRECTORY, path);
         } catch (IOException failure) {
            ClientAccess.logger.warn("Could not create configuration directory {}", path.getFileName(), failure);
         }
      }
      this.bundledResources.installMissingResources();
   }

   static {

      Path parentDirectory = FabricLoader.getInstance().getGameDir();

      ROOT_DIRECTORY = dev.schizo.client.config.ClientDataDirectory.migrateLegacy(parentDirectory.resolve("schizo"));
      parentDirectory = ROOT_DIRECTORY;

      PRESETS_DIRECTORY = parentDirectory.resolve("presets");
      parentDirectory = ROOT_DIRECTORY;

      SPAMMER_DIRECTORY = parentDirectory.resolve("spammer");
      parentDirectory = ROOT_DIRECTORY;

      KILLSAY_DIRECTORY = parentDirectory.resolve("killsay");
      parentDirectory = ROOT_DIRECTORY;

      TEXTURES_DIRECTORY = parentDirectory.resolve("textures");
      parentDirectory = ROOT_DIRECTORY;

      MAPS_DIRECTORY = parentDirectory.resolve("maps");
      parentDirectory = ROOT_DIRECTORY;

      CHUNKS_DIRECTORY = parentDirectory.resolve("chunks");
      parentDirectory = ROOT_DIRECTORY;

      SOUNDS_DIRECTORY = parentDirectory.resolve("sounds");
      parentDirectory = ROOT_DIRECTORY;

      SHADERS_DIRECTORY = parentDirectory.resolve("shaders");

      USER_HOME_MARKER_PATH = ROOT_DIRECTORY.resolve(".game-panel-unlocked");
      directories = new ArrayList<>();
   }

   public PresetRegistry getPresetRegistry(PresetCategory category) {
      return this.presetRegistries.get(category);
   }

   private static boolean isSafeRootDirectory() {
      return !Files.isSymbolicLink(ROOT_DIRECTORY)
         && Files.isDirectory(ROOT_DIRECTORY, LinkOption.NOFOLLOW_LINKS);
   }

   public void saveAsyncSnapshot() {
      if (!isSafeRootDirectory()) {
         ClientAccess.logger.error("Skipping configuration save because the data root is not a real directory");
         return;
      }
      if (PanicCommand.unloaded) {
         return;
      }

      long saveVersion = this.saveGate.reserveAsyncSave();
      List<ConfigSnapshot> snapshots = new ArrayList<>();
      Iterator<?> configurableIterator = this.configurables.iterator();
      while (configurableIterator.hasNext()) {
         JsonConfigurable configurable = (JsonConfigurable)configurableIterator.next();
         if (SafeModeManager.GLOBAL.isActive() && "modules.json".equals(configurable.getConfigName())) {
            ClientAccess.logger.info("Preserving module enablement state while Safe Mode is active");
            continue;
         }
         try {
            JsonElement serialized = configurable.toJson();
            snapshots.add(new ConfigSnapshot(configurable.getConfigName(), serialized.getAsJsonObject().deepCopy()));
         } catch (Exception snapshotFailure) {
            ClientAccess.logger.error("Could not snapshot configuration {}", configurable.getConfigName(), snapshotFailure);
         }
      }

      ClientAccess.executorService.submit(() -> this.writeSnapshots(snapshots, saveVersion));
   }

   private void writeSnapshots(List<ConfigSnapshot> snapshots, long saveVersion) {
      this.saveGate.runIfCurrent(
         saveVersion,
         () -> this.writeSnapshotsLocked(snapshots)
      );
   }

   private void writeSnapshotsLocked(List<ConfigSnapshot> snapshots) {
      if (!isSafeRootDirectory()) {
         ClientAccess.logger.error("Skipping configuration save because the data root is not a real directory");
         return;
      }
      Gson gsonSnapshot = ClientAccess.gson;
      for (ConfigSnapshot snapshot : snapshots) {
         try {
            Path configPath = ROOT_DIRECTORY.resolve(snapshot.name());
            if (Files.exists(configPath, LinkOption.NOFOLLOW_LINKS)
               && (Files.isSymbolicLink(configPath)
                  || !Files.isRegularFile(configPath, LinkOption.NOFOLLOW_LINKS))) {
               ClientAccess.logger.warn(
                  "Skipping configuration save because {} is not a regular file",
                  snapshot.name()
               );
               continue;
            }
            try {
               this.backupManager.backupBeforeSave(snapshot.name());
            } catch (IOException | IllegalArgumentException backupFailure) {
               ClientAccess.logger.warn(
                  "Could not back up configuration {} before saving",
                  snapshot.name(),
                  backupFailure
               );
            }
            FileUtils.writeUtf8Atomically(configPath, gsonSnapshot.toJson(snapshot.json()));
         } catch (Exception saveFailure) {
            ClientAccess.logger.error("Could not save configuration {}", snapshot.name(), saveFailure);
         }
      }
   }

   private record ConfigSnapshot(String name, JsonObject json) {
   }

   public void save() {
      if (!isSafeRootDirectory()) {
         ClientAccess.logger.error("Skipping configuration save because the data root is not a real directory");
         return;
      }
      if (PanicCommand.unloaded) {
         return;
      }

      this.saveGate.invalidatePendingAndRun(this::saveCurrentStateLocked);
   }

   private void saveCurrentStateLocked() {
      if (!isSafeRootDirectory()) {
         ClientAccess.logger.error("Skipping configuration save because the data root is not a real directory");
         return;
      }

      Iterator<?> configurableIterator = this.configurables.iterator();
      while (configurableIterator.hasNext()) {
         JsonConfigurable configurable = (JsonConfigurable)configurableIterator.next();
         if (SafeModeManager.GLOBAL.isActive() && "modules.json".equals(configurable.getConfigName())) {
            ClientAccess.logger.info("Preserving module enablement state while Safe Mode is active");
            continue;
         }

         try {
            Path configPath = ROOT_DIRECTORY.resolve(configurable.getConfigName());
            if (Files.exists(configPath, LinkOption.NOFOLLOW_LINKS)
               && (Files.isSymbolicLink(configPath)
                  || !Files.isRegularFile(configPath, LinkOption.NOFOLLOW_LINKS))) {
               ClientAccess.logger.warn(
                  "Skipping configuration save because {} is not a regular file",
                  configurable.getConfigName()
               );
               continue;
            }
            try {
               this.backupManager.backupBeforeSave(configurable.getConfigName());
            } catch (IOException | IllegalArgumentException backupFailure) {
               ClientAccess.logger.warn(
                  "Could not back up configuration {} before saving",
                  configurable.getConfigName(),
                  backupFailure
               );
            }
            Gson gsonSnapshot = ClientAccess.gson;
            JsonElement serialized = configurable.toJson();
            JsonObject object = serialized.getAsJsonObject();
            FileUtils.writeUtf8Atomically(configPath, gsonSnapshot.toJson(object));
         } catch (Exception saveFailure) {
            ClientAccess.logger.error("Could not save configuration {}", configurable.getConfigName(), saveFailure);
         }
      }
   }

}
