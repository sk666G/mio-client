/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.SchizoClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public final class BundledResourceInstaller {
   private final HashMap<String, String> resourcePaths = new HashMap<>();

   public void installMissingResources() {
      this.resourcePaths.forEach(this::installMissingResource);
   }

   public BundledResourceInstaller() {
      this.resourcePaths.put("textures/overlay.png", "assets/schizo/textures/overlay.png");
      this.resourcePaths.put("textures/skin_protect.png", "assets/schizo/textures/skin_protect.png");
      this.resourcePaths.put("textures/shine.png", "assets/schizo/textures/shine.png");
      this.resourcePaths.put("shaders/sky.frag", "assets/schizo/shaders/sky/sky.frag");
      this.resourcePaths.put("shaders/custom.frag", "assets/schizo/shaders/custom/example.frag");
   }

   private void installMissingResource(String relativeDestination, String bundledResource) {
      Path root = ConfigurationManager.ROOT_DIRECTORY.toAbsolutePath().normalize();
      Path destination = root.resolve(relativeDestination).normalize();
      if (!destination.startsWith(root)) {
         SchizoClient.LOGGER.warn("Skipping bundled resource with an unsafe destination: {}", relativeDestination);
         return;
      }
      if (Files.isSymbolicLink(root)
         || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
         SchizoClient.LOGGER.warn("Skipping bundled resource because the configuration root is not a real directory");
         return;
      }

      Path parent = destination.getParent();
      if (parent == null || !parent.startsWith(root)
         || Files.isSymbolicLink(parent)
         || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
         SchizoClient.LOGGER.warn("Skipping bundled resource because its destination directory is unsafe: {}", relativeDestination);
         return;
      }
      if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
         if (Files.isSymbolicLink(destination)
            || !Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
            SchizoClient.LOGGER.warn("Skipping bundled resource because its target is not a regular file: {}", relativeDestination);
         }
         return;
      }

      try (InputStream input = SchizoClient.class.getResourceAsStream("/" + bundledResource)) {
         if (input == null) {
            return;
         }
         byte[] resourceBytes = input.readAllBytes();
         try {
            Files.write(destination, resourceBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
         } catch (FileAlreadyExistsException createdConcurrently) {
            // A resource installed by another startup path wins; never replace an existing target.
         }
      } catch (IOException ioFailure) {
         SchizoClient.LOGGER.warn("Could not install bundled resource {}", relativeDestination, ioFailure);
      }
   }
}
