package dev.schizo.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecentVisitsTest {
   @Test void deduplicatesByIdentityAndBoundsMostRecentFirst() {
      var history=new RecentVisits<Object,Object>(2);
      Object a=new String("owner"), b=new String("owner"), x=new String("setting"), y=new String("setting");
      history.record(a,x);history.record(b,x);
      assertEquals(2,history.snapshot().size());
      history.record(a,x);
      assertSame(a,history.snapshot().getFirst().owner());
      history.record(a,y);
      assertSame(y,history.snapshot().getFirst().item());
      assertSame(x,history.snapshot().getLast().item());
      assertEquals(2,history.snapshot().size());
      history.clear();assertTrue(history.snapshot().isEmpty());
   }

   @Test void restoresOpenedVisitAfterItMovesToFront() {
      var history=new RecentVisits<Object,Object>(4);
      Object owner=new Object(), first=new Object(), second=new Object(), third=new Object();
      history.record(owner,first);
      history.record(owner,second);
      history.record(owner,third);
      var selected=history.snapshot().get(2);
      history.record(owner,first);
      assertEquals(0,RecentVisits.restoreSelection(history.snapshot(),selected,2));
      assertSame(first,history.snapshot().get(0).item());
   }

   @Test void restorationUsesIdentityRatherThanEqualNames() {
      var history=new RecentVisits<Object,Object>(4);
      Object a=new String("module"), b=new String("module");
      Object x=new String("setting"), y=new String("setting");
      history.record(a,x);
      var selected=history.snapshot().getFirst();
      history.record(a,y);
      history.record(b,x);
      assertEquals(2,RecentVisits.restoreSelection(history.snapshot(),selected,0));
   }

   @Test void missingVisitFallsBackToBoundedIndexAndEmptyHistoryIsSafe() {
      var history=new RecentVisits<Object,Object>(1);
      Object owner=new Object();
      history.record(owner,new Object());
      var selected=history.snapshot().getFirst();
      history.record(owner,new Object());
      assertEquals(0,RecentVisits.restoreSelection(history.snapshot(),selected,99));
      assertEquals(0,RecentVisits.restoreSelection(history.snapshot(),null,-1));
      history.clear();
      assertEquals(0,RecentVisits.restoreSelection(history.snapshot(),selected,99));
   }
}
