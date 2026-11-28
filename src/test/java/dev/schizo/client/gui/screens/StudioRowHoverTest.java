package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioRowHoverTest {
   @Test void requiresContinuousHoverForSameIdentity() {
      var hover=new StudioRowHover();
      Object a=new String("row"), b=new String("row");
      assertFalse(hover.ready(a,0));
      assertFalse(hover.ready(a,StudioRowHover.DELAY_NANOS-1));
      assertTrue(hover.ready(a,StudioRowHover.DELAY_NANOS));
      assertFalse(hover.ready(b,StudioRowHover.DELAY_NANOS));
      assertTrue(hover.ready(b,2*StudioRowHover.DELAY_NANOS));
   }

   @Test void leavingOrRebuildingClearsDwell() {
      var hover=new StudioRowHover();
      Object row=new Object();
      hover.ready(row,0);
      assertFalse(hover.ready(null,StudioRowHover.DELAY_NANOS));
      assertFalse(hover.ready(row,2*StudioRowHover.DELAY_NANOS));
      assertTrue(hover.ready(row,3*StudioRowHover.DELAY_NANOS));
      hover.clear();
      assertFalse(hover.ready(row,4*StudioRowHover.DELAY_NANOS));
   }
}
