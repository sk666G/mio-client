package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class CommandPaletteShortcutTest {
   @Test
   void matchesCtrlKPressOnlyWhenNoScreenIsOpen() {
      assertTrue(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS,
         GLFW.GLFW_KEY_K,
         GLFW.GLFW_MOD_CONTROL,
         false
      ));

      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_RELEASE,
         GLFW.GLFW_KEY_K,
         GLFW.GLFW_MOD_CONTROL,
         false
      ));
      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS,
         GLFW.GLFW_KEY_K,
         0,
         false
      ));
      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS,
         GLFW.GLFW_KEY_J,
         GLFW.GLFW_MOD_CONTROL,
         false
      ));
      assertFalse(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS,
         GLFW.GLFW_KEY_K,
         GLFW.GLFW_MOD_CONTROL,
         true
      ));
   }

   @Test
   void acceptsControlAlongsideOtherModifierBits() {
      assertTrue(CommandPaletteShortcut.matches(
         GLFW.GLFW_PRESS,
         GLFW.GLFW_KEY_K,
         GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT,
         false
      ));
   }
}
