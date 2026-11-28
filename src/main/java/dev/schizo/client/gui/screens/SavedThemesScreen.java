package dev.schizo.client.gui.screens;

import dev.schizo.client.config.ThemeLibrary;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.modules.client.UiPalette;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local named palette library. Loading a theme changes only the parent preview. */
final class SavedThemesScreen extends StudioScreen {
   private final Screen parent;
   private final UiPalette current;
   private final Consumer<UiPalette> accepted;
   private final ThemeLibrary library = new ThemeLibrary(ConfigurationManager.ROOT_DIRECTORY);
   private List<ThemeLibrary.Entry> entries = List.of();
   private boolean readable, confirming;
   private int selected, left, top, panelWidth;
   private boolean compact;
   private StudioViewportLayouts.SavedThemes layout;
   private EditBox name;
   private Button load, duplicate, rename, delete, save, previous, next;
   private String notice = "";
   private String nameDraft = "";

   SavedThemesScreen(Screen parent, UiPalette current, Consumer<UiPalette> accepted) {
      super(Component.literal("Saved themes"));
      this.parent = parent;
      this.current = current;
      this.accepted = accepted;
      reload();
   }

   private void reload() {
      confirming = false;
      try { entries = library.load(); readable = true; selected = Math.max(0,Math.min(selected,entries.size()-1)); notice = "Saved themes are local."; }
      catch (IOException failure) { readable = false; entries = List.of(); selected = 0; notice = "Library unreadable. Reload before editing."; }
      refresh();
   }

   @Override protected void init() {
      super.init();
      layout = StudioViewportLayouts.savedThemes(width,height);
      panelWidth=layout.width(); left=layout.left(); top=layout.top(); compact=layout.compact();
      name=null; load=duplicate=rename=delete=save=previous=next=null;
      confirming=false;
      if (compact) { button("Back",left,Math.max(1,height-25),panelWidth,this::onClose); return; }
      int w = (panelWidth-8)/3;
      previous = button("Previous",left,layout.navigationY(),w,() -> select(-1));
      next = button("Next",left+w+4,layout.navigationY(),w,() -> select(1));
      load = button("Preview theme",left+2*(w+4),layout.navigationY(),w,() -> {
         if (readable && !entries.isEmpty()) { accepted.accept(entries.get(selected).colors()); onClose(); }
      });
      name = trackResizeFocus("name",addRenderableWidget(new StudioEditBox(font,left,layout.nameY(),panelWidth,20,Component.literal("Theme name"))));
      name.setHint(Component.literal("New theme name"));
      name.setMaxLength(64);
      name.setValue(nameDraft);
      name.setResponder(value -> { nameDraft=value; confirming=false; refresh(); });
      save = button("Save preview",left,layout.actionsY(),w,() -> mutate(0)).help("Save the current detached preview under the entered name.");
      duplicate = button("Duplicate",left+w+4,layout.actionsY(),w,() -> mutate(1)).help("Copy the selected saved theme under a new name.");
      rename = button("Rename",left+2*(w+4),layout.actionsY(),w,() -> mutate(2)).help("Rename the selected saved theme without changing its colors.");
      delete = button("Delete",left,layout.secondaryY(),w,() -> {
         if (!confirming) { confirming=true; refresh(); }
         else mutate(3);
      });
      button("Reload",left+w+4,layout.secondaryY(),w,this::reload);
      button("Back",left+2*(w+4),layout.secondaryY(),w,this::onClose);
      refresh();
   }

   private StudioButton button(String title,int x,int y,int w,Runnable action) {
      return trackResizeFocus("button:"+title,addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(title),ignored -> action.run())));
   }

   private void select(int offset) {
      selected = Math.max(0,Math.min(entries.size()-1,selected+offset));
      confirming=false;
      refresh();
   }

   private void refresh() {
      if (save == null) return;
      boolean has = readable && !entries.isEmpty(), named = !nameDraft.isBlank();
      save.active = readable && named;
      duplicate.active = has && named;
      rename.active = has && named;
      delete.active = has;
      load.active = has;
      previous.active = has && selected>0;
      next.active = has && selected+1<entries.size();
      delete.setMessage(Component.literal(confirming ? "Confirm delete" : "Delete"));
   }

   private void mutate(int operation) {
      if (!readable) return;
      List<ThemeLibrary.Entry> updated = new ArrayList<>(entries);
      try {
         switch(operation) {
            case 0 -> updated.add(new ThemeLibrary.Entry(nameDraft,current));
            case 1 -> updated.add(new ThemeLibrary.Entry(nameDraft,entries.get(selected).colors()));
            case 2 -> updated.set(selected,new ThemeLibrary.Entry(nameDraft,entries.get(selected).colors()));
            case 3 -> updated.remove(selected);
            default -> { return; }
         }
         library.replace(entries,updated);
         entries=List.copyOf(updated);
         selected = operation<=1 ? entries.size()-1 : Math.max(0,Math.min(selected,entries.size()-1));
         notice = "Theme library saved. Live colors unchanged.";
      } catch (IllegalArgumentException invalid) { notice="Invalid or duplicate name, or theme limit reached."; }
      catch (IOException failure) { notice="Theme write rejected or failed. Reload before editing."; readable=false; }
      confirming=false;
      refresh();
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      StudioScreenTheme theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if (compact) {
         graphics.drawString(font,font.plainSubstrByWidth("Reduce GUI scale to manage themes",panelWidth),left,top,0xffeeeeef,false);
      } else {
         graphics.fill(left-4,top,left+panelWidth+4,layout.panelBottom(),theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         graphics.drawString(font,"SAVED THEMES",left,top+5,theme.accent(),false);
         String item=entries.isEmpty()?"No saved themes":(selected+1)+"/"+entries.size()+"  "+entries.get(selected).name();
         graphics.drawString(font,font.plainSubstrByWidth(item,panelWidth),left,top+17,0xffeeeeef,false);
         if (!layout.condensed()) graphics.drawString(font,font.plainSubstrByWidth("New name (save, duplicate or rename)",panelWidth),left,top+54,0xffb8b9c7,false);
         graphics.drawString(font,font.plainSubstrByWidth(confirming?"Confirm deletion of selected theme.":notice,panelWidth),left,layout.noticeY(),0xffb8b9c7,false);
         graphics.drawString(font,font.plainSubstrByWidth("Preview does not apply live settings.",panelWidth),left,layout.noteY(),0xffb8b9c7,false);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose() { returnToParent(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
