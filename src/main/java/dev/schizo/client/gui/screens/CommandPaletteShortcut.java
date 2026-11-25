/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import org.lwjgl.glfw.GLFW;

/** Pure shortcut matching for the global Command Palette key chord. */
public final class CommandPaletteShortcut {
   private CommandPaletteShortcut() {
   }

   public static boolean matches(int action, int key, int modifiers) {
      return action == GLFW.GLFW_PRESS
         && key == GLFW.GLFW_KEY_K
         && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
   }
}
