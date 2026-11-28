package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.PaletteHex;
import dev.schizo.client.modules.client.UiPalette;
import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Edits six detached colors. Accept only changes the parent's preview. */
final class CustomPaletteScreen extends StudioScreen {
   private static final String[] LABELS = {"Accent", "Background", "Button", "Enabled", "Tint", "Gradient"};
   private final Screen parent;
   private final Consumer<UiPalette> accepted;
   private final String[] values;
   private final EditBox[] fields = new EditBox[6];
   private final Color[] parsed = new Color[6];
   private Button preview;
   private StudioViewportLayouts.CustomPalette layout;
   private int left, top, panelWidth;
   private boolean compact;

   CustomPaletteScreen(Screen parent, UiPalette initial, Consumer<UiPalette> accepted) {
      super(Component.literal("Custom palette"));
      this.parent = parent;
      this.accepted = accepted;
      values = new String[]{PaletteHex.format(initial.accent()), PaletteHex.format(initial.background()),
         PaletteHex.format(initial.button()), PaletteHex.format(initial.enabled()), PaletteHex.format(initial.tint()), PaletteHex.format(initial.gradient())};
   }

   @Override protected void init() {
      super.init();
      layout = StudioViewportLayouts.customPalette(width, height);
      panelWidth = layout.width();
      left = layout.left();
      top = layout.top();
      compact = layout.compact();
      preview = null;
      java.util.Arrays.fill(fields, null);
      if (compact) {
         trackResizeFocus("back", addRenderableWidget(new StudioButton(left, Math.max(1, height - 25), panelWidth, 20, Component.literal("Back"), button -> onClose())));
         return;
      }
      for (int i = 0; i < fields.length; i++) {
         final int index = i;
         int x = layout.fieldX(i), y = layout.fieldY(i);
         var field = new StudioEditBox(font, x, y, layout.fieldWidth(), 18, Component.literal(LABELS[i]));
         field.setMaxLength(9);
         field.setValue(values[i]);
         field.setResponder(text -> {
            values[index] = text;
            validate();
         });
         fields[i] = trackResizeFocus("field:" + index, addRenderableWidget(field));
         trackResizeFocus("picker:" + index, addRenderableWidget(new StudioButton(layout.pickerX(i), y, 30, 18, Component.literal("Pick"), button -> {
            Color initial = parsed[index] == null ? Color.WHITE : parsed[index];
            openStudioChild(new PalettePickerScreen(this, LABELS[index], initial, color -> {
               values[index] = PaletteHex.format(color);
               validate();
            }));
         })));
      }
      int half = (panelWidth - 6) / 2;
      preview = trackResizeFocus("preview", addRenderableWidget(new StudioButton(left, layout.footerY(), half, 20, Component.literal("Preview colors"), button -> accept())));
      trackResizeFocus("cancel", addRenderableWidget(new StudioButton(left + half + 6, layout.footerY(), half, 20, Component.literal("Cancel"), button -> onClose())));
      validate();
   }

   private void validate() {
      boolean valid = true;
      for (int i = 0; i < values.length; i++) {
         try { parsed[i] = PaletteHex.parse(values[i]); }
         catch (IllegalArgumentException invalid) { parsed[i] = null; valid = false; }
      }
      if (preview != null) preview.active = valid;
   }

   private void accept() {
      validate();
      if (!preview.active) return;
      accepted.accept(new UiPalette(parsed[0], parsed[1], parsed[2], parsed[3], parsed[4], parsed[5]));
      onClose();
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      if (compact) {
         graphics.drawString(font, font.plainSubstrByWidth("Reduce GUI scale to edit colors", panelWidth), left, top, 0xffeeeeef, false);
      } else {
         graphics.fill(left - 4, top, left + panelWidth + 4, layout.panelBottom(), theme.panel());
         graphics.fill(left - 4, top, left + panelWidth + 4, top + 1, theme.accent());
         graphics.drawString(font, "CUSTOM PALETTE / PREVIEW", left, top + 6, theme.accent(), false);
         graphics.drawString(font, font.plainSubstrByWidth("#RRGGBBAA or #RRGGBB | fixed colors only", panelWidth), left, top + 17, 0xffb8b9c7, false);
         for (int i = 0; i < fields.length; i++) {
            int x = layout.fieldX(i), y = layout.fieldY(i);
            graphics.drawString(font, LABELS[i], x, y - 10, parsed[i] == null ? 0xffff7d83 : 0xffeeeeef, false);
            int swatchX = layout.swatchX(i);
            graphics.fill(swatchX, y, swatchX + 14, y + 18, 0xff444651);
            graphics.fill(swatchX + 1, y + 1, swatchX + 13, y + 17, 0xff252630);
            if (parsed[i] != null) graphics.fill(swatchX + 1, y + 1, swatchX + 13, y + 17, parsed[i].getRGB());
         }
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { returnToParent(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
