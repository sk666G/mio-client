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
import dev.schizo.client.gui.screens.PaletteMatcher;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;

import java.util.Comparator;
import java.util.List;

public class ModuleCategoryPanel extends Panel implements ClientAccess {
   public ModuleCategory category;
   private final QueryMatchCache<GuiElement> searchMatches = new QueryMatchCache<>();

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
      this.searchMatches.clear();
      this.updateLayout();
      super.init();
   }

   public boolean hasSearchMatches() {
      for (GuiElement child : this.children) {
         if (this.isChildVisible(child)) return true;
      }
      return false;
   }

   @Override
   protected boolean isChildVisible(GuiElement child) {
      String query = ClientContext.getClickGui().getNormalizedSearchQuery();
      if (query.isEmpty()) {
         this.searchMatches.clearIfPresent();
         return true;
      }
      if (child instanceof ModuleComponent moduleComponent) {
         return this.searchMatches.matches(query, child, (element, text) -> matchesModule(((ModuleComponent)element).getModule(), text));
      }
      return true;
   }

   private static boolean matchesModule(Module module, String query) {
      StringBuilder context = new StringBuilder(module.getDescription());
      for (String alias : module.getAliases()) context.append(' ').append(alias);
      for (var setting : module.getSettings()) {
         context.append(' ').append(setting.getName()).append(' ').append(setting.getConfigName());
      }
      return PaletteMatcher.matchesModule(module.getName(), context.toString(), query);
   }
}
