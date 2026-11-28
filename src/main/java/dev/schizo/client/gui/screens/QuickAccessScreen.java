package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ModulePreferencesRegistry;
import dev.schizo.client.modules.Module;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

/** Local navigation and preference management; opening a module never toggles it. */
public final class QuickAccessScreen extends StudioScreen {
   private enum View { FAVORITES, RECENT, ALL }
   private final ClickGuiScreen parent;
   private View view = View.FAVORITES;
   private String query = "", notice = "";
   private List<Module> rows = List.of();
   private Module selected;
   private int offset, rowCount, left, top, panelWidth, rowsY, footerY;
   private Button favorite, inspect, reveal, previous, next;
   private EditBox search;
   private boolean compact;

   public QuickAccessScreen(ClickGuiScreen parent) {
      super(Component.literal("Schizo Quick Access"));
      this.parent = parent;
   }

   private ModulePreferencesRegistry preferences() { return ClientContext.modulePreferencesRegistry; }

   @Override protected void init() {
      super.init();
      favorite = inspect = reveal = previous = next = null;
      search = null;
      var layout = StudioViewportLayouts.quickAccess(width, height);
      panelWidth = layout.width(); left = layout.left(); top = layout.top();
      compact = layout.compact(); rowCount = layout.rows();
      rowsY = layout.rowsY(); footerY = layout.footerY();
      if (compact) {
         button("Back", left, Math.max(1,height-25), panelWidth, this::onClose);
         return;
      }
      int w = (panelWidth-8)/3;
      int tab = layout.stacked() ? w : (panelWidth-16)/5;
      button("Favorites",left,top+24,tab,() -> switchView(View.FAVORITES));
      button("Recent",left+tab+4,top+24,tab,() -> switchView(View.RECENT));
      button("All modules",left+2*(tab+4),top+24,tab,() -> switchView(View.ALL));
      int historyWidth = layout.stacked() ? (panelWidth-4)/2 : tab;
      int historyX = layout.stacked() ? left : left+3*(tab+4);
      int historyY = top+(layout.stacked()?47:24);
      button("Visited",historyX,historyY,historyWidth,() -> openStudioChild(new RecentSettingsScreen(this)));
      button("Edit history",historyX+historyWidth+4,historyY,historyWidth,() -> openStudioChild(new ScalarHistoryScreen(this)));
      search = trackResizeFocus("search", addRenderableWidget(new StudioEditBox(font,left,layout.searchY(),panelWidth,20,Component.literal("Search quick access"))));
      search.setMaxLength(64);
      search.setValue(query);
      search.setHint(Component.literal("Search modules..."));
      search.setResponder(value -> {query=value; offset=0; rebuild();});
      int y = footerY;
      previous=button("Previous",left,y,w,() -> select(-1));
      next=button("Next",left+w+4,y,w,() -> select(1));
      favorite=button("Favorite",left+2*(w+4),y,w,this::toggleFavorite);
      inspect=button("Inspect",left,y+23,w,this::inspectSelected);
      reveal=button("Open module",left+w+4,y+23,w,() -> {
         Module target=selected;
         if (target==null) return;
         minecraft.setScreen(parent);
         parent.revealModule(target);
      });
      button("Back",left+2*(w+4),y+23,w,this::onClose);
      rebuild();
      setInitialFocus(search);
   }

