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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/** Controls opt-in startup Safe Mode using a marker in the configuration directory. */
public final class SafeModeManager {
   public static final String MARKER_FILE_NAME = ".safe-mode";
   public static final SafeModeManager GLOBAL = new SafeModeManager();

   private Path root;
   private Path marker;
   private volatile boolean active;
   private volatile boolean initialized;
   private volatile boolean armed;
   private volatile boolean unsafeMarker;
   private volatile boolean initializationFailed;

   public synchronized Status initialize(Path configurationRoot) throws IOException {
      Objects.requireNonNull(configurationRoot, "configurationRoot");
      Path normalizedRoot = configurationRoot.toAbsolutePath().normalize();
      if (this.initialized) {
         if (!normalizedRoot.equals(this.root)) {
            throw new IllegalStateException("Safe Mode was already initialized for another configuration directory");
         }
         return this.status();
      }
      if (Files.isSymbolicLink(normalizedRoot)) {
         throw new IOException("Safe Mode configuration root must not be a symbolic link");
      }
      if (!Files.exists(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
         Files.createDirectories(normalizedRoot);
      }
      if (Files.isSymbolicLink(normalizedRoot)
         || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Safe Mode configuration root is not a real directory");
      }

      this.root = normalizedRoot;
      this.marker = normalizedRoot.resolve(MARKER_FILE_NAME);
      this.armed = Files.exists(this.marker, LinkOption.NOFOLLOW_LINKS);
      this.unsafeMarker = this.armed
         && (Files.isSymbolicLink(this.marker)
            || !Files.isRegularFile(this.marker, LinkOption.NOFOLLOW_LINKS));
      this.active = this.armed;
      this.initialized = true;
      this.initializationFailed = false;
      return this.status();
   }

   /** Fail closed when the configuration root cannot be checked during startup. */
   public synchronized void activateFailSafe() {
      this.active = true;
      this.initializationFailed = true;
      this.initialized = true;
   }

   public boolean isActive() {
      return this.active;
   }

   public synchronized Status status() {
      return new Status(this.active, this.armed, this.unsafeMarker, this.initializationFailed);
   }

   /** Arms Safe Mode for the next process without changing the current session. */
   public synchronized boolean armForNextStartup() throws IOException {
      this.requireInitialized();
      this.requireSafeRoot();
      if (Files.exists(this.marker, LinkOption.NOFOLLOW_LINKS)) {
         if (Files.isSymbolicLink(this.marker)
            || !Files.isRegularFile(this.marker, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Safe Mode marker is not a regular file");
         }
         this.armed = true;
         return false;
      }
      Files.createFile(this.marker);
      this.armed = true;
      this.unsafeMarker = false;
      return true;
   }

   /**
    * Removes the next-startup marker. If this process started in Safe Mode, it remains
    * active until shutdown so modules cannot be enabled mid-session.
    */
   public synchronized boolean disarmForNextStartup() throws IOException {
      this.requireInitialized();
      this.requireSafeRoot();
      boolean removed = Files.deleteIfExists(this.marker);
      this.armed = false;
      this.unsafeMarker = false;
      return removed;
   }

   private void requireInitialized() {
      if (!this.initialized || this.root == null || this.marker == null) {
         throw new IllegalStateException("Safe Mode has not been initialized");
      }
   }

   private void requireSafeRoot() throws IOException {
      if (Files.isSymbolicLink(this.root)
         || !Files.isDirectory(this.root, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Safe Mode configuration root is no longer a real directory");
      }
   }

   public record Status(boolean active, boolean armed, boolean unsafeMarker, boolean initializationFailed) {
   }
}
