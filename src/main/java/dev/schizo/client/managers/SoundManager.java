/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.audio.SoundClip;
import dev.schizo.client.utils.collections.NamedIdentifier;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.audio.OfflineSoundAssets;
import dev.schizo.client.config.ConfigPathGuard;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SoundManager implements ClientAccess {
   private static final Logger LOGGER = LogManager.getLogger("Schizo Sound System");
   private static final long MAX_CUSTOM_SOUND_BYTES = 16L * 1024L * 1024L;
   private static final long MAX_CUSTOM_SOUND_TOTAL_BYTES = 128L * 1024L * 1024L;
   private static final int MAX_CUSTOM_SOUND_FILES = 512;
   private static final int MAX_CUSTOM_SOUND_CATEGORIES = 128;
   private static final List<String> KILLSTREAK_DIRECTORIES = List.of(
      "doublekill_2", "triplekill_3", "dominating_4", "megakill_5",
      "unstoppable_6", "wickedsick_7", "monsterkill_8up"
   );
   private volatile Map<NamedIdentifier, SoundClip> soundsById = Map.of(NamedIdentifier.NONE, SoundClip.NOOP);
   private volatile Map<Integer, List<SoundClip>> killstreakSounds = Map.of();

   public SoundManager() {
   }

   public Set<NamedIdentifier> getSoundIds() {
      return this.soundsById.keySet();
   }

   private List<SoundClip> loadDirectory(Path directory, CustomSoundLoadBudget budget) {
      List<SoundClip> clips = new ArrayList<>();
      try {
         if (!this.ensureSafeSoundDirectory(directory)) {
            return List.of();
         }
         try (var paths = Files.list(directory)) {
            var files = paths
               .filter(this::isSafeSoundFile)
               .filter(SoundManager::isOggFile)
               .iterator();
            while (files.hasNext() && !budget.exhausted()) {
               Path file = files.next();
               try {
                  byte[] bytes = this.readCustomSound(file);
                  if (!budget.reserve(bytes.length)) {
                     LOGGER.warn("Custom sound load budget exhausted in {}", directory.getFileName());
                     break;
                  }
                  clips.add(new SoundClip(bytes));
               } catch (IOException failure) {
                  LOGGER.warn("Could not load sound {}", file.getFileName(), failure);
               }
            }
         }
      } catch (IOException failure) {
         LOGGER.warn("Could not read sound directory {}", directory.getFileName(), failure);
      }
      return List.copyOf(clips);
   }

   private void loadSoundCategory(Path directory, Map<NamedIdentifier, SoundClip> sounds, CustomSoundLoadBudget budget) {
      try {
         if (!this.ensureSafeSoundDirectory(directory)) {
            return;
         }
         try (var files = Files.list(directory)) {
            var soundFiles = files
               .filter(this::isSafeSoundFile)
               .filter(SoundManager::isOggFile)
               .iterator();
            while (soundFiles.hasNext() && !budget.exhausted()) {
               Path file = soundFiles.next();
               String filename = file.getFileName().toString();
               String name = filename.substring(0, filename.length() - 4);
               try {
                  byte[] bytes = this.readCustomSound(file);
                  if (!budget.reserve(bytes.length)) {
                     LOGGER.warn("Custom sound load budget exhausted in {}", directory.getFileName());
                     break;
                  }
                  sounds.put(
                     new NamedIdentifier(directory.getFileName().toString(), name),
                     new SoundClip(bytes)
                  );
               } catch (IOException | RuntimeException failure) {
                  LOGGER.warn("Could not load custom sound {}", filename, failure);
               }
            }
         }
      } catch (IOException failure) {
         LOGGER.warn("Could not read sound category {}", directory.getFileName(), failure);
      }
   }

   private boolean ensureSafeSoundDirectory(Path directory) throws IOException {
      ConfigPathGuard.ensureDirectory(ConfigurationManager.ROOT_DIRECTORY, directory);
      return true;
   }

   private boolean isSafeSoundFile(Path file) {
      return !Files.isSymbolicLink(file)
         && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
   }

   static boolean isOggFile(Path file) {
      if (file == null || file.getFileName() == null) {
         return false;
      }
      return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg");
   }

   private byte[] readCustomSound(Path file) throws IOException {
      if (Files.isSymbolicLink(file)
         || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Custom sound is not a regular file");
      }

      try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
         byte[] bytes = input.readNBytes((int)MAX_CUSTOM_SOUND_BYTES + 1);
         if (bytes.length > MAX_CUSTOM_SOUND_BYTES) {
            throw new IOException("Custom sound exceeds " + MAX_CUSTOM_SOUND_BYTES + " bytes");
         }
         return bytes;
      }
   }

   public void reload() {
      Path killstreakDirectory = ConfigurationManager.SOUNDS_DIRECTORY.resolve("killstreaks");
      try {
         if (this.ensureSafeSoundDirectory(ConfigurationManager.SOUNDS_DIRECTORY)) {
            OfflineSoundAssets.installKillstreaks(ConfigurationManager.SOUNDS_DIRECTORY, killstreakDirectory);
         }
      } catch (IOException failure) {
         LOGGER.warn("Could not prepare offline killstreak sounds", failure);
      }

      CustomSoundLoadBudget customBudget = new CustomSoundLoadBudget(
         MAX_CUSTOM_SOUND_TOTAL_BYTES,
         MAX_CUSTOM_SOUND_FILES
      );
      Map<Integer, List<SoundClip>> streaks = new HashMap<>();
      for (int index = 0; index < KILLSTREAK_DIRECTORIES.size(); index++) {
         Path directory = killstreakDirectory.resolve(KILLSTREAK_DIRECTORIES.get(index));
         streaks.put(index + 2, loadDirectory(directory, customBudget));
      }
      this.killstreakSounds = Map.copyOf(streaks);
      Map<NamedIdentifier, SoundClip> sounds = new HashMap<>();
      sounds.put(NamedIdentifier.NONE, SoundClip.NOOP);
      for (BuiltinSound bundled : BuiltinSound.values()) {
         if (bundled == BuiltinSound.NONE) continue;
         String resource = "/assets/schizo/sounds/" + bundled.getIdentifier().getName() + ".ogg";
         try (InputStream stream = SoundManager.class.getResourceAsStream(resource)) {
            if (stream == null) {
               LOGGER.warn("Bundled sound is missing: {}", bundled.getIdentifier().getName());
               continue;
            }
            sounds.put(bundled.getIdentifier(), new SoundClip(stream.readAllBytes()));
         } catch (IOException failure) {
            LOGGER.warn("Could not load bundled sound {}", bundled.getIdentifier().getName(), failure);
         }
      }
      try {
         if (this.ensureSafeSoundDirectory(ConfigurationManager.SOUNDS_DIRECTORY)) {
            try (var directories = Files.list(ConfigurationManager.SOUNDS_DIRECTORY)) {
               var customDirectories = directories
                  .filter(path -> !Files.isSymbolicLink(path))
                  .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                  .filter(path -> !path.getFileName().toString().equalsIgnoreCase("killstreaks"))
                  .iterator();
               int categoryCount = 0;
               while (customDirectories.hasNext()
                  && categoryCount < MAX_CUSTOM_SOUND_CATEGORIES
                  && !customBudget.exhausted()) {
                  loadSoundCategory(customDirectories.next(), sounds, customBudget);
                  categoryCount++;
               }
               if (customDirectories.hasNext() && categoryCount >= MAX_CUSTOM_SOUND_CATEGORIES) {
                  LOGGER.warn("Skipping custom sound categories beyond the limit of {}", MAX_CUSTOM_SOUND_CATEGORIES);
               }
            }
         }
      } catch (IOException failure) {
         LOGGER.warn("Could not list custom sounds", failure);
      }

      this.soundsById = Map.copyOf(sounds);
   }

   static final class CustomSoundLoadBudget {
      private long remainingBytes;
      private int remainingFiles;

      CustomSoundLoadBudget(long maximumBytes, int maximumFiles) {
         if (maximumBytes < 1L || maximumFiles < 1) {
            throw new IllegalArgumentException("Custom sound load budget must be positive");
         }
         this.remainingBytes = maximumBytes;
         this.remainingFiles = maximumFiles;
      }

      boolean reserve(int bytes) {
         if (bytes < 0) {
            throw new IllegalArgumentException("Custom sound size cannot be negative");
         }
         if (this.remainingFiles < 1 || bytes > this.remainingBytes) {
            return false;
         }
         this.remainingBytes -= bytes;
         this.remainingFiles--;
         return true;
      }

      boolean exhausted() {
         return this.remainingFiles < 1 || this.remainingBytes < 1L;
      }

      long remainingBytes() {
         return this.remainingBytes;
      }

      int remainingFiles() {
         return this.remainingFiles;
      }
   }

   public SoundClip getSound(NamedIdentifier identifier) {
      return getSound(this, identifier);
   }

   public static SoundClip getSound(SoundManager manager, NamedIdentifier identifier) {
      Objects.requireNonNull(manager);
      return identifier == null ? SoundClip.NOOP : manager.soundsById.getOrDefault(identifier, SoundClip.NOOP);
   }

   public void playSound(NamedIdentifier identifier, Vec3 position, float volume) {
      getSound(identifier).playAt(position, volume);
   }

   public void playSound(NamedIdentifier identifier, float volume) {
      getSound(identifier).play(volume);
   }

   public SoundClip getKillstreakSound(int killCount) {
      if (killCount < 2 || killCount > 8 && killCount % 4 != 0) return null;
      List<SoundClip> sounds = this.killstreakSounds.getOrDefault(Math.min(killCount, 8), List.of());
      return sounds.isEmpty() ? null : sounds.get(ThreadLocalRandom.current().nextInt(sounds.size()));
   }
}
