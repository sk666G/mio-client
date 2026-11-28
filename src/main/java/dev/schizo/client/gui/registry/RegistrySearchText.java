package dev.schizo.client.gui.registry;

import java.util.Locale;

/** Match both human-readable labels and stable namespaced resource identifiers. */
final class RegistrySearchText {
   private String raw,normalized="";

   String query(String text) {
      if(!text.equals(raw)) {
         raw=text;
         normalized=normalize(text);
      }
      return normalized;
   }

   static String normalize(String text) {
      return text.strip().toLowerCase(Locale.ROOT);
   }

   static boolean matches(String normalizedLabel,String normalizedId,String normalizedQuery) {
      return normalizedQuery.isEmpty() || normalizedLabel.contains(normalizedQuery) || normalizedId.contains(normalizedQuery);
   }
}
