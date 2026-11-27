package dev.schizo.client.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfflineSoundAssetsInstallTest {
   @TempDir
   Path directory;

   @Test
   void installCreatesMissingFile() throws Exception {
      Path target = this.directory.resolve("sound.ogg");

      assertTrue(OfflineSoundAssets.installMissingFile(target, new byte[]{1, 2, 3}));
      assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(target));
   }

   @Test
   void concurrentWinnerIsPreservedInsteadOfReplaced() throws Exception {
      Path target = this.directory.resolve("sound.ogg");
      Files.write(target, new byte[]{4, 5, 6});

      assertFalse(OfflineSoundAssets.installMissingFile(target, new byte[]{9, 9, 9}));
      assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(target));
   }
}
