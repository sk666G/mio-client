package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.schizo.client.settings.options.FontsStyleChoice;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FontAtlasConfigurationTest {
   @Test
   void identityIsIndependentOfLanguageSetOrderAndSourceMutation() {
      var source = new HashSet<>(Set.of(FontLanguage.LATIN, FontLanguage.CYRILLIC));
      var first = new FontAtlasConfiguration("Dialog", 32, FontsStyleChoice.PLAIN, source);
      var equal = new FontAtlasConfiguration("Dialog", 32, FontsStyleChoice.PLAIN,
         Set.of(FontLanguage.CYRILLIC, FontLanguage.LATIN));
      source.clear();
      assertEquals(equal, first);
      assertEquals(equal.hashCode(), first.hashCode());
      assertThrows(UnsupportedOperationException.class, () -> first.languages().clear());
   }

   @Test
   void everyAtlasContentSettingContributesToIdentity() {
      var languages = Set.of(FontLanguage.LATIN);
      var base = new FontAtlasConfiguration("Dialog", 32, FontsStyleChoice.PLAIN, languages);
      assertNotEquals(base, new FontAtlasConfiguration("Serif", 32, FontsStyleChoice.PLAIN, languages));
      assertNotEquals(base, new FontAtlasConfiguration("Dialog", 34, FontsStyleChoice.PLAIN, languages));
      assertNotEquals(base, new FontAtlasConfiguration("Dialog", 32, FontsStyleChoice.BOLD, languages));
      assertNotEquals(base, new FontAtlasConfiguration("Dialog", 32, FontsStyleChoice.PLAIN,
         Set.of(FontLanguage.LATIN, FontLanguage.CHINESE)));
   }
}
