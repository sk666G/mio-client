/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import dev.schizo.client.ClientContext;
import dev.schizo.client.config.ConfigurationBackupManager;
import dev.schizo.client.config.ModuleConfigLoadReport;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.utils.chat.ChatUtils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Lists, creates and restores bounded backups of client JSON configurations. */
public final class ConfigBackupCommand extends Command {
   private static final int MAX_LISTED = 40;
   private static final int MAX_DISPLAYED_MODULE_ISSUES = 12;
   private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

   public ConfigBackupCommand() {
      super("configbackup");
      this.setAliases("configbackups", "cfgbackup");
   }

   @Override
   public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder root) {
      root.then(literal("configs").executes(context -> listConfigNames()));
      root.then(literal("health").executes(context -> showConfigHealth()));
      root.then(literal("list")
         .executes(context -> listAllBackups())
         .then(argument("config", StringArgumentType.word())
            .executes(this::listConfigBackups)));
      root.then(literal("create")
         .then(argument("config", StringArgumentType.word())
            .executes(this::createBackup)));
      root.then(literal("restore")
         .then(argument("config", StringArgumentType.word())
            .then(argument("backup_file", StringArgumentType.word())
               .executes(this::restoreBackup))));
      root.then(literal("clear")
         .then(argument("config", StringArgumentType.word())
            .executes(this::clearBackups)));

      root.executes(context -> {
         this.print("Config recovery: configs, health, list [config], create <config>, restore <config> <backup-file>, clear <config>.");
         return 1;
      });
   }

   private ConfigurationManager configuration() {
      return ClientContext.configurationManager;
   }

   private boolean isKnownConfig(String configName) {
      return this.configuration() != null && this.configuration().isRegisteredConfig(configName);
   }

   private int showConfigHealth() {
      List<String> names = this.configuration().getConfigurableNames();
      StringBuilder output = new StringBuilder("Schizo configuration health:");
      ModuleConfigLoadReport.Snapshot moduleLoadReport = ClientContext.moduleRegistry == null
         ? new ModuleConfigLoadReport().snapshot()
         : ClientContext.moduleRegistry.getLastConfigLoadReport();
      int valid = 0;
      int issues = 0;

      for (String configName : names) {
         ConfigurationBackupManager.ConfigDiagnostic diagnostic =
            this.configuration().getBackupManager().inspectConfig(configName);
         String backupSummary;
         try {
            int backups = this.configuration().getBackupManager().listBackups(configName).size();
            backupSummary = backups + " backup(s)";
         } catch (IOException failure) {
            backupSummary = "backup directory unavailable";
         }

         boolean partialModuleLoad = configName.equals("modules.json")
            && diagnostic.status() == ConfigurationBackupManager.ConfigStatus.VALID
            && moduleLoadReport.totalIssueCount() > 0;
         if (diagnostic.status() == ConfigurationBackupManager.ConfigStatus.VALID && !partialModuleLoad) {
            valid++;
         } else {
            issues++;
         }

         output.append("\n")
            .append(configName).append(": ")
            .append(partialModuleLoad ? "PARTIAL_LOAD" : diagnostic.status())
            .append(" · ").append(backupSummary);
         if (partialModuleLoad) {
            output.append(" · ")
               .append(moduleLoadReport.totalIssueCount())
               .append(" value(s) failed to apply during the last load");
         } else if (diagnostic.status() != ConfigurationBackupManager.ConfigStatus.VALID) {
            output.append(" · ").append(diagnostic.detail());
         }

         if (partialModuleLoad) {
            int displayed = Math.min(moduleLoadReport.issues().size(), MAX_DISPLAYED_MODULE_ISSUES);
            for (int index = 0; index < displayed; index++) {
               ModuleConfigLoadReport.Issue issue = moduleLoadReport.issues().get(index);
               output.append("\n  ")
                  .append(issue.moduleName()).append(" / ")
                  .append(issue.settingName()).append(" (")
                  .append(issue.failureType()).append(")");
            }
            if (moduleLoadReport.omittedIssueCount() > 0) {
               output.append("\n  … and ")
                  .append(moduleLoadReport.omittedIssueCount())
                  .append(" additional issue(s)");
            }
         }
      }

      output.append("\nSummary: ")
         .append(valid).append(" valid, ")
         .append(issues).append(" requiring attention.");
      this.print(output.toString());
      return issues == 0 ? 1 : 0;
   }

   private int listConfigNames() {
      List<String> names = this.configuration().getConfigurableNames();
      this.print("Backed-up configuration files: " + String.join(", ", names));
      return 1;
   }

   private int listAllBackups() {
      StringBuilder output = new StringBuilder("Configuration backups:");
      int displayed = 0;
      try {
         for (String configName : this.configuration().getConfigurableNames()) {
            List<ConfigurationBackupManager.BackupRecord> backups =
               this.configuration().getBackupManager().listBackups(configName);
            for (ConfigurationBackupManager.BackupRecord backup : backups) {
               if (displayed >= MAX_LISTED) {
                  output.append("\n… additional backups omitted");
                  this.print(output.toString());
                  return 1;
               }
               output.append("\n")
                  .append(configName).append(" → ")
                  .append(backup.fileName()).append(" (")
                  .append(formatTime(backup.createdAtMillis())).append(")");
               displayed++;
            }
         }
      } catch (IOException failure) {
         this.print("Could not list configuration backups: " + failure.getMessage());
         return 0;
      }
      if (displayed == 0) {
         output.append("\nNo backups exist yet. The client creates them automatically before saving changed configuration files.");
      }
      this.print(output.toString());
      return 1;
   }

   private int listConfigBackups(CommandContext<?> context) {
      String configName = StringArgumentType.getString(context, "config");
      if (!this.isKnownConfig(configName)) {
         this.print("Unknown configuration file. Use ;configbackup configs to list valid names.");
         return 0;
      }
      try {
         List<ConfigurationBackupManager.BackupRecord> backups =
            this.configuration().getBackupManager().listBackups(configName);
         if (backups.isEmpty()) {
            this.print("No backups found for " + configName + ".");
            return 1;
         }

         StringBuilder output = new StringBuilder("Backups for ").append(configName).append(":");
         int count = Math.min(backups.size(), MAX_LISTED);
         for (int index = 0; index < count; index++) {
            ConfigurationBackupManager.BackupRecord backup = backups.get(index);
            output.append("\n")
               .append(backup.fileName()).append("  ")
               .append(formatTime(backup.createdAtMillis())).append("  ")
               .append(formatBytes(backup.sizeBytes()));
         }
         if (backups.size() > count) {
            output.append("\n… and ").append(backups.size() - count).append(" more");
         }
         this.print(output.toString());
         return 1;
      } catch (IOException failure) {
         this.print("Could not list backups for " + configName + ": " + failure.getMessage());
         return 0;
      }
   }

   private int createBackup(CommandContext<?> context) {
      String configName = StringArgumentType.getString(context, "config");
      if (!this.isKnownConfig(configName)) {
         this.print("Unknown configuration file. Use ;configbackup configs to list valid names.");
         return 0;
      }
      try {
         ConfigurationBackupManager.BackupRecord backup =
            this.configuration().getBackupManager().createBackup(configName);
         this.print("Created backup " + backup.fileName() + ".");
         return 1;
      } catch (IOException | IllegalArgumentException failure) {
         this.print("Could not create a backup for " + configName + ": " + failure.getMessage());
         return 0;
      }
   }

   private int restoreBackup(CommandContext<?> context) {
      String configName = StringArgumentType.getString(context, "config");
      String backupName = StringArgumentType.getString(context, "backup_file");
      if (!this.isKnownConfig(configName)) {
         this.print("Unknown configuration file. Use ;configbackup configs to list valid names.");
         return 0;
      }
      try {
         this.configuration().restoreBackup(configName, backupName);
         this.print(
            "Restored " + backupName + " to " + configName
               + ". Restart Minecraft to load it. Saves to this config are suspended until restart; the previous live file was backed up first."
         );
         return 1;
      } catch (IOException | IllegalArgumentException failure) {
         this.print("Could not restore configuration: " + failure.getMessage());
         return 0;
      }
   }

   private int clearBackups(CommandContext<?> context) {
      String configName = StringArgumentType.getString(context, "config");
      if (!this.isKnownConfig(configName)) {
         this.print("Unknown configuration file. Use ;configbackup configs to list valid names.");
         return 0;
      }
      try {
         int deleted = this.configuration().getBackupManager().clearBackups(configName);
         this.print("Deleted " + deleted + " backup(s) for " + configName + ".");
         return 1;
      } catch (IOException | IllegalArgumentException failure) {
         this.print("Could not clear backups for " + configName + ": " + failure.getMessage());
         return 0;
      }
   }

   private static String formatTime(long timestamp) {
      return TIME_FORMAT.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
   }

   private static String formatBytes(long bytes) {
      return bytes >= 1_048_576L
         ? String.format("%.1f MiB", bytes / 1_048_576.0D)
         : String.format("%.1f KiB", bytes / 1024.0D);
   }

   private void print(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
