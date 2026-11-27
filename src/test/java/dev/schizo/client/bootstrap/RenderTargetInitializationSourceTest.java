package dev.schizo.client.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RenderTargetInitializationSourceTest {
   private static final Pattern STATIC_RENDER_TARGET = Pattern.compile(
      "(?s)\\bstatic\\s+(?:(?:final|volatile|transient)\\s+)*[^;{}=]+\\s+[A-Za-z_$][\\w$]*\\s*=\\s*RenderTargetRegistry\\.createRenderTarget\\("
   );
   private static final Pattern STATIC_BLOCK_START = Pattern.compile("\\bstatic\\s*\\{");

   @Test
   void renderTargetsAreNotCreatedDuringStaticInitialization() throws IOException {
      Path sourceRoot = Path.of("src", "main", "java", "dev", "schizo");

      List<String> offenders;
      try (Stream<Path> sources = Files.walk(sourceRoot)) {
         offenders = sources
            .filter(path -> path.toString().endsWith(".java"))
            .filter(RenderTargetInitializationSourceTest::createsRenderTargetStatically)
            .map(sourceRoot::relativize)
            .map(Path::toString)
            .sorted()
            .toList();
      }

      assertTrue(
         offenders.isEmpty(),
         "Static initialization must use RenderTargetRegistry.createLazyRenderTarget(...), not createRenderTarget(...): " + offenders
      );
   }

   private static boolean createsRenderTargetStatically(Path source) {
      try {
         String text = Files.readString(source);
         if (STATIC_RENDER_TARGET.matcher(text).find()) {
            return true;
         }

         var staticBlocks = STATIC_BLOCK_START.matcher(text);
         while (staticBlocks.find()) {
            int openingBrace = staticBlocks.end() - 1;
            int closingBrace = findMatchingBrace(text, openingBrace);
            if (closingBrace > openingBrace
               && text.substring(openingBrace + 1, closingBrace).contains("RenderTargetRegistry.createRenderTarget(")) {
               return true;
            }
         }
         return false;
      } catch (IOException readFailure) {
         throw new IllegalStateException("Could not inspect render-target source " + source, readFailure);
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
}
