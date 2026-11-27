package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PresetNameValidatorTest {
   @TempDir Path temporaryDirectory;

   @Test
   void normalizesJsonExtensionAndResolvesInsideCategory() {
      assertEquals("combat", PresetNameValidator.normalize("combat.JSON"));
      Path category = temporaryDirectory.resolve("presets/modules");
      assertEquals(category.toAbsolutePath().normalize().resolve("combat.json"),
         PresetNameValidator.resolve(category, "combat"));
   }

   @Test
   void rejectsTraversalPathsAndPlatformReservedNames() {
      for (String name : new String[] {"", ".", "..", "../outside", "foo..bar", "a/b", "a\\b",
         "C:outside", "CON", "nul.txt", "trailing.", "bad:name", "name?"}) {
         assertThrows(IllegalArgumentException.class, () -> PresetNameValidator.normalize(name), name);
      }
   }

   @Test
   void rejectsOverlongNames() {
      assertThrows(IllegalArgumentException.class, () -> PresetNameValidator.normalize("x".repeat(65)));
   }
}
