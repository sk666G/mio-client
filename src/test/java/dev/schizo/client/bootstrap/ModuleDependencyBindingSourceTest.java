package dev.schizo.client.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleDependencyBindingSourceTest {
   private static final Pattern MODULE_REGISTRY_BINDER = Pattern.compile(
      "(?s)\\bstatic\\s+void\\s+(\\w+)\\s*\\([^)]*\\bModuleRegistry\\b[^)]*\\)"
   );
   private static final Pattern REGISTERED_MODULE = Pattern.compile(
      "registerModule\\(\\s*new\\s+(?:dev\\.schizo\\.client\\.modules(?:\\.[\\w$]+)*\\.)?([A-Za-z_$][\\w$]*)\\s*\\("
   );
   private static final Pattern STATIC_FIELD_MODULE_LOOKUP = Pattern.compile(
      "(?s)\\bstatic\\s+(?:(?:final|volatile|transient)\\s+)*[^;{}=]+\\s+[A-Za-z_$][\\w$]*\\s*=\\s*ClientContext\\.getModule\\("
   );
   private static final Pattern STATIC_BLOCK_START = Pattern.compile("\\bstatic\\s*\\{");

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

   @Test
   void clientSourcesDoNotResolveModulesDuringStaticInitialization() throws IOException {
      Path moduleRoot = Path.of("src", "main", "java", "dev", "schizo", "client");

      List<String> offenders;
      try (Stream<Path> sources = Files.walk(moduleRoot)) {
         offenders = sources
            .filter(path -> path.toString().endsWith(".java"))
            .filter(ModuleDependencyBindingSourceTest::resolvesModuleDuringStaticInitialization)
            .map(moduleRoot::relativize)
            .map(Path::toString)
            .sorted()
            .toList();
      }

      assertTrue(
         offenders.isEmpty(),
         "Client sources must not resolve dependencies through ClientContext.getModule(...) during static initialization: " + offenders
      );
   }

   @Test
   void registeredModuleBindersAreInvokedByModuleDependencies() throws IOException {
      Path sourceRoot = Path.of("src", "main", "java", "dev", "schizo", "client");
      Path moduleRoot = sourceRoot.resolve("modules");
      String registrySource = Files.readString(sourceRoot.resolve("managers").resolve("ModuleRegistry.java"));
      String dependenciesSource = Files.readString(sourceRoot.resolve("bootstrap").resolve("ModuleDependencies.java"));

      Set<String> registeredModules = new HashSet<>();
      var registeredMatcher = REGISTERED_MODULE.matcher(registrySource);
      while (registeredMatcher.find()) {
         registeredModules.add(registeredMatcher.group(1));
      }

      List<String> missingBindings;
      try (Stream<Path> sources = Files.walk(moduleRoot)) {
         missingBindings = sources
            .filter(path -> path.toString().endsWith(".java"))
            .flatMap(path -> {
               String fileName = path.getFileName().toString();
               String moduleName = fileName.substring(0, fileName.length() - ".java".length());
               if (!registeredModules.contains(moduleName)) {
                  return Stream.empty();
               }

               try {
                  String text = Files.readString(path);
                  var binders = MODULE_REGISTRY_BINDER.matcher(text);
                  var missing = new java.util.ArrayList<String>();
                  while (binders.find()) {
                     String binderName = binders.group(1);
                     String invocation = moduleName + "." + binderName + "(registry)";
                     if (!dependenciesSource.contains(invocation)) {
                        missing.add(moduleName + "#" + binderName);
                     }
                  }
                  return missing.stream();
               } catch (IOException readFailure) {
                  throw new IllegalStateException("Could not inspect module binder " + path, readFailure);
               }
            })
            .sorted()
            .toList();
      }

      assertTrue(
         missingBindings.isEmpty(),
         "Registered module binders must be invoked by ModuleDependencies.bind(...): " + missingBindings
      );
   }

   private static boolean resolvesModuleDuringStaticInitialization(Path source) {
      try {
         String text = Files.readString(source);
         if (STATIC_FIELD_MODULE_LOOKUP.matcher(text).find()) {
            return true;
         }

         var staticBlocks = STATIC_BLOCK_START.matcher(text);
         while (staticBlocks.find()) {
            int openingBrace = staticBlocks.end() - 1;
            int closingBrace = findMatchingBrace(text, openingBrace);
            if (closingBrace > openingBrace
               && text.substring(openingBrace + 1, closingBrace).contains("ClientContext.getModule(")) {
               return true;
            }
         }
         return false;
      } catch (IOException readFailure) {
         throw new IllegalStateException("Could not inspect module source " + source, readFailure);
      }
   }

   private static int findMatchingBrace(String text, int openingBrace) {
      int depth = 0;
      for (int index = openingBrace; index < text.length(); index++) {
         char current = text.charAt(index);
         if (current == '{') {
            depth++;
         } else if (current == '}') {
            depth--;
            if (depth == 0) {
               return index;
            }
         }
      }
      return -1;
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
