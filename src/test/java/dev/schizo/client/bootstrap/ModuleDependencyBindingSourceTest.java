package dev.schizo.client.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleDependencyBindingSourceTest {
   private static final Pattern MODULE_REGISTRY_BINDER = Pattern.compile(
      "(?s)\\bstatic\\s+void\\s+\\w+\\s*\\([^)]*\\bModuleRegistry\\b[^)]*\\)"
   );

   @Test
   void binderManagedSourcesDoNotResolveModulesBeforeBinding() throws IOException {
      Path sourceRoot = Path.of("src", "main", "java", "dev", "schizo", "client");

      List<String> offenders;
      try (Stream<Path> sources = Files.walk(sourceRoot)) {
         offenders = sources
            .filter(path -> path.toString().endsWith(".java"))
            .filter(ModuleDependencyBindingSourceTest::mixesEarlyLookupWithBinder)
            .map(sourceRoot::relativize)
            .map(Path::toString)
            .sorted()
            .toList();
      }

      assertTrue(
         offenders.isEmpty(),
         "Sources with an explicit ModuleRegistry binder must not pre-resolve dependencies through ClientContext.getModule(...): " + offenders
      );
   }

   private static boolean mixesEarlyLookupWithBinder(Path source) {
      try {
         String text = Files.readString(source);
         return text.contains("ClientContext.getModule(")
            && MODULE_REGISTRY_BINDER.matcher(text).find();
      } catch (IOException readFailure) {
         throw new IllegalStateException("Could not inspect dependency-bound source " + source, readFailure);
      }
   }
}
