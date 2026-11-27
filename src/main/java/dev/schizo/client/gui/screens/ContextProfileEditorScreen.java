package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.config.ContextRulePreview;
import dev.schizo.client.managers.ContextProfileRegistry.Rule;
import dev.schizo.client.settings.options.PresetCategory;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Edits one rule at a time; explicit review before a registry mutation. */
public final class ContextProfileEditorScreen extends Screen {
   private final Screen parent;
   private EditBox server, dimension, gameMode, preset;
   private PresetCategory category = PresetCategory.COLORS;
   private Button categoryButton;
   private int index = -1, left, panelWidth, top;
   private Rule selected;
   private List<Rule> selectedSnapshot = List.of();
   private String notice = "Select an existing rule or enter a new one.";
   private String serverText = "*", dimensionText = "any", modeText = "any", presetText = "";

   public ContextProfileEditorScreen(Screen parent) {
      super(Component.literal("Schizo Context Profiles"));
      this.parent = parent;
   }

   @Override protected void init() {
      panelWidth = Math.max(1, Math.min(520, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(4, (height - 216) / 2);
      int half = Math.max(1, (panelWidth - 4) / 2);
      server = field("Server (* = any)", serverText, left, top + 41, half, 255, v -> serverText = v);
      dimension = field("Dimension", dimensionText, left + half + 4, top + 41, half, 16, v -> dimensionText = v);
      gameMode = field("Game mode", modeText, left, top + 72, half, 16, v -> modeText = v);
      preset = field("Preset name", presetText, left + half + 4, top + 72, half, 64, v -> presetText = v);
      categoryButton = button("Category: " + category.getName(), left, top + 94, half, () -> {
         category = PresetCategory.values()[(category.ordinal() + 1) % PresetCategory.values().length];
         categoryButton.setMessage(Component.literal("Category: " + category.getName()));
      });
      button("Next saved preset", left + half + 4, top + 94, half, this::nextPreset);
      int third = Math.max(1, (panelWidth - 8) / 3);
      button("Previous rule", left, top + 118, third, () -> browse(-1));
      button("Next rule", left + third + 4, top + 118, third, () -> browse(1));
      button("New rule", left + 2 * (third + 4), top + 118, third, this::newRule);
      button("Preview save", left, top + 142, half, this::preview);
      button("Delete selected", left + half + 4, top + 142, half, this::deleteSelected);
      button("Back", left, top + 166, half, this::onClose);
      button("Active preview", left + half + 4, top + 166, half, () -> minecraft.setScreen(new ExpeditionScreen(this, 2)));
   }

   private EditBox field(String label, String value, int x, int y, int w, int maximum,
                         java.util.function.Consumer<String> responder) {
      EditBox box = new EditBox(font, x, y, w, 18, Component.literal(label));
      box.setMaxLength(maximum);
      box.setValue(value);
      box.setResponder(responder);
      return addRenderableWidget(box);
   }

   private Button button(String text, int x, int y, int w, Runnable action) {
      return addRenderableWidget(Button.builder(Component.literal(text), ignored -> action.run()).bounds(x, y, w, 20).build());
   }

   private void browse(int direction) {
      List<Rule> rules = ClientContext.contextProfileRegistry.getRules();
      if (rules.isEmpty()) { newRule(); notice = "No saved rules."; return; }
      int current = selected == null ? -1 : rules.indexOf(selected);
      index = current < 0 ? (direction > 0 ? 0 : rules.size() - 1) : Math.floorMod(current + direction, rules.size());
      selected = rules.get(index);
      selectedSnapshot = rules;
      server.setValue(selected.server());
      dimension.setValue(selected.dimension());
      gameMode.setValue(selected.gameMode());
      preset.setValue(selected.presetName());
      category = selected.category();
      categoryButton.setMessage(Component.literal("Category: " + category.getName()));
      notice = "Rule " + (index + 1) + " / " + rules.size() + ". Save previews the entered key.";
   }

   private void newRule() {
      selected = null;
      selectedSnapshot = List.of();
      index = -1;
      server.setValue("*");
      dimension.setValue("any");
      gameMode.setValue("any");
      preset.setValue("");
      notice = "New rule. Use * for any server, any for dimension/mode.";
   }

   private void nextPreset() {
      var registry = ClientContext.configurationManager.getPresetRegistry(category);
      List<String> names = registry == null ? List.of() : registry.getPresetNames();
      if (names.isEmpty()) { notice = "No saved presets in this category."; return; }
      int current = names.indexOf(preset.getValue());
      preset.setValue(names.get((current + 1) % names.size()));
   }

   private void preview() {
      try {
         Rule rule = Rule.create(serverText, dimensionText, modeText, category, presetText);
         var registry = ClientContext.configurationManager.getPresetRegistry(rule.category());
         if (registry == null || registry.getPreset(rule.presetName()) == null) {
            notice = "Choose an existing preset before saving.";
            return;
         }
         var result = ContextRulePreview.simulate(ClientContext.contextProfileRegistry.getRules(), rule);
         minecraft.setScreen(new ContextRuleReviewScreen(this, result));
      } catch (IllegalArgumentException | IllegalStateException invalid) {
         notice = invalid.getMessage() == null ? "Invalid rule." : invalid.getMessage();
      }
   }

   private void deleteSelected() {
      if (selected == null) { notice = "Select a saved rule to delete."; return; }
      Rule target = selected;
      List<Rule> expected = selectedSnapshot;
      minecraft.setScreen(new ConfirmScreen(confirmed -> {
         if (confirmed) {
            boolean removed = ClientContext.contextProfileRegistry.removePreviewedRule(expected, target);
            notice = removed ? "Rule removed. Normal profile refresh will follow." : "Rule changed; select it again.";
            selected = null;
            selectedSnapshot = List.of();
         }
         minecraft.setScreen(this);
      }, Component.literal("Delete selected context rule?"), Component.literal("This removes the saved rule, not the preset.")));
   }

   void showNotice(String message) { notice = message; }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      graphics.fill(left - 4, top, left + panelWidth + 4, Math.min(height, top + 215), theme.panel());
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 2, theme.accent());
      draw(graphics, "SCHIZO / CONTEXT RULE EDITOR", left, top + 7, panelWidth, theme.accent());
      var registry = ClientContext.contextProfileRegistry;
      draw(graphics, "Profiles " + (registry.isEnabled() ? "enabled" : "disabled") + " | " + registry.getRules().size() + " rules"
         + (registry.getFailedContextDescription() != null ? " | recovery pending" : ""), left, top + 20, panelWidth, 0xffa1a3af);
      int half = Math.max(1, (panelWidth - 4) / 2);
      draw(graphics, "Server (* = any)", left, top + 32, half, 0xffb8b9c7);
      draw(graphics, "Dimension", left + half + 4, top + 32, half, 0xffb8b9c7);
      draw(graphics, "Game mode", left, top + 63, half, 0xffb8b9c7);
      draw(graphics, "Preset name", left + half + 4, top + 63, half, 0xffb8b9c7);
      draw(graphics, notice, left, top + 190, panelWidth, 0xffa1a3af);
      draw(graphics, "Saving a changed selector adds a new rule; delete the old one separately.", left, top + 202, panelWidth, 0xffa1a3af);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void draw(GuiGraphics graphics, String text, int x, int y, int w, int color) {
      graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(0, w)), x, y, color, false);
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
