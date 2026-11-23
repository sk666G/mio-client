/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings.options;

import dev.schizo.client.settings.NamedEntry;

public enum GameType implements NamedEntry {
   TETRIS("Tetris"),
   SNAKE("Snake"),
   MINESWEEPER("Minesweeper");

   private final String name;

   GameType(String name) {
      this.name = name;
   }

   @Override
   public String getName() {
      return this.name;
   }
}
