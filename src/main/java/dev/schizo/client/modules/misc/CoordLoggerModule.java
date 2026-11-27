/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.utils.FileUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.DimensionType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;

public class CoordLoggerModule extends Module {
   static final long MAX_COORDINATE_LOG_BYTES = 8L * 1024L * 1024L;
   private static final DateTimeFormatter timestampFormatter;
   private final Setting<Boolean> disconnectSetting;
   private final Setting<Boolean> deathSetting;
   private final Setting<Boolean> fileSetting;
   private int lastX, lastY, lastZ;
   private DimensionType lastDimension;
   private final Object fileWriteLock = new Object();

   record CoordinateSnapshot(int x, int y, int z, DimensionType dimension) {
   }

   void cacheCoordinates(int x, int y, int z, DimensionType dimension) {
      this.lastX = x;
      this.lastY = y;
      this.lastZ = z;
      this.lastDimension = dimension;
   }

   CoordinateSnapshot snapshotCoordinates() {
      DimensionType dimension = this.lastDimension;
      return dimension == null
         ? null
         : new CoordinateSnapshot(this.lastX, this.lastY, this.lastZ, dimension);
   }

   void clearCachedCoordinates() {
      this.lastX = 0;
      this.lastY = 0;
      this.lastZ = 0;
      this.lastDimension = null;
   }

   private void appendRetainedEntry(String entry) {
      Path directory = ConfigurationManager.ROOT_DIRECTORY;
      this.appendRetainedEntry(directory.resolve("coordlogger.txt"), entry);
   }

   void appendRetainedEntry(Path logFile, String entry) {
      synchronized (this.fileWriteLock) {
         try {
            Stream<String> existingLines;
            if (Files.exists(logFile, LinkOption.NOFOLLOW_LINKS)) {
               if (Files.isSymbolicLink(logFile)
                  || !Files.isRegularFile(logFile, LinkOption.NOFOLLOW_LINKS)) {
                  ClientAccess.logger.warn("Skipping coordinate log write because coordlogger.txt is not a regular file");
                  return;
               }
               if (Files.size(logFile) > MAX_COORDINATE_LOG_BYTES) {
                  ClientAccess.logger.warn(
                     "Skipping coordinate log write because coordlogger.txt exceeds {} bytes",
                     MAX_COORDINATE_LOG_BYTES
                  );
                  return;
               }
               existingLines = Files.lines(logFile);
            } else {
               existingLines = Stream.empty();
            }

            List<String> retainedLines;
            try (existingLines) {
               retainedLines = Stream.concat(existingLines, Stream.of(entry))
                  .filter(this::isRetainedEntry)
                  .toList();
            }
            String retainedText = String.join(System.lineSeparator(), retainedLines);
            if (!retainedText.isEmpty()) {
               retainedText += System.lineSeparator();
            }
            FileUtils.writeUtf8Atomically(logFile, retainedText);
         } catch (IOException | UncheckedIOException writeFailure) {
            ClientAccess.logger.warn("Could not update coordinate log", writeFailure);
         }
      }
   }

   private boolean isRetainedEntry(String entry) {
      if (entry == null || entry.isBlank()) {
         return false;
      }
      String[] fields = entry.split(("\\|"));
      int fieldCount = fields.length;

      if (fieldCount != (3)) {
         // Preserve nonblank legacy or manually edited lines rather than silently destroying them.
         return true;
      }

      try {
         LocalDateTime now = LocalDateTime.now();

         String timestampText = fields[2];
         String timestampSnapshot = timestampText.trim();
         DateTimeFormatter parserFormatter = timestampFormatter;
         LocalDateTime entryTime = LocalDateTime.parse(timestampSnapshot, parserFormatter);
         ChronoUnit ageUnit = ChronoUnit.DAYS;
         long ageDays = ageUnit.between(entryTime, now);

         return ageDays < 7L;
      } catch (DateTimeParseException invalidTimestamp) {
         return true;
      }
   }

   private void saveCoordinates(String reason) {
      CoordinateSnapshot snapshot = this.snapshotCoordinates();
      if (snapshot == null) {
         return;
      }

      if (!ClientAccess.schizoMinecraft.isSameThread()) {
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (this.isToggled()) {
               this.saveCoordinates(snapshot, reason);
            }
         });
         return;
      }

      this.saveCoordinates(snapshot, reason);
   }

   private void saveCoordinates(CoordinateSnapshot snapshot, String reason) {
      String coordinates = "X: %d, Y: %d, Z: %d in The %s".formatted(
         snapshot.x(), snapshot.y(), snapshot.z(), snapshot.dimension().getDisplayName()
      );
      StringBuilder entry = new StringBuilder().append(coordinates).append(" | ").append(reason);
      KeyboardHandler keyboard = ClientAccess.schizoMinecraft.keyboardHandler;
      if (!dev.schizo.client.utils.CoordinatePrivacy.isEnabled()) keyboard.setClipboard(entry.toString());

      if (this.fileSetting.getValue()) {
         String timestamp = LocalDateTime.now().format(timestampFormatter);
         String retainedEntry = entry.append(" | ").append(timestamp).toString();
         try {
            ClientAccess.executorService.submit(() -> this.appendRetainedEntry(retainedEntry));
         } catch (java.util.concurrent.RejectedExecutionException rejected) {
            ClientAccess.logger.warn("Could not schedule coordinate log write", rejected);
         }
      }
   }

   static {

      timestampFormatter = DateTimeFormatter.ofPattern(("HH:mm dd-MM-yyyy"));
   }

   @EventHandler
   private void onDisconnect(MinecraftDisconnectEvent event) {
      try {
         if (ClientAccess.schizoMinecraft.player != null && this.disconnectSetting.getValue()) {
            this.saveCoordinates("Disconnect");
         }
      } finally {
         this.clearCachedCoordinates();
      }
   }

   @EventHandler
   private void onPlayerTick(LocalPlayerTickEvent event) {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player == null) {
         this.clearCachedCoordinates();
         return;
      }
      this.cacheCoordinates(
         player.getBlockX(),
         player.getBlockY(),
         player.getBlockZ(),
         WorldUtils.getDimension()
      );
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      if (event.getPacket() instanceof ClientboundPlayerCombatKillPacket) {
         Setting<Boolean> deathSettingSnapshot = this.deathSetting;
         Boolean saveOnDeath = deathSettingSnapshot.getValue();
         if (saveOnDeath) {

            this.saveCoordinates("Death");
         }
      }
   }

   @Override
   public void onEnable() {
      this.clearCachedCoordinates();
   }

   @Override
   public void onDisable() {
      this.clearCachedCoordinates();
   }

   public CoordLoggerModule() {
      super("CoordLogger", "Saves your coordinates whenever you log/die.", ModuleCategory.MISC, new String[0]);
      this.disconnectSetting = this.add(new BooleanSetting("Log", true));
      this.deathSetting = this.add(new BooleanSetting("Death", true));
      this.fileSetting = this.add(new BooleanSetting("File", false));
   }

}
