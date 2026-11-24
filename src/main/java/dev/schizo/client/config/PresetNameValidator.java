/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class PresetNameValidator {
   private static final int MAX_NAME_LENGTH = 64;
   private static final Set<String> RESERVED_NAMES = Set.of(
      "CON", "PRN", "AUX", "NUL",
      "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
      "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
   );

   private PresetNameValidator() {
   }

   public static String normalize(String input) {
      if (input == null) {
         throw new IllegalArgumentException("Preset name is required");
      }
      String name = input;
      if (name.regionMatches(true, Math.max(0, name.length() - 5), ".json", 0, 5)) {
         name = name.substring(0, name.length() - 5);
      }
      if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || name.equals(".") || name.equals("..")
         || name.contains("..") || name.startsWith(" ") || name.endsWith(" ") || name.endsWith(".")) {
         throw new IllegalArgumentException("Preset name is empty, too long, or not a valid filename");
      }
      try {
         if (Path.of(name).isAbsolute() || name.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Preset name must be a filename, not a path");
         }
      } catch (InvalidPathException invalid) {
         throw new IllegalArgumentException("Preset name contains invalid filename characters", invalid);
      }
      for (int index = 0; index < name.length(); index++) {
         char character = name.charAt(index);
         if (Character.isISOControl(character) || "<>:\"/\\|?*".indexOf(character) >= 0) {
            throw new IllegalArgumentException("Preset name contains invalid filename characters");
         }
      }
      String deviceName = name.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
      if (RESERVED_NAMES.contains(deviceName)) {
         throw new IllegalArgumentException("Preset name is reserved by the operating system");
      }
      return name;
   }

   public static Path resolve(Path directory, String name) {
      String normalizedName = normalize(name);
      Path root = directory.toAbsolutePath().normalize();
      Path result = root.resolve(normalizedName + ".json").normalize();
      if (!result.startsWith(root)) {
         throw new IllegalArgumentException("Preset path escapes its category directory");
      }
      return result;
   }
}
