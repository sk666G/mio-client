package dev.schizo.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Resolves configuration files under the Schizo data root without following
 * symbolic-link directory components.
 */
public final class ConfigPathGuard {
   private ConfigPathGuard() {
   }

   public static Path resolveForRead(Path root, String configName) throws IOException {
      return resolve(root, configName, false);
   }

   public static Path resolveForWrite(Path root, String configName) throws IOException {
      return resolve(root, configName, true);
   }

   private static Path resolve(Path root, String configName, boolean createParents) throws IOException {
      if (root == null || configName == null || configName.isBlank()) {
         throw new IOException("Configuration path is missing");
      }

      Path normalizedRoot = root.toAbsolutePath().normalize();
      if (Files.isSymbolicLink(normalizedRoot)
         || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Configuration root is not a real directory");
      }

      final Path relative;
      try {
         relative = Path.of(configName);
      } catch (RuntimeException invalidPath) {
         throw new IOException("Invalid configuration path", invalidPath);
      }
      if (relative.isAbsolute()
         || relative.getNameCount() < 1
         || !relative.equals(relative.normalize())) {
         throw new IOException("Configuration path must stay relative to the data root");
      }

      Path target = normalizedRoot.resolve(relative).normalize();
      if (!target.startsWith(normalizedRoot)
         || !normalizedRoot.relativize(target).equals(relative)) {
         throw new IOException("Configuration path escaped the data root");
      }

      Path parent = target.getParent();
      if (parent == null) {
         throw new IOException("Configuration path has no parent directory");
      }

      Path current = normalizedRoot;
      for (Path component : normalizedRoot.relativize(parent)) {
         current = current.resolve(component);
         if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            if (!createParents) {
               return target;
            }
            Files.createDirectory(current);
         }
         if (Files.isSymbolicLink(current)
            || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Configuration parent is not a real directory: " + current);
         }
      }

      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
         && (Files.isSymbolicLink(target)
            || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))) {
         throw new IOException("Configuration target is not a regular file");
      }

      return target;
   }
}
