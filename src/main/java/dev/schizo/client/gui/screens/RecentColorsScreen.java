package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.PaletteHex;
import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A local swatch browser: choosing a color changes only the parent picker draft. */
final class RecentColorsScreen extends StudioScreen {
   private final StudioScreen parent;
   private final List<Color> colors;
   private final Consumer<Color> accepted;
   private int left,top,panelWidth,column;
   private boolean compact;

   RecentColorsScreen(StudioScreen parent,List<Color> colors,Consumer<Color> accepted) {
      super(Component.literal("Recent colors"));
      this.parent=parent;
      this.colors=List.copyOf(colors);
      this.accepted=accepted;
   }

   @Override protected void init() {
      super.init();
      panelWidth=Math.max(1,Math.min(340,width-16));
      left=(width-panelWidth)/2;
      top=Math.max(4,(height-142)/2);
      compact=panelWidth<210 || height<150;
      if(!compact) {
         column=(panelWidth-6)/2;
         for(int index=0;index<colors.size();index++) {
            Color color=colors.get(index);
            int x=cellX(index),y=cellY(index);
            trackResizeFocus("color:"+Integer.toUnsignedString(color.getRGB()),addRenderableWidget(
               new StudioButton(x+24,y,column-24,18,Component.literal(PaletteHex.format(color)),ignored -> {
                  accepted.accept(color);
                  onClose();
               }).help("Use this color in the detached picker draft.")));
         }
      }
      trackResizeFocus("back",addRenderableWidget(new StudioButton(left,compact?Math.max(1,height-25):top+118,
         panelWidth,18,Component.literal("Back"),ignored -> onClose())));
   }

   private int cellX(int index) { return left+(index%2)*(column+6); }
   private int cellY(int index) { return top+29+(index/2)*22; }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      var theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) {
         graphics.drawString(font,font.plainSubstrByWidth("Reduce GUI scale to browse colors",panelWidth),left,top,0xffeeeeef,false);
      } else {
         graphics.fill(left-4,top,left+panelWidth+4,top+142,theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         graphics.drawString(font,"RECENT COLORS",left,top+6,theme.accent(),false);
         graphics.drawString(font,font.plainSubstrByWidth("Session only | Accepted picker colors",panelWidth),left,top+17,0xffb8b9c7,false);
         if(colors.isEmpty()) graphics.drawString(font,"No accepted colors yet.",left,top+34,0xffb8b9c7,false);
         for(int index=0;index<colors.size();index++) {
            int x=cellX(index),y=cellY(index);
            Color color=colors.get(index);
            graphics.fill(x,y,x+20,y+18,0xff444651);
            for(int iy=0;iy<16;iy+=4) for(int ix=0;ix<18;ix+=4) {
               graphics.fill(x+1+ix,y+1+iy,x+1+Math.min(ix+4,18),y+1+iy+4,
                  ((ix/4+iy/4)&1)==0?0xff777781:0xff303139);
            }
            graphics.fill(x+1,y+1,x+19,y+17,color.getRGB());
         }
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() { returnToParent(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
