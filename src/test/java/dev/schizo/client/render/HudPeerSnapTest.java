package dev.schizo.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudPeerSnapTest {
   @Test void snapsLeadingCenterAndTrailingEdges() {
      assertEquals(new HudPeerSnap.Match(50,50,0,2),HudPeerSnap.nearest(48,20,200,50,40,5));
      assertEquals(new HudPeerSnap.Match(60,70,1,2),HudPeerSnap.nearest(58,20,200,50,40,5));
      assertEquals(new HudPeerSnap.Match(70,90,2,2),HudPeerSnap.nearest(72,20,200,50,40,5));
   }

   @Test void rejectsInvalidAndOutOfBoundsCandidates() {
      assertNull(HudPeerSnap.nearest(90,10,100,95,10,5));
      assertNull(HudPeerSnap.nearest(0,80,100,90,10,5));
      assertNull(HudPeerSnap.nearest(Float.NaN,20,100,0,20,5));
      assertNull(HudPeerSnap.nearest(10,20,100,10,0,5));
   }

   @Test void tieBreakIsIndependentOfCandidateIterationOrder() {
      var a=new HudPeerSnap.Match(20,20,0,2);
      var b=new HudPeerSnap.Match(24,24,0,2);
      assertSame(a,HudPeerSnap.better(a,b));
      assertSame(a,HudPeerSnap.better(b,a));
      var center=new HudPeerSnap.Match(10,20,1,2);
      assertSame(a,HudPeerSnap.better(center,a));
   }

   @Test void guideRequiresBothLiveEdgesToRemainAligned() {
      var match=HudPeerSnap.nearest(58,20,200,50,40,5);
      assertTrue(HudPeerSnap.aligned(match,60,20,50,40));
      assertFalse(HudPeerSnap.aligned(match,60,20,51,40));
      assertFalse(HudPeerSnap.aligned(match,60,22,50,40));
      assertFalse(HudPeerSnap.aligned(match,Float.NaN,20,50,40));
   }
}
