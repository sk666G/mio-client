package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContextProfileMatcherTest {
   @Test
   void matchesWildcardServerAndContextSpecificRules() {
      List<ContextProfileMatcher.Entry<String>> entries = List.of(
         new ContextProfileMatcher.Entry<>("*", "any", "any", "global"),
         new ContextProfileMatcher.Entry<>("play.example.net", "any", "any", "server"),
         new ContextProfileMatcher.Entry<>("play.example.net", "the_nether", "any", "dimension"),
         new ContextProfileMatcher.Entry<>("play.example.net", "the_nether", "survival", "exact")
      );

      assertEquals(
         List.of("global", "server", "dimension", "exact"),
         ContextProfileMatcher.select(entries, "play.example.net", "the_nether", "survival")
      );
   }

   @Test
   void doesNotMatchRulesForAnotherServerDimensionOrMode() {
      List<ContextProfileMatcher.Entry<String>> entries = List.of(
         new ContextProfileMatcher.Entry<>("other.example.net", "any", "any", "other-server"),
         new ContextProfileMatcher.Entry<>("*", "the_end", "any", "end-only"),
         new ContextProfileMatcher.Entry<>("*", "any", "creative", "creative-only")
      );

      assertEquals(
         List.of(),
         ContextProfileMatcher.select(entries, "play.example.net", "overworld", "survival")
      );
   }

   @Test
   void stableOrderKeepsInsertionOrderForEqualSpecificity() {
      List<ContextProfileMatcher.Entry<String>> entries = List.of(
         new ContextProfileMatcher.Entry<>("*", "overworld", "any", "first"),
         new ContextProfileMatcher.Entry<>("play.example.net", "any", "any", "second"),
         new ContextProfileMatcher.Entry<>("*", "any", "survival", "third")
      );

      assertEquals(
         List.of("first", "second", "third"),
         ContextProfileMatcher.select(entries, "play.example.net", "overworld", "survival")
      );
   }
}
