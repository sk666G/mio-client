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
import java.util.Locale;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only module details and explicit reversible edits of reviewed local scalar controls. */
public final class ModuleInspectorScreen extends StudioScreen {
   private final Screen parent;
   private final Module module;
   private final SettingEditHistory history = new SettingEditHistory();
   private final List<Setting<?>> allSettings = new ArrayList<>();
   private List<Setting<?>> settings = List.of();
   private String filterText = "";
   private final ModuleCompatibility.Assessment assessment;
   private int selected, left, top, panelWidth, rowCount, offset;
   private EditBox value, filter;
   private Button resetDefault;
   private Button apply, undo, redo;
   private JsonElement expected;
   private String notice = "";
   private String bindingNotice = "";
   private long bindingRefresh;

   public ModuleInspectorScreen(Screen parent, Module module) {
      this(parent, module, null);
   }

   public ModuleInspectorScreen(Screen parent, Module module, Setting<?> target) {
      super(Component.literal("Schizo Module Inspector"));
      this.parent = parent;
      this.module = module;
      this.assessment = ModuleCompatibility.assess(module.getClass());
      for (Object item : module.getSettings()) {
         if (item instanceof Setting<?> setting && !setting.isGroupOnly() && !setting.isExcludedFromConfig()) allSettings.add(setting);
      }
      settings = List.copyOf(allSettings);
      if (target != null) {
         boolean found = false;
         for (int index = 0; index < settings.size(); index++) {
            if (settings.get(index) == target) {
               selected = index;
               found = true;
               break;
            }
         }
         notice = found ? "Selected from search: " + target.getName() : "Setting unavailable in inspector.";
      }
   }

   /** Exact selected setting identity, including read-only targets. */
   public Setting<?> selectedSetting() { return current(); }

   @Override protected void init() {
      super.init();
      panelWidth = Math.max(1, Math.min(550, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(4, (height - 320) / 2);
      rowCount = Math.max(1, Math.min(9, (height - top - 209) / 13));
      int controlsY = top + 106 + rowCount * 13;
      filter = new StudioEditBox(font, left, top + 75, panelWidth, 20, Component.literal("Filter settings"));
      filter.setMaxLength(64);
      filter.setValue(filterText);
      filter.setHint(Component.literal("Filter settings..."));
      addRenderableWidget(filter);
      int half = Math.max(1, (panelWidth - 4) / 2);
      button("Previous setting", left, controlsY, half, () -> select(-1));
      button("Next setting", left + half + 4, controlsY, half, () -> select(1));
      value = new StudioEditBox(font, left, controlsY + 24, half, 20, Component.literal("Proposed value"));
      value.setMaxLength(48);
      addRenderableWidget(value);
      apply = button("Apply value", left + half + 4, controlsY + 24, half, this::applyValue);
      int quarter = Math.max(1, (panelWidth - 12) / 4);
      undo = button("Undo", left, controlsY + 48, quarter, () -> replay(true));
      redo = button("Redo", left + quarter + 4, controlsY + 48, quarter, () -> replay(false));
      resetDefault = button("Default", left + 2 * (quarter + 4), controlsY + 48, quarter, this::resetDefault);
      button("Back", left + 3 * (quarter + 4), controlsY + 48, quarter, this::onClose);
      filter.setResponder(this::filterSettings);
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
      return addRenderableWidget(new StudioButton(x, y, w, 20, Component.literal(label), ignored -> action.run()));
   }

   private Setting<?> current() { return settings.isEmpty() ? null : settings.get(selected); }

   private boolean editable(Setting<?> setting) {
      return setting != null && !assessment.requiresMultiplayerWarning()
         && SettingEditHistory.supports(setting) && setting.isVisible();
   }

   private void filterSettings(String text) {
      Setting<?> previous = current();
      filterText = text;
      String query = text.strip().toLowerCase(Locale.ROOT);
      settings = allSettings.stream().filter(setting -> setting.getName().toLowerCase(Locale.ROOT).contains(query)
         || setting.getConfigName().toLowerCase(Locale.ROOT).contains(query)).toList();
      selected = 0;
      for (int i = 0; i < settings.size(); i++) if (settings.get(i) == previous) { selected = i; break; }
      offset = 0;
      refreshSelection();
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      int row = InspectorRows.hit(event.x(), event.y(), left, top + 100, panelWidth, rowCount, offset, settings.size());
      if (event.button() == 0 && row >= 0) {
         selected = row;
         refreshSelection();
         setFocused(null);
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (vertical != 0 && x >= left && x < left + panelWidth && y >= top + 100 && y < top + 100 + rowCount * 13) {
         offset = Math.max(0, Math.min(Math.max(0, settings.size() - rowCount), offset + (vertical < 0 ? 1 : -1)));
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   private void select(int delta) {
      if (settings.isEmpty()) return;
      selected = Math.floorMod(selected + delta, settings.size());
      refreshSelection();
   }

   private void refreshSelection() {
      offset = InspectorRows.reveal(selected, offset, rowCount, settings.size());
      var setting = current();
      boolean supported = editable(setting);
      expected = supported ? setting.toJson().deepCopy() : null;
      value.setEditable(supported);
      value.setValue(supported ? expected.getAsString() : "");
      apply.active = supported;
      resetDefault.active = supported;
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

   private void resetDefault() {
      var setting = current();
      if (!editable(setting) || expected == null) return;
      Object original = setting.getDefaultValue();
      JsonPrimitive target;
      if (original instanceof Boolean flag) target = new JsonPrimitive(flag);
      else if (original instanceof Number number) target = new JsonPrimitive(number);
      else return;
      notice = "Default: " + history.apply(setting, expected, target);
      refreshSelection();
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
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      graphics.fill(left - 4, top, left + panelWidth + 4, Math.min(height, top + 196 + rowCount * 13), theme.panel());
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 2, theme.accent());
      line(graphics, "SCHIZO / INSPECTOR / " + module.getName(), top + 8, theme.accent());
      line(graphics, module.getDescription(), top + 21, 0xffeeeeef);
      line(graphics, "Compatibility: " + assessment.classification() + " | individual settings unverified", top + 34, 0xffa1a3af);
      line(graphics, bindingNotice, top + 47, 0xffb8b9c7);
      line(graphics, notice.isEmpty() ? "Local scalar edits only | history lasts for this inspector session" : notice, top + 60, 0xffa1a3af);
      if (settings.isEmpty()) line(graphics, "No matching settings.", top + 101, 0xffa1a3af);
      for (int i = 0; i < rowCount && offset + i < settings.size(); i++) {
         int index = offset + i;
         Setting<?> setting = settings.get(index);
         int y = top + 100 + i * 13;
         boolean hover = mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + 12;
         graphics.fill(left, y, left + panelWidth, y + 12, index == selected ? theme.selected() : hover ? theme.hover() : theme.row());
         if (index == selected) graphics.fill(left, y, left + 2, y + 12, theme.accent());
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
