package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class CommandPaletteShortcutTest {
   @Test
   void acceptsControlKPress() {
      assertTrue(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS, GLFW.GLFW_KEY_K, GLFW.GLFW_MOD_CONTROL
      ));
      assertTrue(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS, GLFW.GLFW_KEY_K,
         GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT
      ));
   }

   @Test
   void rejectsOldShiftOnlyChordAndNonPressActions() {
      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS, GLFW.GLFW_KEY_K, GLFW.GLFW_MOD_SHIFT
      ));
      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_RELEASE, GLFW.GLFW_KEY_K, GLFW.GLFW_MOD_CONTROL
      ));
      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS, GLFW.GLFW_KEY_J, GLFW.GLFW_MOD_CONTROL
      ));
   }
}
