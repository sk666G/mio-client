package dev.schizo.client.render;

/** Pure same-edge and center snapping. Screen/grid policy remains with the caller. */
public final class HudPeerSnap {
   private HudPeerSnap() {}

   public record Match(float position,float line,int edge,float distance) {}

   public static float line(float position,float extent,int edge) {
      return position+extent*(edge*0.5F);
   }

   public static Match nearest(float position,float extent,float viewport,
                               float peerPosition,float peerExtent,float threshold) {
      if(!Float.isFinite(position)||!Float.isFinite(extent)||!Float.isFinite(viewport)
         ||!Float.isFinite(peerPosition)||!Float.isFinite(peerExtent)||!Float.isFinite(threshold)
         ||extent<=0||peerExtent<=0||viewport<=0||threshold<0
         ||extent>viewport||peerPosition<0||(double)peerPosition+peerExtent>viewport) return null;
      Match best=null;
      for(int edge=0;edge<3;edge++) {
         float guide=line(peerPosition,peerExtent,edge);
         float next=guide-extent*(edge*0.5F);
         float distance=Math.abs(next-position);
         if(next>=0&&(double)next+extent<=viewport&&distance<=threshold)
            best=better(best,new Match(next,guide,edge,distance));
      }
      return best;
   }

   /** Closest displacement wins; ties prefer lower guide coordinate then leading edge. */
   public static Match better(Match first,Match next) {
      if(next==null) return first;
      if(first==null) return next;
      int distance=Float.compare(next.distance(),first.distance());
      if(distance!=0) return distance<0?next:first;
      int line=Float.compare(next.line(),first.line());
      if(line!=0) return line<0?next:first;
      return next.edge()<first.edge()?next:first;
   }

   public static boolean aligned(Match match,float position,float extent,float peerPosition,float peerExtent) {
      if(match==null||extent<=0||peerExtent<=0) return false;
      return Math.abs(line(position,extent,match.edge())-match.line())<=0.01F
         && Math.abs(line(peerPosition,peerExtent,match.edge())-match.line())<=0.01F;
   }
}
