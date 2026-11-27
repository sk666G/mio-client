/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import dev.schizo.client.render.text.ReverseStringFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class FileUtils {
   private static final String suffixPathPattern = "\u0001\u0001";

   private FileUtils() {
   }

   public static byte[] readBytes(Path path) throws IOException {
      try (FileInputStream input = new FileInputStream(path.toFile())) {
         return input.readAllBytes();
      }
   }

   public static Path findExistingWithSuffixes(Path path, String... suffixes) {
      File baseFile = path.toFile();
      if (baseFile.exists()) {
         return path;
      }

      for (String suffix : suffixes) {
         String pathText = String.valueOf(path);
         String candidatePathText = new ReverseStringFormatter()
            .prepend(suffix)
            .prepend(pathText)
            .format(suffixPathPattern);
         Path candidatePath = Path.of(candidatePathText);
         if (candidatePath.toFile().exists()) {
            return candidatePath;
         }
      }

      return path;
   }

   public static void writeBytes(Path path, byte[] bytes) throws IOException {
      try (FileOutputStream output = new FileOutputStream(path.toFile())) {
         output.write(bytes);
      }
   }

   public static void writeUtf8(Path path, String text) throws IOException {
      writeBytes(path, text.getBytes(StandardCharsets.UTF_8));
   }

   public static void writeBytesAtomically(Path path, byte[] bytes) throws IOException {
      Path target = path.toAbsolutePath().normalize();
      Path parent = target.getParent();
      if (parent == null) {
         throw new IOException("Cannot write a file without a parent directory: " + target);
      }
      Files.createDirectories(parent);
      Path temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
      Throwable primaryFailure = null;
      try {
         Files.write(temporary, bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
         try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException failure) {
         IOException wrapped = new IOException("Unable to atomically write " + target, failure);
         primaryFailure = wrapped;
         throw wrapped;
      } catch (RuntimeException | Error failure) {
         primaryFailure = failure;
         throw failure;
      } finally {
         cleanupTemporary(temporary, primaryFailure);
      }
   }

   static void cleanupTemporary(Path temporary, Throwable primaryFailure) throws IOException {
      try {
         Files.deleteIfExists(temporary);
      } catch (IOException cleanupFailure) {
         if (primaryFailure == null) {
            throw cleanupFailure;
         }
         if (primaryFailure != cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
         }
      }
   }

   public static void writeUtf8Atomically(Path path, String text) throws IOException {
      Path target = path.toAbsolutePath().normalize();
      Path parent = target.getParent();
      if (parent == null) {
         throw new IOException("Cannot write a file without a parent directory: " + target);
      }
      Files.createDirectories(parent);
      Path temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
      Throwable primaryFailure = null;
      try {
         try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(text);
            writer.flush();
         }
         try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException failure) {
         IOException wrapped = new IOException("Unable to atomically write " + target, failure);
         primaryFailure = wrapped;
         throw wrapped;
      } catch (RuntimeException | Error failure) {
         primaryFailure = failure;
         throw failure;
      } finally {
         cleanupTemporary(temporary, primaryFailure);
      }
   }

   public static String readUtf8(Path path) throws IOException {
      return new String(readBytes(path), StandardCharsets.UTF_8);
   }
}
