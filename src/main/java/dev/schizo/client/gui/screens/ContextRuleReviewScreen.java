package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.config.ContextRulePreview;
import dev.schizo.client.config.JsonChangePreview;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only simulation; saving registers the rule for the normal lifecycle, not direct preset application. */
final class ContextRuleReviewScreen extends StudioScreen {
   private final ContextProfileEditorScreen parent;
   private final ContextRulePreview.Result preview;
   private List<String> rows = List.of();
   private int offset, left, panelWidth, top, rowCount;
   private String notice = "";

   ContextRuleReviewScreen(ContextProfileEditorScreen parent, ContextRulePreview.Result preview) {
      super(Component.literal("Review context rule"));
      this.parent = parent;
      this.preview = preview;
   }

   @Override protected void init() {
      super.init();
      panelWidth = Math.max(1, Math.min(560, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(6, (height - 320) / 2);
      rowCount = Math.max(1, Math.min(17, (height - top - 72) / 13));
      int y = top + 34 + rowCount * 13 + 5;
      int half = Math.max(1, (panelWidth - 4) / 2);
      addRenderableWidget(new StudioButton(left, y, half, 20, Component.literal("Cancel / edit"), button -> onClose()));
      addRenderableWidget(new StudioButton(left + half + 4, y, half, 20, Component.literal(preview.replaces() ? "Confirm replacement" : "Confirm new rule"), button -> commit()));
      rows = buildRows();
      offset = Math.min(offset, Math.max(0, rows.size() - rowCount));
   }

   private List<String> buildRows() {
      List<String> result = new ArrayList<>();
      result.add(preview.replaces() ? "Replaces existing rule with identical selectors/category." : "Adds a rule; other rule keys remain unchanged.");
      result.add("Example: " + preview.server() + " / " + preview.dimension() + " / " + preview.gameMode());
      result.add("Wildcard example only; other contexts can select different rules.");
      result.add("Before: " + preview.before().size() + " selected categories.");
      for (var rule : preview.before()) result.add("  " + rule.category().getName() + " -> " + rule.presetName());
      result.add("After: " + preview.after().size() + " selected categories, in application order:");
      for (var rule : preview.after()) {
         var registry = ClientContext.configurationManager.getPresetRegistry(rule.category());
         var preset = registry == null ? null : registry.getPreset(rule.presetName());
         result.add("  " + rule.category().getName() + " -> " + rule.presetName() + (preset == null ? " [MISSING]" : ""));
      }
      if (!preview.after().contains(preview.proposed())) result.add("Warning: proposed rule is shadowed in this example.");
      if (preview.shadowed() > 0) result.add(preview.shadowed() + " matching rule(s) lose to category precedence.");
      if (preview.allOverlap()) result.add("Warning: All and category presets may write overlapping settings.");
      var rule = preview.proposed();
      try {
         var registry = ClientContext.configurationManager.getPresetRegistry(rule.category());
         var preset = registry == null ? null : registry.getPreset(rule.presetName());
         if (preset == null) result.add("Proposed preset is missing.");
         else {
            var diff = JsonChangePreview.compare(rule.category().toJson(), preset.copyContents());
            result.add("Proposed preset vs live settings: " + diff.paths().size() + (diff.truncated() ? "+" : "") + " differing JSON paths.");
            result.add("These are not the restored baseline or the final combined settings.");
            for (String field : diff.paths()) result.add("  " + field);
         }
      } catch (RuntimeException invalid) {
         result.add("Setting difference unavailable; rule ordering is still shown.");
      }
      return List.copyOf(result);
   }

   private void commit() {
      var rule = preview.proposed();
      var registry = ClientContext.configurationManager.getPresetRegistry(rule.category());
      if (registry == null || registry.getPreset(rule.presetName()) == null) {
         notice = "Preset no longer exists. Cancel and edit.";
         return;
      }
      if (!ClientContext.contextProfileRegistry.savePreviewedRule(preview.original(), rule)) {
         notice = "Rules changed since preview. Cancel and preview again.";
         return;
      }
      parent.showNotice("Rule registered. Normal profile refresh/persistence will follow.");
      minecraft.setScreen(parent);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 62 + rowCount * 13, theme.panel());
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 2, theme.accent());
      draw(graphics, "SCHIZO / REVIEW RULE", top + 8, theme.accent());
      draw(graphics, notice.isEmpty() ? "No settings applied | Scroll for full preview" : notice, top + 21, 0xffa1a3af);
      for (int i = 0; i < rowCount && offset + i < rows.size(); i++) {
         int y = top + 34 + i * 13;
         graphics.fill(left, y, left + panelWidth, y + 12, theme.row());
         draw(graphics, rows.get(offset + i), y + 2, 0xffeeeeef);
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void draw(GuiGraphics graphics, String text, int y, int color) {
      graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(0, panelWidth - 8)), left + 4, y, color, false);
   }

   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (vertical != 0) {
         offset = Math.max(0, Math.min(Math.max(0, rows.size() - rowCount), offset + (vertical < 0 ? 1 : -1)));
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
