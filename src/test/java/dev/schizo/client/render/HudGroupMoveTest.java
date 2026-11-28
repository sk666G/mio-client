package dev.schizo.client.render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudGroupMoveTest {
   @Test void commonTranslationPreservesSpacingAndStopsAtEdge() {
      var group = List.of(new HudGroupMove.Bounds(10, 20, 20, 10), new HudGroupMove.Bounds(75, 50, 20, 20));
      assertEquals(new HudGroupMove.Delta(5, 0), HudGroupMove.clamp(group, 100, 100, 10, 0));
      assertEquals(new HudGroupMove.Delta(-10, -20), HudGroupMove.clamp(group, 100, 100, -40, -40));
   }

   @Test void oversizedOrInvalidGeometryCannotForceUnexpectedMovement() {
      var group = List.of(new HudGroupMove.Bounds(0, 5, 120, 10));
      assertEquals(new HudGroupMove.Delta(0, 2), HudGroupMove.clamp(group, 100, 100, 5, 2));
      assertEquals(new HudGroupMove.Delta(0, 0), HudGroupMove.clamp(List.of(), 100, 100, 5, 2));
      assertEquals(new HudGroupMove.Delta(0, 0), HudGroupMove.clamp(group, 100, 100, Float.NaN, 2));
   }

   @Test void groupEnvelopePreservesSpacingWhenSnappedAsOneUnit() {
      var members=java.util.List.of(new HudGroupMove.Bounds(10,20,15,10),new HudGroupMove.Bounds(40,30,20,20));
      var box=HudGroupMove.envelope(members);
      assertEquals(new HudGroupMove.Bounds(10,20,50,30),box);
      var match=HudPeerSnap.nearest(48,box.width(),200,50,50,5);
      assertNotNull(match);
      var delta=new HudGroupMove.Delta(match.position()-box.x(),5);
      assertTrue(HudGroupMove.fits(members,200,100,delta));
      assertEquals(30F,(members.get(1).x()+delta.x())-(members.get(0).x()+delta.x()),0.0001F);
      assertFalse(HudGroupMove.fits(members,95,100,delta));
   }

   @Test void groupEnvelopeAndFitRejectInvalidGeometry() {
      assertNull(HudGroupMove.envelope(java.util.List.of()));
      assertNull(HudGroupMove.envelope(java.util.List.of(new HudGroupMove.Bounds(Float.NaN,0,1,1))));
      assertNull(HudGroupMove.envelope(java.util.List.of(new HudGroupMove.Bounds(Float.MAX_VALUE,0,Float.MAX_VALUE,1))));
      var valid=java.util.List.of(new HudGroupMove.Bounds(0,0,20,20));
      assertTrue(HudGroupMove.fits(valid,100,100,new HudGroupMove.Delta(80,80)));
      assertFalse(HudGroupMove.fits(valid,100,100,new HudGroupMove.Delta(81,80)));
      assertFalse(HudGroupMove.fits(valid,100,100,new HudGroupMove.Delta(Float.NaN,0)));
   }
}
