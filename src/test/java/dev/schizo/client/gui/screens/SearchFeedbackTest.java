package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SearchFeedbackTest {
   @Test void literalRangeUsesOriginalTextAndDoesNotInventFuzzySpans() {
      assertEquals(new SearchFeedback.Span(6,13),SearchFeedback.literalSpan("Frame Metrics","METRICS"));
      assertNull(SearchFeedback.literalSpan("Metrics","metrcis"));
      assertNull(SearchFeedback.literalSpan("Metrics",""));
      assertEquals(new SearchFeedback.Span(2,5),SearchFeedback.literalSpan("😀HUD","hud"));
   }

   @Test void labelsOnlyNonliteralResults() {
      assertEquals("",SearchFeedback.hint(1,"met"));
      assertEquals("Context match",SearchFeedback.hint(3,"crosshair"));
      assertEquals("Flexible match",SearchFeedback.hint(4,"crosshair length"));
      assertEquals("Typo match",SearchFeedback.hint(5,"metrcis"));
      assertEquals("",SearchFeedback.hint(5,""));
   }

   @Test void textFitsWithoutSplittingSurrogatePairs() {
      assertEquals("hello",SearchFeedback.fit("hello",5,String::length));
      assertEquals("he...",SearchFeedback.fit("hello world",5,String::length));
      assertEquals("...",SearchFeedback.fit("😀abcd",4,String::length));
      assertEquals("",SearchFeedback.fit("long",2,String::length));
   }
}
