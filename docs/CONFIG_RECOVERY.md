# Configuration Backups and Recovery

Schizo automatically keeps a small rolling backup history for each top-level JSON configuration file.

Before an existing config is overwritten during a normal configuration save, the client compares it with the newest backup. If the file changed, it creates a new atomic backup. Identical files do not create duplicate backups.

## Commands

| Command | Action |
| --- | --- |
| `;configbackup configs` | List valid configuration file names |
| `;configbackup list` | List recent backups across all configuration files |
| `;configbackup list modules.json` | List backups for one config |
| `;configbackup create modules.json` | Manually create a backup of the current file |
| `;configbackup restore modules.json <backup-file>` | Restore a selected backup |
| `;configbackup clear modules.json` | Delete backups for one config |

Aliases are `;configbackups` and `;cfgbackup`.

After a restore, restart Minecraft to load the restored configuration. The current file is automatically backed up before it is replaced, so the restore can itself be undone by restoring that newer backup.

## Safety and limits

- Backups are stored in `schizo/backups`.
- Each registered config file keeps at most five backups.
- Config and backup files are limited to 8 MiB.
- Backup writes use a temporary file followed by an atomic move when supported.
- Restore rejects invalid JSON, non-object documents, symbolic links, unsafe names and paths outside the expected directories.
- Only config files registered by Schizo can be listed, backed up, restored or cleared through the command.
- Backups may contain the same private settings as the source config. They remain local files and should not be shared publicly. In particular, protect your Schizo data directory as you would your original configuration files.
