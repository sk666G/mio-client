package dev.schizo.client.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MixinModuleBindingSourceTest {
   private static final Pattern STATIC_REGISTRY_LOOKUP = Pattern.compile(
      "(?m)^\\s*private\\s+static\\s+.*=\\s*MixinModuleRegistry\\.getModule\\("
   );

   @Test
   void mixinsDoNotResolveModulesBeforeRegistryBinding() throws IOException {
      Path mixinRoot = Path.of("src", "main", "java", "dev", "schizo", "mixin");

      List<String> offenders;
      try (Stream<Path> sources = Files.walk(mixinRoot)) {
         offenders = sources
            .filter(path -> path.toString().endsWith(".java"))
            .filter(MixinModuleBindingSourceTest::containsUnsafeModuleLookup)
            .map(mixinRoot::relativize)
            .map(Path::toString)
            .sorted()
            .toList();
      }

      assertTrue(
         offenders.isEmpty(),
         "Mixin sources must not resolve modules from static initialization or bypass MixinModuleRegistry: " + offenders
      );
   }

   private static boolean containsUnsafeModuleLookup(Path source) {
      try {
         String text = Files.readString(source);
         return text.contains("ClientContext.getModule(")
            || STATIC_REGISTRY_LOOKUP.matcher(text).find();
      } catch (IOException readFailure) {
         throw new IllegalStateException("Could not inspect mixin source " + source, readFailure);
      }
   }
}
