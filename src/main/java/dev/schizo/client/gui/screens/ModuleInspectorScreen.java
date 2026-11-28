package dev.schizo.client.gui.screens;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.schizo.client.gui.SettingEditHistory;
import dev.schizo.client.gui.InspectorEnumOptions;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
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
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only module details and explicit reversible edits of reviewed local scalar controls. */
public final class ModuleInspectorScreen extends StudioScreen {
   private final Screen parent;
   private final Module module;
   private final SettingEditHistory history = ScalarHistoryScreen.history();
   private final List<Setting<?>> allSettings = new ArrayList<>();
   private List<Setting<?>> settings = List.of();
   private String filterText = "";
   private final ModuleCompatibility.Assessment assessment;
   private int selected, left, top, panelWidth, rowCount, offset, rowsY, controlsY;
   private boolean compact, condensed;
   private EditBox value, filter;
   private Button resetDefault;
   private Button apply, undo, redo;
   private EditDraft draft = EditDraft.empty();

   /** A widget-independent draft; keep the original comparison value across resize. */
   record EditDraft(JsonElement expected, String text) {
      static EditDraft empty() { return new EditDraft(null, ""); }
      EditDraft withText(String text) { return new EditDraft(expected, text); }
      EditDraft refresh(JsonElement live, boolean preserve) {
         if (live == null) return empty();
         if (preserve && expected != null && !text.equals(expected.getAsString())) return this;
         return new EditDraft(live.deepCopy(), live.getAsString());
      }
   }
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
               RecentSettingsScreen.record(module, target);
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
      var layout = StudioViewportLayouts.inspector(width,height);
      panelWidth=layout.width(); left=layout.left(); top=layout.top();
      compact=layout.compact(); condensed=layout.condensed();
      rowsY=layout.rowsY(); controlsY=layout.controlsY(); rowCount=layout.rows();
      if (compact) {
         value = filter = null;
         apply = undo = redo = resetDefault = null;
         button("Back",left,Math.max(0,height-24),panelWidth,this::onClose);
         return;
      }
      filter = new StudioEditBox(font, left, rowsY - 25, panelWidth, 20, Component.literal("Filter settings"));
      filter.setMaxLength(64);
      filter.setValue(filterText);
      filter.setHint(Component.literal("Filter settings..."));
      trackResizeFocus("filter", addRenderableWidget(filter));
      int half = Math.max(1, (panelWidth - 4) / 2);
      button("Previous setting", left, controlsY, half, () -> select(-1));
      button("Next setting", left + half + 4, controlsY, half, () -> select(1));
      value = new StudioEditBox(font, left, controlsY + 24, half, 20, Component.literal("Proposed value"));
      value.setMaxLength(128);
      trackResizeFocus("value", addRenderableWidget(value));
      apply = button("Apply value", left + half + 4, controlsY + 24, half, this::applyValue).help("Validate and apply the proposed setting value.");
      int quarter = Math.max(1, (panelWidth - 12) / 4);
      undo = button("Undo", left, controlsY + 48, quarter, () -> replay(true));
      redo = button("Redo", left + quarter + 4, controlsY + 48, quarter, () -> replay(false));
      resetDefault = button("Default", left + 2 * (quarter + 4), controlsY + 48, quarter, this::resetDefault).help("Restore the selected setting default through guarded edit history.");
      button("Back", left + 3 * (quarter + 4), controlsY + 48, quarter, this::onClose);
      filter.setResponder(this::filterSettings);
      refreshSelection(true);
      value.setResponder(text -> draft = draft.withText(text));
      refreshBindingNotice();
      setInitialFocus(filter);
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
      if (!compact && !condensed && System.nanoTime() - bindingRefresh >= 500_000_000L) refreshBindingNotice();
   }

   private StudioButton button(String label, int x, int y, int w, Runnable action) {
      return trackResizeFocus("button:" + label, addRenderableWidget(new StudioButton(x, y, w, 20, Component.literal(label), ignored -> action.run())));
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
      refreshSelection(current()==previous);
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if (!compact && (getFocused()==null || getFocused()==filter)) {
         int delta=switch(event.key()) {
            case GLFW.GLFW_KEY_UP -> -1;
            case GLFW.GLFW_KEY_DOWN -> 1;
            case GLFW.GLFW_KEY_PAGE_UP -> -rowCount;
            case GLFW.GLFW_KEY_PAGE_DOWN -> rowCount;
            default -> 0;
         };
         if(delta!=0) {
            int next=InspectorRows.step(selected,delta,settings.size());
            if(next>=0 && next!=selected) {
               selected=next;
               RecentSettingsScreen.record(module,current());
               refreshSelection();
            }
            return true;
         }
      }
      return super.keyPressed(event);
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (compact) return super.mouseClicked(event, doubleClick);
      int row = InspectorRows.hit(event.x(), event.y(), left, rowsY, panelWidth, rowCount, offset, settings.size());
      if (event.button() == 0 && row >= 0) {
         boolean changed=selected!=row;
         selected = row;
         RecentSettingsScreen.record(module, current());
         refreshSelection(!changed);
         setFocused(null);
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (!compact && vertical != 0 && x >= left && x < left + panelWidth && y >= rowsY && y < rowsY + rowCount * 13) {
         offset = Math.max(0, Math.min(Math.max(0, settings.size() - rowCount), offset + (vertical < 0 ? 1 : -1)));
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   private void select(int delta) {
      if (settings.isEmpty()) return;
      int next=Math.floorMod(selected+delta,settings.size());
      boolean changed=next!=selected;
      selected=next;
      RecentSettingsScreen.record(module,current());
      refreshSelection(!changed);
   }

   private void refreshSelection() { refreshSelection(false); }

   private void refreshSelection(boolean preserveDraft) {
      offset = InspectorRows.reveal(selected, offset, rowCount, settings.size());
      var setting = current();
      boolean supported = editable(setting);
      draft = draft.refresh(supported ? setting.toJson() : null, preserveDraft);
      value.setEditable(supported);
      value.setValue(draft.text());
      apply.active = supported;
      resetDefault.active = supported;
      undo.active = setting != null && history.undoTarget() == setting;
      redo.active = setting != null && history.redoTarget() == setting;
   }

   private void applyValue() {
      Setting<?> setting = current();
      JsonElement expected = draft.expected();
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
         } else if(setting instanceof EnumSetting<?>) {
            String canonical=InspectorEnumOptions.canonical(InspectorEnumOptions.selectable(setting),raw);
            if(canonical==null) {notice="Invalid or unavailable choice. Hover the row for valid choices.";return;}
            proposed=new JsonPrimitive(canonical);
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
      JsonElement expected = draft.expected();
      if (!editable(setting) || expected == null) return;
      Object original = setting.getDefaultValue();
      JsonPrimitive target;
      if (original instanceof Boolean flag) target = new JsonPrimitive(flag);
      else if (original instanceof Number number) target = new JsonPrimitive(number);
      else if (original instanceof NamedEntry named && setting instanceof EnumSetting<?>) target=new JsonPrimitive(named.getName());
      else return;
      notice = "Default: " + history.apply(setting, expected, target);
      refreshSelection();
   }

   private void replay(boolean backward) {
      Setting<?> target = backward ? history.undoTarget() : history.redoTarget();
      if (target != current()) {
         notice = "Use Quick Access / Edit history for other settings.";
         return;
      }
      notice = (backward ? "Undo: " : "Redo: ") + (backward ? history.undo() : history.redo());
      refreshSelection();
   }

   private String settingHelp(Setting<?> setting) {
      StringBuilder text=new StringBuilder(setting.getName());
      if (dev.schizo.client.ClientContext.languageTable!=null) {
         String description=dev.schizo.client.ClientContext.languageTable.getTranslation(
            "descriptions.settings."+module.getConfigName()+"."+setting.getConfigName());
         if(description!=null && !description.isBlank() && !description.startsWith("descriptions.settings.")) text.append("\n").append(description);
      }
      if(setting instanceof BooleanSetting || setting instanceof NumberSetting<?>) {
         text.append("\nDefault: ").append(setting.getDefaultValue());
         if(setting.isNumericBoundsEnabled()) text.append("\nRange: ").append(setting.getMinimumValue()).append(" – ").append(setting.getMaximumValue());
      }
      if(setting instanceof EnumSetting<?> && !assessment.requiresMultiplayerWarning()) {
         if(setting.getDefaultValue() instanceof NamedEntry named) text.append("\nDefault: ").append(named.getName());
         if(editable(setting)) text.append("\nChoices: ").append(String.join(", ",InspectorEnumOptions.selectable(setting)));
      }
      if(isModified(setting)) text.append("\nModified from default.");
      if(assessment.requiresMultiplayerWarning()) text.append("\nRead-only: compatibility review required.");
      else if(!setting.isVisible()) text.append("\nRead-only: setting is currently hidden.");
      else if(!SettingEditHistory.supports(setting)) text.append("\nRead-only here: use the original control.");
      else text.append("\nEditable through guarded history.");
      return text.toString();
   }

   private boolean isModified(Setting<?> setting) {
      return (setting instanceof BooleanSetting || setting instanceof NumberSetting<?> || setting instanceof EnumSetting<?>)
         && !java.util.Objects.equals(setting.getValue(),setting.getDefaultValue());
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
      if (compact) {
         line(graphics, "Reduce GUI scale to use the inspector", top, 0xffeeeeef);
         super.render(graphics, mouseX, mouseY, partialTick);
         return;
      }
      graphics.fill(left - 4, top, left + panelWidth + 4, Math.min(height, controlsY + 72), theme.panel());
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 2, theme.accent());
      line(graphics, "SCHIZO / INSPECTOR / " + module.getName(), top + 8, theme.accent());
      if (condensed) {
         line(graphics, "Compatibility: " + assessment.classification() + " | settings unverified", top + 19, 0xffa1a3af);
         line(graphics, notice.isEmpty() ? "Guarded setting edits | Quick Access history" : notice, top + 30, 0xffa1a3af);
      } else {
         line(graphics, module.getDescription(), top + 21, 0xffeeeeef);
         line(graphics, "Compatibility: " + assessment.classification() + " | individual settings unverified", top + 34, 0xffa1a3af);
         line(graphics, bindingNotice, top + 47, 0xffb8b9c7);
         line(graphics, notice.isEmpty() ? "Guarded setting edits | cross-module history in Quick Access" : notice, top + 60, 0xffa1a3af);
      }
      if (settings.isEmpty()) line(graphics, "No matching settings.", rowsY + 1, 0xffa1a3af);
      for (int i = 0; i < rowCount && offset + i < settings.size(); i++) {
         int index = offset + i;
         Setting<?> setting = settings.get(index);
         int y = rowsY + i * 13;
         boolean hover = mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + 12;
         graphics.fill(left, y, left + panelWidth, y + 12, index == selected ? theme.selected() : hover ? theme.hover() : theme.row());
         if (index == selected) graphics.fill(left, y, left + 2, y + 12, theme.accent());
         String text = (isModified(setting)?"* ":"")+setting.getName()+": "+displayValue(setting)+(editable(setting)?"":" [read-only]");
         line(graphics, text, y + 2, index == selected ? 0xffeeeeef : 0xffb8b9c7);
      }
      int hoverIndex=compact?-1:InspectorRows.hit(mouseX,mouseY,left,rowsY,panelWidth,rowCount,offset,settings.size());
      if (rowHelpReady(hoverIndex<0?null:settings.get(hoverIndex))) {
         Setting<?> hovered=settings.get(hoverIndex);
         showRowHelp(graphics,settingHelp(hovered),mouseX,mouseY);
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() {
      returnToParent(parent);
   }

   @Override public boolean isPauseScreen() { return false; }
}
