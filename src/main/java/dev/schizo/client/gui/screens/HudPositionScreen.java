package dev.schizo.client.gui.screens;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Detached property editor; the parent validates each explicit layout operation. */
final class HudPositionScreen extends StudioScreen {
   private final Screen parent;
   private final String widgetName;
   private final List<String> metadata;
   private final Function<HudPositionInput,String> apply;
   private final Function<Float,String> applyScale;
   private final Runnable restored;
   private String xDraft,yDraft,scaleDraft,notice="Moving detaches the anchor. Scaling preserves it.";
   private final String bounds;
   private EditBox xField,yField,scaleField;
   private int left,top,panelWidth;
   private boolean compact,condensed;
   private int body,fieldY,positionY,scaleY,backY;

   HudPositionScreen(Screen parent,String widgetName,float x,float y,float maxX,float maxY,
                     List<String> metadata,Float scale,Function<HudPositionInput,String> apply,
                     Function<Float,String> applyScale,Runnable restored) {
      super(Component.literal("HUD widget properties"));
      this.parent=parent;
      this.widgetName=widgetName;
      this.metadata=List.copyOf(metadata);
      this.xDraft=Float.toString(x);
      this.yDraft=Float.toString(y);
      this.scaleDraft=scale==null?"Read-only":Float.toString(scale*100F);
      this.bounds="GUI pixels | X: 0 – "+Float.toString(maxX)+" | Y: 0 – "+Float.toString(maxY);
      this.apply=apply;
      this.applyScale=applyScale;
      this.restored=restored;
   }

   @Override protected void init() {
      super.init();
      xField=yField=scaleField=null;
      panelWidth=Math.max(1,Math.min(420,width-16));
      left=(width-panelWidth)/2;
      condensed=height<212;
      body=condensed?130:204;
      top=Math.max(4,(height-body)/2);
      compact=panelWidth<220 || height<138;
      fieldY=top+(condensed?39:76);
      positionY=top+(condensed?62:100);
      scaleY=top+(condensed?85:136);
      backY=top+(condensed?108:179);
      if(compact) {
         button("Back",left,Math.max(1,height-25),panelWidth,this::onClose);
         return;
      }
      int half=(panelWidth-4)/2;
      xField=field("x","X coordinate",xDraft,left,fieldY,half,text->xDraft=text);
      yField=field("y","Y coordinate",yDraft,left+half+4,fieldY,half,text->yDraft=text);
      button("Apply position",left,positionY,panelWidth,this::commit).help("Apply only X/Y. The scale draft is not applied.");
      if(applyScale!=null) {
         int small=(panelWidth-8)/3;
         scaleField=field("scale","Scale percent",scaleDraft,left,scaleY,small,text->scaleDraft=text);
         button("100%",left+small+4,scaleY,small,()->scaleField.setValue("100")).help("Reset the draft to 100%. Apply scale to commit.");
         button("Apply scale",left+2*(small+4),scaleY,small,this::commitScale).help("Apply only scale (50–200%). Preserve the current anchor.");
      }
      button("Back",left,backY,panelWidth,this::onClose).help("Discard uncommitted drafts.");
      setInitialFocus(xField);
   }

   private EditBox field(String role,String label,String draft,int x,int y,int w,java.util.function.Consumer<String> responder) {
      var field=trackResizeFocus(role,addRenderableWidget(new StudioEditBox(font,x,y,w,20,Component.literal(label))));
      field.setMaxLength(24);
      field.setValue(draft);
      field.setResponder(responder);
      return field;
   }

   private StudioButton button(String label,int x,int y,int w,Runnable action) {
      return trackResizeFocus("button:"+label,addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(label),ignored->action.run())));
   }

   private void commit() {
      var position=HudPositionInput.parse(xDraft,yDraft);
      if(position==null) {notice="Enter two finite coordinates.";return;}
      String error=apply.apply(position);
      if(error!=null) notice=error;
      else onClose();
   }

   private void commitScale() {
      Float scale=HudPositionInput.parseScalePercent(scaleDraft);
      if(scale==null) {notice="Enter a finite scale between 50 and 200 percent.";return;}
      String error=applyScale.apply(scale);
      if(error!=null) notice=error;
      else onClose();
   }

   private void line(GuiGraphics graphics,String text,int x,int y,int w,int color) {
      graphics.drawString(font,font.plainSubstrByWidth(text,Math.max(0,w)),x,y,color,false);
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      var theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) line(graphics,"Reduce GUI scale to inspect HUD properties",left,top,panelWidth,0xffeeeeef);
      else {
         graphics.fill(left-4,top,left+panelWidth+4,top+body,theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"HUD PROPERTIES / "+widgetName,left,top+6,panelWidth,theme.accent());
         if(!condensed) {
            for(int i=0;i<Math.min(3,metadata.size());i++)
               line(graphics,metadata.get(i),left,top+18+i*11,panelWidth,0xffb8b9c7);
            line(graphics,bounds,left,top+51,panelWidth,0xffb8b9c7);
         }
         int half=(panelWidth-4)/2;
         line(graphics,"X coordinate",left,fieldY-11,half,0xffeeeeef);
         line(graphics,"Y coordinate",left+half+4,fieldY-11,half,0xffeeeeef);
         if(!condensed || applyScale==null) line(graphics,applyScale==null?"Scale: read-only":"Scale percent (50–200)",left,condensed?scaleY+5:scaleY-11,panelWidth,0xffeeeeef);
         line(graphics,notice,left,top+(condensed?17:165),panelWidth,0xffb8b9c7);
         if(condensed && rowHelpReady(mouseX>=left&&mouseX<left+panelWidth&&mouseY>=top&&mouseY<top+16?metadata:null))
            showRowHelp(graphics,String.join("\n",metadata)+"\n"+bounds,mouseX,mouseY);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() {
      returnToParent(parent);
      if(minecraft.screen==parent) restored.run();
   }
   @Override public boolean isPauseScreen() { return false; }
}
