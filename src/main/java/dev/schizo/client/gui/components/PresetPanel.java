/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.config.PresetData;
import dev.schizo.client.config.PresetUndoAction;
import dev.schizo.client.gui.CloseState;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.screens.PresetScreen;
import dev.schizo.client.managers.PresetRegistry;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.StringSetting;
import dev.schizo.client.settings.options.PresetCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.Util;
import org.apache.commons.lang3.SystemUtils;

public class PresetPanel extends Panel {
   private final StringSetting saveNameSetting=new StringSetting("Name","");
   private final StringSetting destination=new StringSetting("New name","");
   private final EnumSetting<PresetCategory> categorySetting=new EnumSetting<>("Category",PresetCategory.MODULES);
   private final PresetScreen screen;
   private PresetPanelMode mode=PresetPanelMode.NEW;
   private PresetData selectedPreset;
   private PresetCategory selectedPresetCategory;
   private boolean listRefreshPending,controlsRebuildPending;
   private String status="Choose a preset or create one.";
   private final PresetConfirmation confirmation=new PresetConfirmation();

   public PresetPanel(PresetScreen screen) {
      super("Preset");
      this.screen=screen;
      destination.setChangeCallback(confirmation::clear);
      categorySetting.setChangeCallback(()->{
         screen.setSelectedCategory(categorySetting.getValue());
         confirmation.clear();
         requestListRefresh();
      });
   }

   @Override public void init() {
      rebuildControls();
   }

   @Override public void render(PoseStack poses,double mouseX,double mouseY) {
      if(controlsRebuildPending) {
         controlsRebuildPending=false;
         rebuildControls();
         updateLayout();
      }
      if(listRefreshPending) {
         listRefreshPending=false;
         screen.getListPanel().refreshPresets();
      }
      super.render(poses,mouseX,mouseY);
   }

   public void rebuildControls() {
      cancelInput();
      children.clear();
      if(mode==PresetPanelMode.EDIT && selectedPreset==null) mode=PresetPanelMode.NEW;
      if(mode==PresetPanelMode.NEW) {
         addChild(new EnumSettingComponent<>(this,CloseState.ALWAYS_OPEN,categorySetting));
         addChild(new StringSettingComponent(this,CloseState.ALWAYS_OPEN,saveNameSetting));
         action("Create new",this::createPreset);
         action("Refresh",this::refresh);
         action("Undo",this::undo);
      } else {
         addChild(new ButtonComponent(this,"Preset "+selectedPreset.getName()));
         action("Load",()->perform("Load"));
         addChild(new ActionButtonComponent(this,()->confirmation.armed("Save")?"Confirm overwrite":"Save changes",()->perform("Save")));
         addChild(new ActionButtonComponent(this,()->confirmation.armed("Delete")?"Confirm delete":"Delete",()->perform("Delete")));
         addChild(new StringSettingComponent(this,CloseState.ALWAYS_OPEN,destination));
         action("Rename",()->perform("Rename"));
         action("Duplicate",()->perform("Duplicate"));
         action("Open",this::openPresetLocation);
         action("Back",()->{status="Choose a preset or create one.";setMode(PresetPanelMode.NEW);});
      }
      addChild(new ButtonComponent(this,()->StudioControlStyle.fitText(status,Math.max(0,getWidth()-8))));
      requestListRefresh();
   }

   private void action(String label,Runnable action) {
      addChild(new ActionButtonComponent(this,label,action));
   }

   private void createPreset() {
      if(mode!=PresetPanelMode.NEW || controlsRebuildPending) return;
      String name=saveNameSetting.getValue();
      if(name.isBlank()) {status="Enter a name first.";return;}
      var registry=getSelectedCategoryPresets();
      try {
         var undo=PresetUndoAction.beforeSave(registry,name);
         registry.createPreset(name);
         ClientContext.configurationManager.getPresetUndoState().push(undo);
         saveNameSetting.setValue("");
         status="Preset created.";
         requestListRefresh();
      } catch(IOException | RuntimeException failure) {
         fail("Create",failure);
      }
   }

   private void refresh() {
      if(mode!=PresetPanelMode.NEW || controlsRebuildPending) return;
      confirmation.clear();
      getSelectedCategoryPresets().loadPresets();
      requestListRefresh();
      status="List refreshed from registry.";
   }

   private void undo() {
      if(mode!=PresetPanelMode.NEW || controlsRebuildPending) return;
      try {
         status=ClientContext.configurationManager.getPresetUndoState().undo()==null?"Nothing to undo.":"Preset operation undone.";
         requestListRefresh();
      } catch(IOException | RuntimeException failure) {fail("Undo",failure);}
   }

