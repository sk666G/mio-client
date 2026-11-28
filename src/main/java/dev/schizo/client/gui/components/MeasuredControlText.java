package dev.schizo.client.gui.components;

import java.util.function.ToDoubleFunction;

/** Fit a control label by measured width, preserving Unicode code-point boundaries. */
final class MeasuredControlText {
   private MeasuredControlText() {}

   static String fit(String text,float available,ToDoubleFunction<String> measure) {
      if(!Float.isFinite(available)||available<=0) return "";
      if(fits(text,available,measure)) return text;
      String suffix="...";
      if(!fits(suffix,available,measure)) return "";
      int low=0,high=Math.min(4096,text.codePointCount(0,text.length()));
      while(low<high) {
         int mid=low+(high-low+1)/2;
         String candidate=text.substring(0,text.offsetByCodePoints(0,mid))+suffix;
         if(fits(candidate,available,measure)) low=mid;
         else high=mid-1;
      }
      String result=text.substring(0,text.offsetByCodePoints(0,low))+suffix;
      // Font substitutions and kerning are not necessarily monotonic.
      return fits(result,available,measure)?result:suffix;
   }

   private static boolean fits(String value,float width,ToDoubleFunction<String> measure) {
      double measured=measure.applyAsDouble(value);
      return Double.isFinite(measured)&&measured>=0&&measured<=width;
   }
}
