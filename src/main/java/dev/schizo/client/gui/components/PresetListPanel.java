/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.screens.PresetScreen;
import dev.schizo.client.gui.components.ActionButtonComponent;
import dev.schizo.client.config.PresetData;
import dev.schizo.client.gui.GuiElement;
import dev.schizo.client.settings.options.PresetCategory;

import com.mojang.blaze3d.vertex.PoseStack;

public class PresetListPanel extends Panel {
   private final PresetScreen screen;
   private boolean layoutUpdatePending;

   public PresetListPanel(PresetScreen screen) {
      super("List");
      this.screen = screen;
   }

   @Override
   public void render(PoseStack poses, double mouseX, double mouseY) {
      if (this.layoutUpdatePending) {
         for (GuiElement child : this.children) {
            child.update(mouseX, mouseY);
         }

         this.updateLayout();
         this.layoutUpdatePending = false;
      }

      super.render(poses, mouseX, mouseY);
   }

   public void refreshPresets() {
      this.layoutUpdatePending = true;
      this.cancelInput();
      this.children.clear();
      var category=screen.getSelectedCategory();
      ClientContext.configurationManager
         .getPresetRegistry(
            this.screen
               .getSelectedCategory()
         )
         .forEachPreset(
            (java.util.function.Consumer<dev.schizo.client.config.PresetData>)(preset -> this.addChild(
               new PresetListPanel.PresetButton(
                  this.screen, this, preset, category
               )
            ))
         );
   }

   private static final class PresetButton extends ActionButtonComponent {
      private final PresetScreen screen;
      private final PresetData preset;
      private final PresetCategory category;

      public PresetButton(PresetScreen screen, Panel panel, PresetData preset,PresetCategory category) {
         super(
            panel,
            preset.getName(),
            () -> {
               if(screen.getSelectedCategory()!=category) return;
               screen.getPresetPanel()
                  .editPreset(
                     preset
                  );
               screen.getPresetPanel()
                  .updateLayout();
            }
         );
         this.preset = preset;
         this.category=category;
         this.screen = screen;
      }

      @Override
      public boolean isHighlighted() {
         return screen.getSelectedCategory()==category && this.preset
            .equals(
               this.screen
                  .getPresetPanel()
                  .getSelectedPreset()
            );
      }

   }
}
