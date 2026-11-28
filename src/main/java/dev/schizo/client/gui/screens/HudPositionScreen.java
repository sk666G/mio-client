package dev.schizo.client.gui.screens;

import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit detached coordinate edit. The parent validates the live layout before committing. */
final class HudPositionScreen extends StudioScreen {
   private final Screen parent;
   private final String widgetName;
   private final Function<HudPositionInput,String> apply;
   private final Runnable restored;
   private String xDraft,yDraft,notice="Applying a move detaches the widget from its anchor.";
   private final String bounds;
   private EditBox xField,yField;
   private int left,top,panelWidth;
   private boolean compact;

   HudPositionScreen(Screen parent,String widgetName,float x,float y,float maxX,float maxY,
                     Function<HudPositionInput,String> apply,Runnable restored) {
      super(Component.literal("HUD widget position"));
      this.parent=parent;
      this.widgetName=widgetName;
      this.xDraft=Float.toString(x);
      this.yDraft=Float.toString(y);
      this.bounds="GUI pixels | X: 0 – "+Float.toString(maxX)+" | Y: 0 – "+Float.toString(maxY);
      this.apply=apply;
      this.restored=restored;
   }

   @Override protected void init() {
      super.init();
      xField=yField=null;
      panelWidth=Math.max(1,Math.min(380,width-16));
      left=(width-panelWidth)/2;
      top=Math.max(4,(height-130)/2);
      compact=panelWidth<220 || height<138;
      if(compact) {
         button("Back",left,Math.max(1,height-25),panelWidth,this::onClose);
         return;
      }
      int half=(panelWidth-4)/2;
      xField=trackResizeFocus("x",addRenderableWidget(new StudioEditBox(font,left,top+43,half,20,Component.literal("X coordinate"))));
      yField=trackResizeFocus("y",addRenderableWidget(new StudioEditBox(font,left+half+4,top+43,half,20,Component.literal("Y coordinate"))));
      xField.setMaxLength(24); yField.setMaxLength(24);
      xField.setValue(xDraft); yField.setValue(yDraft);
      xField.setResponder(text->xDraft=text);
      yField.setResponder(text->yDraft=text);
      button("Apply position",left,top+88,half,this::commit).help("Validate both coordinates and record one reversible layout edit.");
      button("Cancel",left+half+4,top+88,half,this::onClose);
      setInitialFocus(xField);
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

   private void line(GuiGraphics graphics,String text,int x,int y,int w,int color) {
      graphics.drawString(font,font.plainSubstrByWidth(text,Math.max(0,w)),x,y,color,false);
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      var theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) line(graphics,"Reduce GUI scale to edit position",left,top,panelWidth,0xffeeeeef);
      else {
         graphics.fill(left-4,top,left+panelWidth+4,top+130,theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"HUD POSITION / "+widgetName,left,top+6,panelWidth,theme.accent());
         line(graphics,bounds,left,top+18,panelWidth,0xffb8b9c7);
         int half=(panelWidth-4)/2;
         line(graphics,"X coordinate",left,top+32,half,0xffeeeeef);
         line(graphics,"Y coordinate",left+half+4,top+32,half,0xffeeeeef);
         line(graphics,notice,left,top+70,panelWidth,0xffb8b9c7);
         line(graphics,"Layout history retains the previous anchor and position.",left,top+113,panelWidth,0xffb8b9c7);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() {
      returnToParent(parent);
      if(minecraft.screen==parent) restored.run();
   }
   @Override public boolean isPauseScreen() { return false; }
}
