package dev.schizo.client.gui.screens;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DashboardTextTest {
   @Test void fitsActualMeasuredWidthAndPreservesCodePoints() {
      var cache=new DashboardText();
      cache.begin(new Object(),0);
      assertEquals("😀…",cache.fit("😀😀😀",3,String::length).text());
      assertEquals("",cache.fit("long",0.5F,String::length).text());
      var result=cache.fit("WWWiii",5,s->s.codePoints().map(c->c=='W'?3:1).sum());
      assertEquals("W…",result.text());
      assertTrue(result.width()<=5);
   }

   @Test void invalidatesForWidthFontAndPeriodicResourceRefresh() {
      var cache=new DashboardText();
      var font=new Object();
      cache.begin(font,0);
      AtomicInteger reads=new AtomicInteger();
      java.util.function.ToDoubleFunction<String> measure=s->{reads.incrementAndGet();return s.length();};
      var first=cache.fit("sample",10,measure);
      int count=reads.get();
      assertSame(first,cache.fit("sample",10,measure));
      assertEquals(count,reads.get());
      assertNotSame(first,cache.fit("sample",4,measure));
      cache.begin(new Object(),1);
      var second=cache.fit("sample",10,measure);
      assertNotSame(first,second);
      cache.begin(null,2);
      assertNotSame(cache.fit("sample",10,measure),cache.fit("sample",10,measure));
      cache.begin(font,3);
      var third=cache.fit("sample",10,measure);
      cache.begin(font,500_000_003L);
      assertNotSame(third,cache.fit("sample",10,measure));
   }

   @Test void cacheDoesNotRetainAnUnboundedNumberOfLabels() {
      var cache=new DashboardText();
      cache.begin(new Object(),0);
      var first=cache.fit("first",100,String::length);
      for(int i=0;i<256;i++) cache.fit("label"+i,100,String::length);
      assertNotSame(first,cache.fit("first",100,String::length));
   }
}
