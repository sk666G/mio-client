package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Detached color sample gallery with individual contrast diagnostics. */
final class ThemeGalleryScreen extends StudioScreen {
   private final StudioScreen parent;
   private final UiPalette palette;
   private boolean light,chrome;
   private List<ThemeContrastReport.Pair> pairs=List.of();
   private List<String> scores=List.of();
   private boolean[] meets=new boolean[0];
   private int left,top,panelWidth;
   private boolean compact;
   private StudioButton mode,backdrop;

   ThemeGalleryScreen(StudioScreen parent,UiPalette palette,boolean light) {
      super(Component.literal("Theme preview gallery"));
      this.parent=parent;this.palette=palette;this.light=light;
   }

   @Override protected void init() {
      super.init();
      panelWidth=Math.max(1,Math.min(480,width-16));
      left=(width-panelWidth)/2;
      top=Math.max(4,(height-208)/2);
      compact=panelWidth<240||height<216;
      mode=backdrop=null;
      if(compact) {
         button("back","Back",left,Math.max(0,height-24),panelWidth,this::onClose);
         return;
      }
      int half=(panelWidth-4)/2;
      mode=button("mode","",left,top+24,half,()->{chrome=!chrome;refresh();});
      backdrop=button("backdrop","",left+half+4,top+24,half,()->{light=!light;refresh();});
      button("back","Back",left,top+186,panelWidth,this::onClose);
      refresh();
   }

   private StudioButton button(String role,String text,int x,int y,int w,Runnable action) {
      return trackResizeFocus("button:"+role,addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(text),ignored->action.run())));
   }

   private void refresh() {
      pairs=chrome?ThemeContrastReport.chrome(palette):ThemeContrastReport.preview(palette,light);
      var labels=new java.util.ArrayList<String>(pairs.size());
      meets=new boolean[pairs.size()];
      for(int i=0;i<pairs.size();i++) {
         var p=pairs.get(i);
         double ratio=p.ratio();
         meets[i]=ratio>=p.target();
         labels.add(String.format(Locale.ROOT,"%.2f:1 / %.1f %s",ratio,p.target(),meets[i]?"OK":"LOW"));
      }
      scores=List.copyOf(labels);
      mode.setMessage(Component.literal(chrome?"Show palette samples":"Show Studio chrome"));
      backdrop.setMessage(Component.literal(light?"Light backdrop":"Dark backdrop"));
      backdrop.active=!chrome;
      backdrop.help(chrome?"Studio chrome is opaque. Backdrop affects only palette samples.":"Change the reference backdrop, not the saved palette.");
   }

   private void line(GuiGraphics graphics,String text,int x,int y,int available,int color) {
      graphics.drawString(font,font.plainSubstrByWidth(text,Math.max(0,available)),x,y,color,false);
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      var theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) {
         line(graphics,"Reduce GUI scale to view the sample gallery",left,top,panelWidth,0xffeeeeef);
         super.render(graphics,mouseX,mouseY,partialTick);
         return;
      }
      graphics.fill(left-4,top,left+panelWidth+4,top+208,theme.panel());
      graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
      line(graphics,"THEME GALLERY / "+(chrome?"STUDIO CHROME":"PALETTE SAMPLE"),left,top+6,panelWidth,theme.accent());
      line(graphics,"Read-only | Reference colors, not live game captures",left,top+15,panelWidth,0xffb8b9c7);
      int labelWidth=panelWidth*43/100,scoreWidth=panelWidth*32/100;
      int sampleX=left+labelWidth,sampleRight=left+panelWidth-scoreWidth-4;
      for(int i=0;i<pairs.size();i++) {
         var p=pairs.get(i);
         int y=top+49+i*18;
         line(graphics,p.label(),left,y+4,labelWidth-4,0xffeeeeef);
         graphics.fill(sampleX,y,sampleRight,y+16,p.background());
         if(p.target()==3.0) {
            graphics.fill(sampleX+3,y+3,sampleRight-3,y+5,p.foreground());
            graphics.fill(sampleX+3,y+11,sampleRight-3,y+13,p.foreground());
         } else line(graphics,"Aa",sampleX+4,y+4,sampleRight-sampleX-8,p.foreground());
         line(graphics,scores.get(i),sampleRight+4,y+4,scoreWidth,meets[i]?0xffb8b9c7:0xffffa66b);
      }
      int index=mouseX>=left&&mouseX<left+panelWidth&&mouseY>=top+49?(mouseY-top-49)/18:-1;
      var hovered=index>=0&&index<pairs.size()?pairs.get(index):null;
      if(rowHelpReady(hovered)) showRowHelp(graphics,hovered.label()+"\n"+scores.get(index)
         +"\nAdvisory target: "+(hovered.target()==3?"non-text indicator":"normal text")+". Saved colors are unchanged.",mouseX,mouseY);
      line(graphics,"4.5:1 text / 3:1 indicators | Advisory targets only",left,top+176,panelWidth,0xffb8b9c7);
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() {returnToParent(parent);}
   @Override public boolean isPauseScreen() {return false;}
}
