package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConfigurationManagerBackupPolicyTest {
   @Test
   void topLevelConfigsUseTheBackupManager() {
      assertTrue(ConfigurationManager.usesTopLevelBackup("modules.json"));
      assertTrue(ConfigurationManager.usesTopLevelBackup("webhook.json"));
   }

   @Test
   void nestedConfigsDoNotUseTheTopLevelBackupManager() {
      assertFalse(ConfigurationManager.usesTopLevelBackup("presets/auto.json"));
      assertFalse(ConfigurationManager.usesTopLevelBackup("presets\\auto.json"));
   }
}
