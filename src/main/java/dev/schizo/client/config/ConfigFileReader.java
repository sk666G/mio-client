/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/** Reads a bounded UTF-8 configuration file without following a final-component symlink. */
public final class ConfigFileReader {
   private ConfigFileReader() {
   }

   public static String readUtf8(Path path, int maximumBytes) throws IOException {
      Objects.requireNonNull(path, "path");
      if (maximumBytes < 0 || maximumBytes == Integer.MAX_VALUE) {
         throw new IllegalArgumentException("Maximum config size must be between 0 and Integer.MAX_VALUE - 1");
      }

      if (Files.isSymbolicLink(path)
         || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration path is not a regular file");
      }

      byte[] bytes;
      try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
         bytes = input.readNBytes(maximumBytes + 1);
      }
      if (bytes.length > maximumBytes) {
         throw new IOException("Configuration exceeds the supported size limit");
      }
      return new String(bytes, StandardCharsets.UTF_8);
   }
}
