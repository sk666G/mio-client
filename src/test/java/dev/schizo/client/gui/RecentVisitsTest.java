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
}
