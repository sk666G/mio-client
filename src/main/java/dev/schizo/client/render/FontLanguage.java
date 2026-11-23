/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.render.text.CharacterRange;
import dev.schizo.client.render.text.ContiguousCharacterRange;
import dev.schizo.client.render.text.DeltaEncodedCharacterRange;
import dev.schizo.client.render.text.ExplicitCharacterRange;
import dev.schizo.client.SchizoClient;

public enum FontLanguage {
   SPECIAL(new ExplicitCharacterRange('∞', '♫', '♪')),
   LATIN(new ContiguousCharacterRange(0, 383)),
   CYRILLIC(new ContiguousCharacterRange(1024, 1327)),
   CHINESE(new DeltaEncodedCharacterRange(19968, SchizoClient.class.getResourceAsStream("/assets/schizo/font/range_cn.bin"))),
   JAPANESE(
      new DeltaEncodedCharacterRange(19968, SchizoClient.class.getResourceAsStream("/assets/schizo/font/range_jp.bin"))
   ),
   JAPANESE_ALPHABET(new ContiguousCharacterRange(12288, 12543));

   private final CharacterRange characterRange;

   FontLanguage(CharacterRange characterRange) {
      this.characterRange = characterRange;
   }

   public CharacterRange getRange() {
      return this.characterRange;
   }

}
