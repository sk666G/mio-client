package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.managers.SoundManager;
import org.junit.jupiter.api.Test;

class SoundSelectionScreenImportTest {
   @Test
   void acceptsImportSizesThroughConfiguredLimit() {
      assertTrue(SoundSelectionScreen.isSupportedImportSize(0L));
      assertTrue(SoundSelectionScreen.isSupportedImportSize(1L));
      assertTrue(SoundSelectionScreen.isSupportedImportSize(SoundManager.MAX_CUSTOM_SOUND_BYTES));
   }

   @Test
   void rejectsNegativeAndOversizeImports() {
      assertFalse(SoundSelectionScreen.isSupportedImportSize(-1L));
      assertFalse(SoundSelectionScreen.isSupportedImportSize(SoundManager.MAX_CUSTOM_SOUND_BYTES + 1L));
   }
}
