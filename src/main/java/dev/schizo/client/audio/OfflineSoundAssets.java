/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.audio;

import dev.schizo.client.config.ConfigPathGuard;
import dev.schizo.client.utils.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class OfflineSoundAssets {
    private static final String PREFIX = "/assets/schizo/offline/killstreaks/";

    private OfflineSoundAssets() {
    }

    public static void installKillstreaks(Path root, Path path) {
        try {
            Path directory = ConfigPathGuard.ensureDirectory(root, path);
            String manifest;
            try (InputStream inputStream = OfflineSoundAssets.class.getResourceAsStream(PREFIX + "manifest.txt")) {
                if (inputStream == null) {
                    throw new IOException("Preserved sound-pack manifest is missing");
                }
                manifest = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (String relativePath : manifest.lines().toList()) {
                if (relativePath.isBlank()) {
                    continue;
                }
                Path destination = directory.resolve(relativePath).normalize();
                if (!destination.startsWith(directory) || !relativePath.endsWith(".ogg")) {
                    throw new IOException("Invalid preserved sound path");
                }
                InputStream inputStream = OfflineSoundAssets.class.getResourceAsStream(PREFIX + relativePath);

                if (inputStream == null) {
                    continue;
                }
                try {
                    Path parent = destination.getParent();
                    if (parent == null) {
                        throw new IOException("Preserved sound path has no parent");
                    }
                    ConfigPathGuard.ensureDirectory(directory, parent);
                    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                        if (Files.isSymbolicLink(destination)
                            || !Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
                            throw new IOException("Preserved sound path is not a regular file: " + destination.getFileName());
                        }
                        continue;
                    }
                    installMissingFile(destination, inputStream.readAllBytes());
                } finally {
                    inputStream.close();
                }
            }
        } catch (IOException | SecurityException exception) {
            System.getLogger("Schizo Offline Sounds").log(System.Logger.Level.WARNING, "Optional killstreak sounds are unavailable: " + exception.getMessage());
        }
    static boolean installMissingFile(Path destination, byte[] bytes) throws IOException {
        try {
            FileUtils.writeBytesCreateNew(destination, bytes);
            return true;
        } catch (FileAlreadyExistsException createdConcurrently) {
            return false;
        }
    }

    }
}

