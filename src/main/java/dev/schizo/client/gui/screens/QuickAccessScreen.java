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
import net.minecraft.network.chat.Component;

/** Local navigation and preference management; opening a module never toggles it. */
public final class QuickAccessScreen extends StudioScreen {
   private enum View { FAVORITES, RECENT, ALL }
   private final ClickGuiScreen parent;
   private View view = View.FAVORITES;
   private String query = "", notice = "";
   private List<Module> rows = List.of();
   private Module selected;
   private int offset, rowCount, left, top, panelWidth;
   private Button favorite, inspect, reveal, previous, next;
   private boolean compact;

   public QuickAccessScreen(ClickGuiScreen parent) {
      super(Component.literal("Schizo Quick Access"));
      this.parent = parent;
   }

   private ModulePreferencesRegistry preferences() { return ClientContext.modulePreferencesRegistry; }

   @Override protected void init() {
      super.init();
      favorite = null;
      panelWidth = Math.max(1, Math.min(490, width - 16));
      left = (width - panelWidth) / 2;
      compact = panelWidth < 240 || height < 180;
      top = Math.max(4, (height - 268) / 2);
      if (compact) {
         button("Back", left, Math.max(1,height-25), panelWidth, this::onClose);
         return;
      }
      rowCount = Math.max(1, Math.min(10, (height - top - 150) / 13));
      int w = (panelWidth-8)/3;
      button("Favorites",left,top+24,w,() -> switchView(View.FAVORITES));
      button("Recent",left+w+4,top+24,w,() -> switchView(View.RECENT));
      button("All modules",left+2*(w+4),top+24,w,() -> switchView(View.ALL));
      EditBox search = addRenderableWidget(new StudioEditBox(font,left,top+48,panelWidth,20,Component.literal("Search quick access")));
      search.setMaxLength(64);
      search.setValue(query);
      search.setHint(Component.literal("Search modules..."));
      search.setResponder(value -> {query=value; offset=0; rebuild();});
      int y = top+75+rowCount*13;
      previous=button("Previous",left,y,w,() -> select(-1));
      next=button("Next",left+w+4,y,w,() -> select(1));
      favorite=button("Favorite",left+2*(w+4),y,w,this::toggleFavorite);
      inspect=button("Inspect",left,y+23,w,() -> {
         if (selected != null) minecraft.setScreen(new ModuleInspectorScreen(this,selected));
      });
      reveal=button("Open module",left+w+4,y+23,w,() -> {
         Module target=selected;
         if (target==null) return;
         minecraft.setScreen(parent);
         parent.revealModule(target);
      });
      button("Back",left+2*(w+4),y+23,w,this::onClose);
      rebuild();
   }

   private Button button(String label,int x,int y,int w,Runnable action) {
      return addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(label),ignored -> action.run()));
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
      if(rows.isEmpty()) return;
      selected=rows.get(Math.max(0,Math.min(rows.size()-1,selectedIndex()+step)));
      refresh();
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
         int index=InspectorRows.hit(event.x(),event.y(),left,top+75,panelWidth,rowCount,offset,rows.size());
         if(index>=0) {selected=rows.get(index);refresh();setFocused(null);return true;}
      }
      return super.mouseClicked(event,doubleClick);
   }

   @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
      if(!compact && vertical!=0 && x>=left && x<left+panelWidth && y>=top+75 && y<top+75+rowCount*13) {
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
         graphics.fill(left-4,top,left+panelWidth+4,Math.min(height,top+141+rowCount*13),theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"SCHIZO / QUICK ACCESS",left,top+6,panelWidth,theme.accent());
         line(graphics,view.name()+" | "+rows.size()+" modules",left,top+15,panelWidth,0xffb8b9c7);
         if(rows.isEmpty()) line(graphics,"No matching modules. Browse All modules to add favorites.",left,top+77,panelWidth,0xffb8b9c7);
         var prefs=preferences();
         for(int i=0;i<rowCount && offset+i<rows.size();i++) {
            Module module=rows.get(offset+i);
            int y=top+75+i*13;
            graphics.fill(left,y,left+panelWidth,y+12,module==selected?theme.selected():theme.row());
            if(module==selected) graphics.fill(left,y,left+2,y+12,theme.accent());
            String pin=prefs!=null && prefs.isFavorite(module)?"* ":"";
            line(graphics,pin+module.getName()+" | "+module.getCategory().getName(),left+4,y+3,panelWidth-70,0xffeeeeef);
            line(graphics,module.isToggled()?"Enabled":"Disabled",left+panelWidth-60,y+3,58,0xffb8b9c7);
         }
         line(graphics,notice.isEmpty()?"Recent = most recently enabled. Opening does not toggle.":notice,left,top+122+rowCount*13,panelWidth,0xffb8b9c7);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() {minecraft.setScreen(parent);}
   @Override public boolean isPauseScreen() {return false;}
}
