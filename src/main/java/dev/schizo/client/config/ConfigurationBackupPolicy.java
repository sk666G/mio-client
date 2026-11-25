/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

/** Backup eligibility for configuration paths handled by the top-level backup manager. */
public final class ConfigurationBackupPolicy {
   private ConfigurationBackupPolicy() {
   }

   public static boolean usesTopLevelBackup(String configName) {
      return configName != null
         && configName.indexOf('/') < 0
         && configName.indexOf('\\') < 0;
   }
}
