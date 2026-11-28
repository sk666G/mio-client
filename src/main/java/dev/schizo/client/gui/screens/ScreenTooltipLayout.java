package dev.schizo.client.gui.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Pure tooltip positioning math shared by client screens. */
final class ScreenTooltipLayout {
   private ScreenTooltipLayout() {
   }

   static float clampStart(float preferredStart, float extent, float viewportExtent) {
      if (!(extent > 0.0F) || !(viewportExtent > 0.0F)) {
         return preferredStart;
      }

      float maximumStart = Math.max(0.0F, viewportExtent - extent);
      return Math.max(0.0F, Math.min(preferredStart, maximumStart));
   }

   record Wrapped(List<String> lines, float width) {}

   /** Word-aware wrapping with code-point-safe hard breaks and bounded work. */
   static Wrapped wrap(String text, float maxWidth, int maxLines, ToDoubleFunction<String> measure) {
      if (text == null || text.isBlank() || !Float.isFinite(maxWidth) || maxWidth<=0 || maxLines<=0)
         return new Wrapped(List.of(),0F);
      int limit=Math.min(24,maxLines);
      boolean truncated=text.length()>4096;
      if(truncated) {
         int end=4096;
         if(Character.isHighSurrogate(text.charAt(end-1)) && Character.isLowSurrogate(text.charAt(end))) end--;
         text=text.substring(0,end);
      }
      var lines=new ArrayList<String>();
      String[] paragraphs=text.split("\\R",-1);
      outer: for(String paragraph:paragraphs) {
         if(lines.size()>=limit) { truncated=true; break; }
         String rest=paragraph.strip();
         if(rest.isEmpty()) { lines.add(""); continue; }
         while(!rest.isEmpty()) {
            if(lines.size()>=limit) { truncated=true; break outer; }
            int end=fitPrefix(rest,maxWidth,measure);
            if(end==0) { truncated=true; break outer; }
            int next=end;
            if(end<rest.length()) {
               for(int i=end;i>0;) {
                  int cp=rest.codePointBefore(i);
                  i-=Character.charCount(cp);
                  if(Character.isWhitespace(cp) && i>0) { end=i; next=i+Character.charCount(cp); break; }
               }
            }
            lines.add(rest.substring(0,end).stripTrailing());
            rest=rest.substring(next).stripLeading();
         }
      }
      if(truncated && measure.applyAsDouble("…")<=maxWidth) {
         if(lines.size()<limit) lines.add("…");
         else {
            String last=lines.removeLast();
            float room=maxWidth-(float)measure.applyAsDouble("…");
            int end=fitPrefix(last,room,measure);
            String finalLine=last.substring(0,end).stripTrailing()+"…";
            lines.add(measure.applyAsDouble(finalLine)<=maxWidth?finalLine:"…");
         }
      }
      float width=0F;
      for(String line:lines) width=Math.max(width,(float)measure.applyAsDouble(line));
      return new Wrapped(List.copyOf(lines),width);
   }

   private static int fitPrefix(String text,float width,ToDoubleFunction<String> measure) {
      if(width<=0 || text.isEmpty()) return 0;
      if(measure.applyAsDouble(text)<=width) return text.length();
      int low=0,high=text.codePointCount(0,text.length());
      while(low<high) {
         int mid=low+(high-low+1)/2;
         int end=text.offsetByCodePoints(0,mid);
         if(measure.applyAsDouble(text.substring(0,end))<=width) low=mid;
         else high=mid-1;
      }
      return text.offsetByCodePoints(0,low);
   }
}
