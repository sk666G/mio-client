package dev.schizo.client.gui.components;

import java.util.Objects;

/** Detached, code-point-aware text edit; no configuration writes. */
final class LegacyTextDraft {
   private final String original;
   private String text;
   private int caret;

   LegacyTextDraft(String original) {
      this.original=Objects.requireNonNull(original);
      text=original;
      caret=text.length();
   }

   String text() {return text;}
   int caret() {return caret;}
   boolean matchesLive(String live) {return original.equals(live);}
   boolean changed() {return !original.equals(text);}
   void home() {caret=0;}
   void end() {caret=text.length();}
   void left() {if(caret>0) caret=text.offsetByCodePoints(caret,-1);}
   void right() {if(caret<text.length()) caret=text.offsetByCodePoints(caret,1);}
   void backspace() {
      if(caret==0) return;
      int previous=text.offsetByCodePoints(caret,-1);
      text=text.substring(0,previous)+text.substring(caret);
      caret=previous;
   }
   void delete() {
      if(caret==text.length()) return;
      text=text.substring(0,caret)+text.substring(text.offsetByCodePoints(caret,1));
   }
   void insert(String input) {
      if(input==null||input.isEmpty()) return;
      text=text.substring(0,caret)+input+text.substring(caret);
      caret+=input.length();
      // Joining a malformed boundary must never leave the cursor inside a pair.
      if(caret>0&&caret<text.length()&&Character.isHighSurrogate(text.charAt(caret-1))
         &&Character.isLowSurrogate(text.charAt(caret))) caret++;
   }
}
