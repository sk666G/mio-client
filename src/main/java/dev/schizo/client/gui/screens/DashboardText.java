package dev.schizo.client.gui.screens;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Bounded, font-identity-aware measured labels for the legacy dashboard. */
final class DashboardText {
   private static final int LIMIT=256;
   private final Map<String,Fitted> cache=new HashMap<>();
   private Object fontIdentity;
   private long refreshedAt;
   record Fitted(String text,float width,float available) {}

   void begin(Object identity,long now) {
      if(identity==null || fontIdentity!=identity || now-refreshedAt>=500_000_000L) {
         cache.clear();
         fontIdentity=identity;
         refreshedAt=now;
      }
   }

   Fitted fit(String text,float available,ToDoubleFunction<String> measure) {
      if(!Float.isFinite(available) || available<=0) return new Fitted("",0,available);
      Fitted cached=cache.get(text);
      if(fontIdentity!=null && cached!=null && cached.available()==available) return cached;
      Fitted result=measure(text,available,measure);
      if(fontIdentity!=null) {
         if(cache.size()>=LIMIT && !cache.containsKey(text)) cache.clear();
         cache.put(text,result);
      }
      return result;
   }

   private static Fitted measure(String text,float available,ToDoubleFunction<String> measure) {
      float full=(float)measure.applyAsDouble(text);
      if(Float.isFinite(full) && full>=0 && full<=available) return new Fitted(text,full,available);
      final String ellipsis="…";
      float dots=(float)measure.applyAsDouble(ellipsis);
      if(!Float.isFinite(dots) || dots<0 || dots>available) return new Fitted("",0,available);
      int low=0,high=Math.min(4096,text.codePointCount(0,text.length()));
      while(low<high) {
         int mid=low+(high-low+1)/2;
         String candidate=text.substring(0,text.offsetByCodePoints(0,mid))+ellipsis;
         double width=measure.applyAsDouble(candidate);
         if(Double.isFinite(width) && width>=0 && width<=available) low=mid;
         else high=mid-1;
      }
      if(low==0) return new Fitted(ellipsis,dots,available);
      String candidate=text.substring(0,text.offsetByCodePoints(0,low))+ellipsis;
      float width=(float)measure.applyAsDouble(candidate);
      return Float.isFinite(width) && width>=0 && width<=available
         ? new Fitted(candidate,width,available):new Fitted(ellipsis,dots,available);
   }
}
