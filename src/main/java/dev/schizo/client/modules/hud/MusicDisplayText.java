package dev.schizo.client.modules.hud;

/** Side-effect-free Music HUD text composition. */
final class MusicDisplayText {
   private MusicDisplayText() {
   }

   static String format(String text, boolean prefixEnabled, String prefixText) {
      String musicText = String.valueOf(text);
      if (!prefixEnabled) {
         return musicText.trim();
      }
      return (String.valueOf(prefixText) + " " + musicText).trim();
   }
}
