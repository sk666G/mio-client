package dev.schizo.client.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MixinModuleBindingSourceTest {
   @Test
   void mixinsDoNotResolveModulesBeforeRegistryBinding() throws IOException {
      Path mixinRoot = Path.of("src", "main", "java", "dev", "schizo", "mixin");

      List<String> offenders;
      try (Stream<Path> sources = Files.walk(mixinRoot)) {
         offenders = sources
            .filter(path -> path.toString().endsWith(".java"))
            .filter(path -> containsDirectModuleLookup(path))
            .map(mixinRoot::relativize)
            .map(Path::toString)
            .sorted()
            .toList();
      }

      assertTrue(
         offenders.isEmpty(),
         "Mixin sources must use MixinModuleRegistry instead of ClientContext.getModule(...): " + offenders
      );
   }

   private static boolean containsDirectModuleLookup(Path source) {
      try {
         return Files.readString(source).contains("ClientContext.getModule(");
      } catch (IOException readFailure) {
         throw new IllegalStateException("Could not inspect mixin source " + source, readFailure);
      }
   }
}
