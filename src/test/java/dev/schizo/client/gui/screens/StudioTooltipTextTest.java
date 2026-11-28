package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioTooltipTextTest {
   @Test void onlyShowsFullLabelWhenClipped() {
      assertNull(StudioTooltipText.resolve("Apply",false,null));
      assertEquals("Apply",StudioTooltipText.resolve("Apply",true,null));
      assertNull(StudioTooltipText.resolve("",true," "));
   }

   @Test void contextualHelpWorksAtAllWidths() {
      assertEquals("Preview only.",StudioTooltipText.resolve("Preview",false,"Preview only."));
      assertEquals("Preview\nPreview only.",StudioTooltipText.resolve("Preview",true,"Preview only."));
   }

   @Test void currentLabelReplacesStaleLabelAfterChange() {
      assertEquals("Delete",StudioTooltipText.resolve("Delete",true,null));
      assertEquals("Confirm delete",StudioTooltipText.resolve("Confirm delete",true,null));
      assertNull(StudioTooltipText.resolve("Confirm delete",false,null));
   }
}
