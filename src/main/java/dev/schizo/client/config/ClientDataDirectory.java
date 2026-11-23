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
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public final class ClientDataDirectory {
    private ClientDataDirectory() {
    }

    public static Path migrateLegacy(Path destination) {
        Path legacy = destination.resolveSibling("mio-fabric");
        Path staging = null;
        try {
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(destination) || !Files.isDirectory(destination, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Schizo data path is not a real directory: " + destination);
                }
                return destination;
            }
            if (!Files.exists(legacy, LinkOption.NOFOLLOW_LINKS)) {
                return destination;
            }
            if (Files.isSymbolicLink(legacy) || !Files.isDirectory(legacy, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Legacy data path is not a real directory: " + legacy);
            }
            staging = Files.createTempDirectory(destination.toAbsolutePath().getParent(), ".schizo-migration-");
            Path copied = staging;
            Files.walkFileTree(legacy, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                    if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!directory.equals(legacy)) {
                        Path relative = legacy.relativize(directory).normalize();
                        Path target = copied.resolve(relative).normalize();
                        if (relative.isAbsolute() || relative.startsWith("..") || !target.startsWith(copied)) {
                            throw new IOException("Legacy directory escaped its migration root: " + directory);
                        }
                        Files.createDirectory(target);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (attributes.isSymbolicLink()) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!attributes.isRegularFile()) {
                        throw new IOException("Unsupported entry in legacy data: " + file);
                    }
                    Path relative = legacy.relativize(file).normalize();
                    Path target = copied.resolve(relative).normalize();
                    if (relative.isAbsolute() || relative.startsWith("..") || !target.startsWith(copied)) {
                        throw new IOException("Legacy file escaped its migration root: " + file);
                    }
                    Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                    return FileVisitResult.CONTINUE;
                }
            });

            Files.move(staging, destination);
            return destination;
        } catch (IOException failure) {
            if (staging != null) {
                try {
                    removeStaging(staging);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw new UncheckedIOException("Unable to copy legacy client data to " + destination, failure);
        }
    }

    private static void removeStaging(Path staging) throws IOException {
        Files.walkFileTree(staging, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
