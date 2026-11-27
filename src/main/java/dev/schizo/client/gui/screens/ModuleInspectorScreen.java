package dev.schizo.client.gui.screens;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.schizo.client.gui.SettingEditHistory;
import dev.schizo.client.diagnostics.ClientBindingDiagnostics;
import dev.schizo.client.integrations.ModuleCompatibility;
import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only module details and explicit reversible edits of reviewed local scalar controls. */
public final class ModuleInspectorScreen extends Screen {
   private final Screen parent;
   private final Module module;
   private final SettingEditHistory history = new SettingEditHistory();
   private final List<Setting<?>> settings = new ArrayList<>();
   private final ModuleCompatibility.Assessment assessment;
   private int selected, left, top, panelWidth, rowCount, offset;
   private EditBox value;
   private Button apply, undo, redo;
   private JsonElement expected;
   private String notice = "";
   private String bindingNotice = "";
   private long bindingRefresh;

   public ModuleInspectorScreen(Screen parent, Module module) {
      super(Component.literal("Schizo Module Inspector"));
      this.parent = parent;
      this.module = module;
      this.assessment = ModuleCompatibility.assess(module.getClass());
      for (Object item : module.getSettings()) {
         if (item instanceof Setting<?> setting && !setting.isGroupOnly() && !setting.isExcludedFromConfig()) settings.add(setting);
      }
   }

   @Override protected void init() {
      panelWidth = Math.max(1, Math.min(550, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(4, (height - 320) / 2);
      rowCount = Math.max(1, Math.min(9, (height - top - 168) / 13));
      int controlsY = top + 80 + rowCount * 13;
      int half = Math.max(1, (panelWidth - 4) / 2);
      button("Previous setting", left, controlsY, half, () -> select(-1));
      button("Next setting", left + half + 4, controlsY, half, () -> select(1));
      value = new EditBox(font, left, controlsY + 24, half, 20, Component.literal("Proposed value"));
      value.setMaxLength(48);
      addRenderableWidget(value);
      apply = button("Apply value", left + half + 4, controlsY + 24, half, this::applyValue);
      int third = Math.max(1, (panelWidth - 8) / 3);
      undo = button("Undo", left, controlsY + 48, third, () -> replay(true));
      redo = button("Redo", left + third + 4, controlsY + 48, third, () -> replay(false));
      button("Back", left + 2 * (third + 4), controlsY + 48, third, this::onClose);
      refreshSelection();
      refreshBindingNotice();
   }

   private void refreshBindingNotice() {
      var binding = module.getKeybind();
      int others = ClientBindingDiagnostics.capture().othersFor(binding);
      bindingNotice = binding == null || binding.isUnbound() ? "Module input: unbound"
         : others == 0 ? "Module input: no shared binding recorded"
         : "Shared input: " + others + " other module binding(s); may be intentional";
      bindingRefresh = System.nanoTime();
   }

   @Override public void tick() {
      if (System.nanoTime() - bindingRefresh >= 500_000_000L) refreshBindingNotice();
   }

   private Button button(String label, int x, int y, int w, Runnable action) {
      return addRenderableWidget(Button.builder(Component.literal(label), ignored -> action.run()).bounds(x, y, w, 20).build());
   }

   private Setting<?> current() { return settings.isEmpty() ? null : settings.get(selected); }

   private boolean editable(Setting<?> setting) {
      return setting != null && !assessment.requiresMultiplayerWarning()
         && SettingEditHistory.supports(setting) && setting.isVisible();
   }

   private void select(int delta) {
      if (settings.isEmpty()) return;
      selected = Math.floorMod(selected + delta, settings.size());
      refreshSelection();
   }

   private void refreshSelection() {
      offset = Math.max(0, Math.min(selected, Math.max(0, settings.size() - rowCount)));
      var setting = current();
      boolean supported = editable(setting);
      expected = supported ? setting.toJson().deepCopy() : null;
      value.setEditable(supported);
      value.setValue(supported ? expected.getAsString() : "");
      apply.active = supported;
      undo.active = history.canUndo();
      redo.active = history.canRedo();
   }

   private void applyValue() {
      Setting<?> setting = current();
      if (!editable(setting) || expected == null) { notice = "Read-only setting."; return; }
      try {
         String raw = value.getValue().strip();
         JsonPrimitive proposed;
         if (setting instanceof BooleanSetting) {
            if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
               notice = "Enter true or false.";
               return;
            }
            proposed = new JsonPrimitive(Boolean.parseBoolean(raw));
         } else {
            double number = Double.parseDouble(raw);
            if (!Double.isFinite(number)) { notice = "Enter a finite number."; return; }
            proposed = new JsonPrimitive(number);
         }
         notice = "Edit: " + history.apply(setting, expected, proposed);
         refreshSelection();
      } catch (NumberFormatException invalid) {
         notice = "Invalid number.";
      }
   }

   private void replay(boolean backward) {
      notice = (backward ? "Undo: " : "Redo: ") + (backward ? history.undo() : history.redo());
      refreshSelection();
   }

   private String displayValue(Setting<?> setting) {
      if (setting instanceof BooleanSetting || setting instanceof NumberSetting<?> || setting.hasEnumValue()) {
         return setting.toJson().getAsString();
      }
      return "Use original controls";
   }

   private void line(GuiGraphics graphics, String text, int y, int color) {
      graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(0, panelWidth - 8)), left + 4, y, color, false);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, width, height, 0xee08090d);
      graphics.fill(left - 4, top, left + panelWidth + 4, Math.min(height, top + 170 + rowCount * 13), 0xff13141a);
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 2, 0xffe7394d);
      line(graphics, "SCHIZO / INSPECTOR / " + module.getName(), top + 8, 0xffe7394d);
      line(graphics, module.getDescription(), top + 21, 0xffeeeeef);
      line(graphics, "Compatibility: " + assessment.classification() + " | individual settings unverified", top + 34, 0xffa1a3af);
      line(graphics, bindingNotice, top + 47, 0xffb8b9c7);
      line(graphics, notice.isEmpty() ? "Local scalar edits only | history lasts for this inspector session" : notice, top + 60, 0xffa1a3af);
      if (settings.isEmpty()) line(graphics, "No configurable settings.", top + 75, 0xffa1a3af);
      for (int i = 0; i < rowCount && offset + i < settings.size(); i++) {
         int index = offset + i;
         Setting<?> setting = settings.get(index);
         int y = top + 74 + i * 13;
         graphics.fill(left, y, left + panelWidth, y + 12, index == selected ? 0xff48232c : 0xff21222a);
         String text = setting.getName() + ": " + displayValue(setting) + (editable(setting) ? "" : " [read-only]");
         line(graphics, text, y + 2, index == selected ? 0xffeeeeef : 0xffb8b9c7);
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() {
      history.clear();
      minecraft.setScreen(parent);
   }

   @Override public boolean isPauseScreen() { return false; }
}
