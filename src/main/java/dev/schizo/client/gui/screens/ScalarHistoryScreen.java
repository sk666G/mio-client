package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.SettingEditHistory;
import dev.schizo.client.integrations.ModuleCompatibility;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-thread, process-local inspector scalar history with explicit cross-module replay. */
final class ScalarHistoryScreen extends StudioScreen {
   private static final SettingEditHistory HISTORY = new SettingEditHistory(ScalarHistoryScreen::allowed);
   private final Screen parent;
   private Button undo,redo,clear;
   private int left,top,panelWidth;
   private boolean compact,confirming;
   private String notice="Only reviewed scalar inspector edits are recorded.";

   ScalarHistoryScreen(Screen parent) {
      super(Component.literal("Edit history"));
      this.parent=parent;
   }

   static SettingEditHistory history() {return HISTORY;}

   private static Module owner(Setting<?> setting) {
      if(setting==null || ClientContext.moduleRegistry==null) return null;
      for(Object entry:ClientContext.moduleRegistry.getRegistry()) {
         if(entry instanceof Module module && module.getSettings().stream().anyMatch(item -> item==setting)) return module;
      }
      return null;
   }

   private static boolean allowed(Setting<?> setting) {
      Module module=owner(setting);
      return module!=null && !ModuleCompatibility.assess(module.getClass()).requiresMultiplayerWarning();
   }

   private static String label(Setting<?> setting) {
      if(setting==null) return "None";
      Module module=owner(setting);
      return module==null ? "Unavailable setting" : module.getName()+" / "+setting.getName();
   }

   @Override protected void init() {
      super.init();
      undo=null; confirming=false;
      panelWidth=Math.max(1,Math.min(440,width-16));
      left=(width-panelWidth)/2;
      top=Math.max(4,(height-142)/2);
      compact=panelWidth<220 || height<150;
      if(compact) {button("Back",left,Math.max(1,height-25),panelWidth,this::onClose);return;}
      int half=(panelWidth-4)/2;
      undo=button("Undo edit",left,top+40,half,() -> replay(true));
      redo=button("Redo edit",left+half+4,top+40,half,() -> replay(false));
      clear=button("Clear history",left,top+93,half,() -> {
         if(confirming) {HISTORY.clear();confirming=false;notice="History cleared. Values unchanged.";}
         else {confirming=true;notice="Confirm clearing all recorded scalar edits.";}
         refresh();
      });
      button("Back",left+half+4,top+93,half,this::onClose);
      refresh();
   }

   private Button button(String label,int x,int y,int w,Runnable action) {
      return addRenderableWidget(new StudioButton(x,y,w,20,Component.literal(label),ignored -> action.run()));
   }

   private void replay(boolean backward) {
      confirming=false;
      notice=(backward?"Undo: ":"Redo: ")+(backward?HISTORY.undo():HISTORY.redo());
      refresh();
   }

   private void refresh() {
      if(undo==null)return;
      undo.active=HISTORY.canUndo();
      redo.active=HISTORY.canRedo();
      clear.active=undo.active||redo.active;
      clear.setMessage(Component.literal(confirming?"Confirm clear":"Clear history"));
   }

   private void line(GuiGraphics graphics,String text,int y,int color) {
      graphics.drawString(font,font.plainSubstrByWidth(text,Math.max(0,panelWidth)),left,y,color,false);
   }

   @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
      StudioScreenTheme theme=StudioScreenTheme.current();
      graphics.fill(0,0,width,height,theme.backdrop());
      if(compact) line(graphics,"Reduce GUI scale to use edit history",top,0xffeeeeef);
      else {
         graphics.fill(left-4,top,left+panelWidth+4,top+138,theme.panel());
         graphics.fill(left-4,top,left+panelWidth+4,top+1,theme.accent());
         line(graphics,"SCHIZO / EDIT HISTORY",top+6,theme.accent());
         line(graphics,"Undo target: "+label(HISTORY.undoTarget()),top+17,0xffeeeeef);
         line(graphics,"Redo target: "+label(HISTORY.redoTarget()),top+28,0xffeeeeef);
         line(graphics,notice,top+66,0xffb8b9c7);
         line(graphics,"Session only | Theme and HUD history remain separate.",top+78,0xffb8b9c7);
         line(graphics,"Stale, hidden or unsupported edits are refused.",top+120,0xffb8b9c7);
      }
      super.render(graphics,mouseX,mouseY,partialTick);
   }

   @Override public void onClose(){returnToParent(parent);}
   @Override public boolean isPauseScreen(){return false;}
}