   private Button button(String label,int x,int y,int w,Runnable action) {
      return trackResizeFocus("button:" + label, addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(label),ignored -> action.run())));
   }

   private void switchView(View nextView) {
      view=nextView; selected=null; offset=0; notice=""; rebuild();
   }

   private void rebuild() {
      Map<String,Module> modules=new LinkedHashMap<>();
      if (ClientContext.moduleRegistry != null) {
         for (Object item : ClientContext.moduleRegistry.getRegistry()) {
            if (item instanceof Module module) modules.putIfAbsent(module.getConfigName(),module);
         }
      }
      var prefs=preferences();
      List<Module> candidates=new ArrayList<>();
      if (view==View.ALL) {
         candidates.addAll(modules.values());
         candidates.sort(Comparator.comparing(Module::getName,String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Module::getConfigName));
      } else if (prefs!=null) {
         for (String name : view==View.FAVORITES ? prefs.getFavorites() : prefs.getRecentlyUsed()) {
            Module module=modules.get(name);
            if (module!=null) candidates.add(module);
         }
      }
      rows=candidates.stream().filter(module -> PaletteMatcher.matchesModule(module.getName(),
         module.getDescription()+" "+String.join(" ",module.getAliases()),query)).toList();
      if (rows.stream().noneMatch(module -> module==selected)) selected=rows.isEmpty()?null:rows.getFirst();
      refresh();
   }

   private int selectedIndex() {
      for(int i=0;i<rows.size();i++) if(rows.get(i)==selected) return i;
      return -1;
   }

   private void select(int step) {
      int index = InspectorRows.step(selectedIndex(), step, rows.size());
      if (index < 0) return;
      selected=rows.get(index);
      refresh();
   }

   private void inspectSelected() {
      if (selected != null) openStudioChild(new ModuleInspectorScreen(this, selected));
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if (!compact && (getFocused() == null || getFocused() == search)) {
         switch (event.key()) {
            case GLFW.GLFW_KEY_UP -> { select(-1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { select(1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { select(-rowCount); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { select(rowCount); return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { inspectSelected(); return true; }
         }
      }
      return super.keyPressed(event);
   }

   private void refresh() {
      if(favorite==null) return;
      int index=selectedIndex();
      offset=InspectorRows.reveal(Math.max(0,index),offset,rowCount,rows.size());
      var prefs=preferences();
      boolean valid=index>=0;
      boolean pinned=valid && prefs!=null && prefs.isFavorite(selected);
      previous.active=valid && index>0;
      next.active=valid && index+1<rows.size();
      favorite.active=valid && prefs!=null;
      favorite.setMessage(Component.literal(pinned?"Unfavorite":"Favorite"));
      inspect.active=valid;
      reveal.active=valid && selected.getCategory()!=dev.schizo.client.modules.ModuleCategory.HUD;
   }

   private void toggleFavorite() {
      var prefs=preferences();
      if(selected==null || prefs==null) return;
      if(prefs.isFavorite(selected)) {
         prefs.removeFavorite(selected);
         notice="Favorite removed.";
      } else {
         if(prefs.getFavorites().size()>=ModulePreferencesRegistry.MAX_FAVORITES) {
            notice="Favorite limit reached. Remove one first.";
            return;
         }
         prefs.addFavorite(selected);
         notice="Favorite added.";
      }
      ClientContext.configurationManager.saveAsyncSnapshot();
      rebuild();
   }

   @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
      if(!compact && event.button()==0) {
         int index=InspectorRows.hit(event.x(),event.y(),left,rowsY,panelWidth,rowCount,offset,rows.size());
         if(index>=0) {selected=rows.get(index);refresh();setFocused(null);return true;}
      }
      return super.mouseClicked(event,doubleClick);
   }

   @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
      if(!compact && vertical!=0 && x>=left && x<left+panelWidth && y>=rowsY && y<footerY) {
         offset=Math.max(0,Math.min(Math.max(0,rows.size()-rowCount),offset+(vertical<0?1:-1)));
         return true;
      }
      return super.mouseScrolled(x,y,horizontal,vertical);
   }

   private void line(GuiGraphics graphics,String text,int x,int y,int available,int color) {
      graphics.drawString(font,font.plainSubstrByWidth(text,Math.max(0,available)),x,y,color,false);
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      StudioScreenTheme theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) {
         line(graphics,"Reduce GUI scale to use Quick Access",left,top,panelWidth,0xffeeeeef);
      } else {
         graphics.fill(left-4,top,left+panelWidth+4,Math.min(height,footerY+66),theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"SCHIZO / QUICK ACCESS",left,top+6,panelWidth,theme.accent());
         line(graphics,view.name()+" | "+rows.size()+" modules",left,top+15,panelWidth,0xffb8b9c7);
         if(rows.isEmpty()) line(graphics,"No matching modules. Browse All modules to add favorites.",left,rowsY+2,panelWidth,0xffb8b9c7);
         var prefs=preferences();
         for(int i=0;i<rowCount && offset+i<rows.size();i++) {
            Module module=rows.get(offset+i);
            int y=rowsY+i*13;
            graphics.fill(left,y,left+panelWidth,y+12,module==selected?theme.selected():theme.row());
            if(module==selected) graphics.fill(left,y,left+2,y+12,theme.accent());
            String pin=prefs!=null && prefs.isFavorite(module)?"* ":"";
            line(graphics,pin+module.getName()+" | "+module.getCategory().getName(),left+4,y+3,panelWidth-70,0xffeeeeef);
            line(graphics,module.isToggled()?"Enabled":"Disabled",left+panelWidth-60,y+3,58,0xffb8b9c7);
         }
         line(graphics,notice.isEmpty()?"Recent = most recently enabled. Opening does not toggle.":notice,left,footerY+47,panelWidth,0xffb8b9c7);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() {returnToParent(parent);}
   @Override public boolean isPauseScreen() {return false;}
}
