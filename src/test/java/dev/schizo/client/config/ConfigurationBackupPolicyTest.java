package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConfigurationBackupPolicyTest {
   @Test
   void topLevelConfigsUseTheBackupManager() {
      assertTrue(ConfigurationBackupPolicy.usesTopLevelBackup("modules.json"));
      assertTrue(ConfigurationBackupPolicy.usesTopLevelBackup("webhook.json"));
   }

   @Test
   void nestedConfigsDoNotUseTheTopLevelBackupManager() {
      assertFalse(ConfigurationBackupPolicy.usesTopLevelBackup("presets/auto.json"));
      assertFalse(ConfigurationBackupPolicy.usesTopLevelBackup("presets\\auto.json"));
   }
}
