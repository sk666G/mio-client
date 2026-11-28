package dev.schizo.client.render;

import dev.schizo.client.settings.options.FontsStyleChoice;
import java.util.Set;

/** Identity of atlas content only; offsets and filtering are applied at draw time. */
record FontAtlasConfiguration(String family, int size, FontsStyleChoice style, Set<FontLanguage> languages) {
   FontAtlasConfiguration {
      languages = Set.copyOf(languages);
   }

   AtlasTextRenderer create() {
      return AtlasTextRenderer.create(family, size, style, languages);
   }
}