   private void perform(String action) {
      if(mode!=PresetPanelMode.EDIT || controlsRebuildPending || selectedPreset==null) return;
      var registry=getSelectedCategoryPresets();
      PresetData expected=selectedPreset;
      try {
         if(selectedPresetCategory!=screen.getSelectedCategory() || !registry.matchesReviewedPreset(expected)) {
            stale();
            return;
         }
         if(action.equals("Save") || action.equals("Delete")) {
            if(!confirmation.confirm(action,selectedPresetCategory,expected)) {
               status=action.equals("Save")?"Click Confirm overwrite.":"Click Confirm delete.";
               return;
            }
         } else confirmation.clear();
         String name=expected.getName();
         PresetUndoAction undo;
         switch(action) {
            case "Load" -> {
               undo=PresetUndoAction.beforeLoad(selectedPresetCategory,name);
               if(!registry.applyPreset(name)) {status="Load rejected; inspect log.";return;}
            }
            case "Save" -> {
               undo=PresetUndoAction.beforeSave(registry,name);
               registry.savePreset(name);
            }
            case "Delete" -> {
               undo=PresetUndoAction.beforeDelete(registry,name);
               if(!registry.deletePreset(name)) {status="Registry changed; refresh.";requestListRefresh();return;}
            }
            case "Rename" -> {
               String target=destination.getValue();
               undo=PresetUndoAction.beforeRename(registry,name,target);
               if(!registry.renamePreset(name,target)) {status="Choose a different name.";return;}
            }
            case "Duplicate" -> {
               String target=destination.getValue();
               undo=PresetUndoAction.beforeSave(registry,target);
               if(!registry.copyPreset(name,target)) {status="Source unavailable.";return;}
            }
            default -> throw new IllegalArgumentException("Unknown preset action");
         }
         ClientContext.configurationManager.getPresetUndoState().push(undo);
         status=switch(action) {
            case "Load" -> "Preset loaded.";
            case "Save" -> "Preset saved.";
            case "Delete" -> "Preset deleted.";
            case "Rename" -> "Preset renamed.";
            default -> "Preset duplicated.";
         };
         setMode(PresetPanelMode.NEW);
      } catch(IOException | RuntimeException failure) {fail(action,failure);}
   }

   private void stale() {
      status="Preset changed; refresh and select again.";
      setMode(PresetPanelMode.NEW);
      getSelectedCategoryPresets().loadPresets();
   }

   private void fail(String action,Exception failure) {
      confirmation.clear();
      ClientAccess.logger.warn("Preset action failed: {}",action,failure);
      status=action+" failed; inspect log.";
      requestListRefresh();
   }

   private void requestListRefresh() {listRefreshPending=true;}

   private PresetRegistry getSelectedCategoryPresets() {
      return ClientContext.configurationManager.getPresetRegistry(screen.getSelectedCategory());
   }

   public void editPreset(PresetData preset) {
      if(preset==null || !preset.equals(getSelectedCategoryPresets().getPreset(preset.getName()))) {
         stale();
         return;
      }
      selectedPreset=preset;
      selectedPresetCategory=screen.getSelectedCategory();
      destination.setValue("");
      status="Select an action.";
      setMode(PresetPanelMode.EDIT);
   }

   public PresetData getSelectedPreset() {return mode==PresetPanelMode.EDIT?selectedPreset:null;}

   public void setMode(PresetPanelMode mode) {
      cancelInput();
      this.mode=mode;
      if(mode==PresetPanelMode.NEW) {
         selectedPreset=null;
         selectedPresetCategory=null;
      }
      controlsRebuildPending=true;
      requestListRefresh();
   }

   @Override public void cancelInput() {
      confirmation.clear();
      super.cancelInput();
   }

   private void openPresetLocation() {
      if(mode!=PresetPanelMode.EDIT || controlsRebuildPending || selectedPreset==null) return;
      confirmation.clear();
      try {
         if(!getSelectedCategoryPresets().matchesReviewedPreset(selectedPreset)) {stale();return;}
         Path directory=screen.getSelectedCategory().getDirectory();
         if(!SystemUtils.IS_OS_WINDOWS_10 && !SystemUtils.OS_NAME.startsWith("Windows 11"))
            Util.getPlatform().openPath(directory);
         else new ProcessBuilder("explorer.exe","/select,",directory.toAbsolutePath().resolve(selectedPreset.getName()+".json").toString()).start();
      } catch(IOException | RuntimeException failure) {fail("Open",failure);}
   }
}
