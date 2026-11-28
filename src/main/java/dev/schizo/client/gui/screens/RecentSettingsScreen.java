package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.RecentVisits;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.Setting;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Explicitly visited inspector settings, retained only during the current client process. */
public final class RecentSettingsScreen extends StudioScreen {
   private static final RecentVisits<Module,Setting<?>> HISTORY = new RecentVisits<>(32);
   private final Screen parent;
   private List<RecentVisits.Visit<Module,Setting<?>>> rows=List.of();
   private int selected,offset,rowCount,left,top,panelWidth,rowsY,footerY,statusY,panelBottom;
   private boolean compact,confirming;
   private Button open,previous,next,clear;
   private EditBox search;
   private String query="";

   public RecentSettingsScreen(Screen parent) {
      super(Component.literal("Visited settings"));
      this.parent=parent;
   }

   static void record(Module module,Setting<?> setting) {
      if (module!=null && setting!=null && !setting.isGroupOnly() && !setting.isExcludedFromConfig()) HISTORY.record(module,setting);
   }

   public static boolean wasVisited(Module module,Setting<?> setting) {
      return HISTORY.snapshot().stream().anyMatch(v -> v.owner()==module && v.item()==setting);
   }

   private static boolean registered(RecentVisits.Visit<Module,Setting<?>> visit) {
      if (ClientContext.moduleRegistry==null) return false;
      for (Object entry : ClientContext.moduleRegistry.getRegistry()) {
         if (entry==visit.owner()) {
            return visit.owner().getSettings().stream().anyMatch(setting -> setting==visit.item())
               && !visit.item().isGroupOnly() && !visit.item().isExcludedFromConfig();
         }
      }
      return false;
   }

   @Override protected void init() {
      super.init();
      open=previous=next=clear=null; search=null; confirming=false;
      var layout=StudioViewportLayouts.visited(width,height);
      panelWidth=layout.width(); left=layout.left(); top=layout.top();
      compact=layout.compact(); rowCount=layout.rows();
      rowsY=layout.rowsY(); footerY=layout.footerY();
      statusY=layout.statusY(); panelBottom=layout.panelBottom();
      if(compact) {button("Back",left,Math.max(1,height-25),panelWidth,this::onClose);return;}
      search = trackResizeFocus("search", addRenderableWidget(new StudioEditBox(font,left,layout.searchY(),panelWidth,20,Component.literal("Search visited settings"))));
      search.setMaxLength(64); search.setValue(query);
      search.setResponder(text -> {query=text;selected=0;offset=0;confirming=false;reload(false);});
      int y=footerY, w=layout.stacked()?(panelWidth-8)/3:(panelWidth-16)/5;
      previous=button("Previous",left,y,w,() -> move(-1));
      next=button("Next",left+w+4,y,w,() -> move(1));
      open=button("Open",left+2*(w+4),y,w,this::open);
      int actionWidth=layout.stacked()?(panelWidth-4)/2:w;
      int actionX=layout.stacked()?left:left+3*(w+4);
      int actionY=y+(layout.stacked()?23:0);
      clear=button("Clear",actionX,actionY,actionWidth,() -> {
         if(confirming) { HISTORY.clear(); confirming=false; reload(false); }
         else {confirming=true;refresh();}
      });
      button("Back",actionX+actionWidth+4,actionY,actionWidth,this::onClose);
      reload();
      setInitialFocus(search);
   }

   private Button button(String label,int x,int y,int w,Runnable action) {
      return trackResizeFocus("button:" + label, addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(label),ignored -> action.run())));
   }

   private void reload() { reload(true); }

   private void reload(boolean preserveSelection) {
      var previousVisit = preserveSelection && selected>=0 && selected<rows.size() ? rows.get(selected) : null;
      rows=HISTORY.snapshot().stream().filter(RecentSettingsScreen::registered)
         .filter(v -> PaletteMatcher.score(v.item().getName(),v.owner().getName()+" "+v.item().getConfigName(),query)>=0).toList();
      selected=RecentVisits.restoreSelection(rows,previousVisit,preserveSelection?selected:0);
      refresh();
   }

   private void refresh() {
      if(open==null)return;
      offset=InspectorRows.reveal(selected,offset,rowCount,rows.size());
      open.active=!rows.isEmpty();
      previous.active=open.active && selected>0;
      next.active=open.active && selected+1<rows.size();
      clear.active=!HISTORY.snapshot().isEmpty();
      clear.setMessage(Component.literal(confirming?"Confirm":"Clear"));
   }

   private void move(int delta) {
      int index=InspectorRows.step(selected,delta,rows.size());
      if(index<0)return;
      selected=index;
      confirming=false;refresh();
   }

   private void open() {
      if(rows.isEmpty())return;
      var visit=rows.get(selected);
      confirming=false;
      if(!registered(visit)) {reload();return;}
      openStudioChild(new ModuleInspectorScreen(this,visit.owner(),visit.item()));
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if(!compact && (getFocused()==null || getFocused()==search)) {
         switch(event.key()) {
            case GLFW.GLFW_KEY_UP -> {move(-1);return true;}
            case GLFW.GLFW_KEY_DOWN -> {move(1);return true;}
            case GLFW.GLFW_KEY_PAGE_UP -> {move(-rowCount);return true;}
            case GLFW.GLFW_KEY_PAGE_DOWN -> {move(rowCount);return true;}
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {confirming=false;refresh();open();return true;}
         }
      }
      return super.keyPressed(event);
   }

   @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
      if(!compact && event.button()==0) {
         int row=InspectorRows.hit(event.x(),event.y(),left,rowsY,panelWidth,rowCount,offset,rows.size());
         if(row>=0) {selected=row;confirming=false;refresh();setFocused(null);return true;}
      }
      return super.mouseClicked(event,doubleClick);
   }

   @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
      if(!compact && vertical!=0 && x>=left && x<left+panelWidth && y>=rowsY && y<footerY) {
         offset=Math.max(0,Math.min(Math.max(0,rows.size()-rowCount),offset+(vertical<0?1:-1)));return true;
      }
      return super.mouseScrolled(x,y,horizontal,vertical);
   }

   private void line(GuiGraphics graphics,String text,int y,int color) {
      graphics.drawString(font,font.plainSubstrByWidth(text,panelWidth-4),left+2,y,color,false);
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      StudioScreenTheme theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) line(graphics,"Reduce GUI scale to browse visited settings",top,0xffeeeeef);
      else {
         graphics.fill(left-4,top,left+panelWidth+4,Math.min(height,panelBottom),theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"SCHIZO / VISITED SETTINGS",top+6,theme.accent());
         line(graphics,"Session only | Explicit inspector selections",top+18,0xffb8b9c7);
         if(rows.isEmpty())line(graphics,"No matching visited settings.",rowsY+2,0xffb8b9c7);
         for(int i=0;i<rowCount && offset+i<rows.size();i++) {
            int index=offset+i,y=rowsY+i*13;
            var v=rows.get(index);
            graphics.fill(left,y,left+panelWidth,y+12,index==selected?theme.selected():theme.row());
            if(index==selected)graphics.fill(left,y,left+2,y+12,theme.accent());
            line(graphics,v.owner().getName()+" / "+v.item().getName(),y+2,0xffeeeeef);
         }
         line(graphics,confirming?"Confirm clearing all visited settings.":"Opening a setting never changes its value.",statusY,0xffb8b9c7);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose(){returnToParent(parent);}
   @Override public boolean isPauseScreen(){return false;}
}
