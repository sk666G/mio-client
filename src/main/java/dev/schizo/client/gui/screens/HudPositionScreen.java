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
   private boolean compact;

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
      top=Math.max(4,(height-204)/2);
      compact=panelWidth<240 || height<212;
      if(compact) {
         button("Back",left,Math.max(1,height-25),panelWidth,this::onClose);
         return;
      }
      int half=(panelWidth-4)/2;
      xField=field("x","X coordinate",xDraft,left,top+76,half,text->xDraft=text);
      yField=field("y","Y coordinate",yDraft,left+half+4,top+76,half,text->yDraft=text);
      button("Apply position",left,top+100,panelWidth,this::commit).help("Apply only X/Y. The scale draft is not applied.");
      if(applyScale!=null) {
         int small=(panelWidth-8)/3;
         scaleField=field("scale","Scale percent",scaleDraft,left,top+136,small,text->scaleDraft=text);
         button("100%",left+small+4,top+136,small,()->scaleField.setValue("100")).help("Reset the draft to 100%. Apply scale to commit.");
         button("Apply scale",left+2*(small+4),top+136,small,this::commitScale).help("Apply only scale (50–200%). Preserve the current anchor.");
      }
      button("Back",left,top+179,panelWidth,this::onClose).help("Discard uncommitted drafts.");
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
         graphics.fill(left-4,top,left+panelWidth+4,top+204,theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"HUD PROPERTIES / "+widgetName,left,top+6,panelWidth,theme.accent());
         for(int i=0;i<Math.min(3,metadata.size());i++)
            line(graphics,metadata.get(i),left,top+18+i*11,panelWidth,0xffb8b9c7);
         line(graphics,bounds,left,top+51,panelWidth,0xffb8b9c7);
         int half=(panelWidth-4)/2;
         line(graphics,"X coordinate",left,top+65,half,0xffeeeeef);
         line(graphics,"Y coordinate",left+half+4,top+65,half,0xffeeeeef);
         line(graphics,applyScale==null?"Scale: read-only (widget has no size control)":"Scale percent (50–200)",left,top+125,panelWidth,0xffeeeeef);
         line(graphics,notice,left,top+165,panelWidth,0xffb8b9c7);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() {
      returnToParent(parent);
      if(minecraft.screen==parent) restored.run();
   }
   @Override public boolean isPauseScreen() { return false; }
}
