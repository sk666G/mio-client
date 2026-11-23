package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationBackupManagerTest {
   @TempDir Path temporaryDirectory;

   private ConfigurationBackupManager manager() {
      return new ConfigurationBackupManager(this.temporaryDirectory);
   }

   private Path configPath(String name) {
      return this.temporaryDirectory.resolve(name);
   }

   @Test
   void backsUpBeforeSaveAndSkipsIdenticalSnapshots() throws IOException {
      Files.writeString(this.configPath("modules.json"), "{\"modules\":[]}"); 
      ConfigurationBackupManager manager = this.manager();

      ConfigurationBackupManager.BackupRecord first = manager.backupBeforeSave("modules.json");
      assertNotNull(first);
      assertNull(manager.backupBeforeSave("modules.json"));
      assertEquals(1, manager.listBackups("modules.json").size());
   }

   @Test
   void restoresAValidatedBackupAndPreservesTheLiveFileFirst() throws IOException {
      Path config = this.configPath("modules.json");
      Files.writeString(config, "{\"modules\":[\"old\"]}");
      ConfigurationBackupManager manager = this.manager();
      var original = manager.createBackup("modules.json");

      Files.writeString(config, "{\"modules\":[\"new\"]}");
      manager.restoreBackup("modules.json", original.fileName());

      assertEquals("{\"modules\":[\"old\"]}", Files.readString(config));
      assertEquals(2, manager.listBackups("modules.json").size());
      assertTrue(manager.listBackups("modules.json").stream()
         .anyMatch(backup -> backup.fileName().equals(original.fileName())));
   }

   @Test
   void retainsOnlyTheNewestFiveManualBackups() throws IOException {
      Path config = this.configPath("modules.json");
      ConfigurationBackupManager manager = this.manager();
      for (int index = 0; index < 8; index++) {
         Files.writeString(config, "{\"revision\":" + index + "}");
         manager.createBackup("modules.json");
      }

      List<ConfigurationBackupManager.BackupRecord> backups = manager.listBackups("modules.json");
      assertEquals(ConfigurationBackupManager.MAX_BACKUPS_PER_CONFIG, backups.size());
      assertEquals("{\"revision\":7}", Files.readString(
         manager.getBackupDirectory().resolve(backups.getFirst().fileName())
      ));
   }

   @Test
   void rejectsInvalidConfigNamesAndBackupTraversal() throws IOException {
      ConfigurationBackupManager manager = this.manager();
      for (String name : new String[] {"", "..", "../modules.json", "nested/modules.json", "modules..json", "modules.txt"}) {
         assertThrows(IllegalArgumentException.class, () -> manager.listBackups(name), name);
      }

      Files.writeString(this.configPath("modules.json"), "{\"modules\":[]}");
      var backup = manager.createBackup("modules.json");
      assertThrows(IOException.class, () -> manager.restoreBackup("modules.json", "../" + backup.fileName()));
      assertThrows(IOException.class, () -> manager.restoreBackup("modules.json", "other." + backup.fileName()));
   }

   @Test
   void refusesMalformedOrNonObjectBackupDocuments() throws IOException {
      ConfigurationBackupManager manager = this.manager();
      Path backups = manager.getBackupDirectory();
      Files.createDirectories(backups);
      Files.writeString(backups.resolve("modules.json.0000000000001-malformed.bak"), "{");
      Files.writeString(this.configPath("modules.json"), "{\"modules\":[]}");

      assertThrows(IOException.class, () -> manager.restoreBackup(
         "modules.json", "modules.json.0000000000001-malformed.bak"
      ));
   }

   @Test
   void rejectsOversizedConfigFiles() throws IOException {
      ConfigurationBackupManager manager = this.manager();
      Files.write(this.configPath("modules.json"), new byte[(int)ConfigurationBackupManager.MAX_CONFIG_BYTES + 1]);

      assertThrows(IOException.class, () -> manager.createBackup("modules.json"));
   }

   @Test
   void refusesSymbolicLinksForConfigAndBackupPaths() throws IOException {
      Path external = this.temporaryDirectory.resolveSibling("external-config.json");
      Files.writeString(external, "{\"modules\":[]}");
      Path config = this.configPath("modules.json");
      Path backupDirectory = this.manager().getBackupDirectory();
      try {
         Files.createSymbolicLink(config, external);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }

      assertThrows(IOException.class, () -> this.manager().backupBeforeSave("modules.json"));
      Files.delete(config);
      Files.createDirectories(backupDirectory);
      Path realBackupDirectory = this.temporaryDirectory.resolve("real-backups");
      Files.move(backupDirectory, realBackupDirectory);
      try {
         Files.createSymbolicLink(backupDirectory, realBackupDirectory);
      } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
         Assumptions.assumeTrue(false, "Symbolic links are unavailable on this platform");
      }
      assertThrows(IOException.class, () -> this.manager().listBackups("modules.json"));
   }
}
