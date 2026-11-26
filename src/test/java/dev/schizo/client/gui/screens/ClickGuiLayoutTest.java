package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClickGuiLayoutTest {
   @Test
   void keepsPreferredScaleWhenPanelsAlreadyFit() {
      assertEquals(1.0F, ClickGuiLayout.fitScale(1.0F, 800, 1.0D, 700), 0.0001F);
   }

   @Test
   void reducesOnlyEnoughToFitNarrowViewport() {
      assertEquals(0.5F, ClickGuiLayout.fitScale(1.0F, 400, 1.0D, 800), 0.0001F);
   }

   @Test
   void neverUpscalesPastUserPreference() {
      assertEquals(0.7F, ClickGuiLayout.fitScale(0.7F, 1200, 1.0D, 800), 0.0001F);
   }

   @Test
   void preservesInputForInvalidGeometry() {
      assertEquals(0.8F, ClickGuiLayout.fitScale(0.8F, 0, 1.0D, 800), 0.0001F);
      assertEquals(0.8F, ClickGuiLayout.fitScale(0.8F, 800, 0.0D, 800), 0.0001F);
   }
}
