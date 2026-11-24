/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.panels;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.NamedFeature;
import dev.schizo.client.gui.GuiElement;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.gui.components.ModuleComponent;
import dev.schizo.client.render.TextUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;

import java.util.Comparator;
import java.util.List;

public class ModuleCategoryPanel extends Panel implements ClientAccess {
   public ModuleCategory category;

   private void populateModules() {
      List<Module> modules = (List<Module>)ClientContext.moduleRegistry.getRegistry();
      modules.stream()
         .filter(module -> module.getCategory() == this.category)
         .sorted(Comparator.comparing(NamedFeature::getName))
         .forEach(module -> {
            ModuleComponent component = new ModuleComponent(module, this, 0);
            this.children.add(component);
         });
   }

   public ModuleCategoryPanel(ModuleCategory category) {
      super(FontRenderer.capitalize(category.getName()));
      this.category = category;
      this.populateModules();
   }

   @Override
   public void init() {
      this.updateLayout();
      super.init();
   }

   @Override
   protected boolean isChildVisible(GuiElement child) {
      String query = ClientContext.getClickGui().getNormalizedSearchQuery();
      if (child instanceof ModuleComponent moduleComponent && !query.isEmpty()) {
         for (String alias : moduleComponent.getModule()
            .getAliases()) {
            if (TextUtils.containsIgnoreCase(alias, query)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

}
