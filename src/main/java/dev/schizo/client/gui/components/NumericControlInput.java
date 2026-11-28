package dev.schizo.client.gui.components;

/** Pure numeric input policy for legacy and docked slider controls. */
final class NumericControlInput {
   private NumericControlInput() {}

   static Double parse(String input,double min,double max,boolean whole) {
      if(!validRange(min,max)) return null;
      try {
         double value=Double.parseDouble(input.strip());
         if(!Double.isFinite(value)||value<min||value>max||whole&&value!=Math.rint(value)) return null;
         return value;
      } catch(NumberFormatException invalid) {return null;}
   }

   static double slider(double pointer,double left,double width,double min,double max,boolean whole) {
      if(!Double.isFinite(pointer)||!Double.isFinite(left)||!Double.isFinite(width)||width<=0
         ||!validRange(min,max)||max<=min||!Double.isFinite(max-min)) return Double.NaN;
      double fraction=Math.max(0,Math.min(1,(pointer-left)/width));
      double value=fraction==0?min:fraction==1?max:min+fraction*(max-min);
      if(whole) {
         double lower=Math.ceil(min),upper=Math.floor(max);
         if(lower>upper) return Double.NaN;
         return Math.max(lower,Math.min(upper,Math.floor(value+0.5D)));
      }
      if(fraction>0&&fraction<1&&Math.abs(value)<=Long.MAX_VALUE/10D) value=Math.round(value*10D)/10D;
      return Math.max(min,Math.min(max,value));
   }

   private static boolean validRange(double min,double max) {
      return Double.isFinite(min)&&Double.isFinite(max)&&min<=max;
   }
}
