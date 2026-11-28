package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.ThemePreviewDraft;
import dev.schizo.client.gui.ThemeShareCode;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.utils.FloatAnimation;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Theme sample is live-rendered locally without touching real settings until Apply. */
public final class ThemeStudioScreen extends StudioScreen {
   private final Screen parent;
   private ThemePreviewDraft draft;
   private final FloatAnimation switchMotion = new FloatAnimation();
   private boolean sampleEnabled = true;
   private Button apply;
   private int left, top, panelWidth, previewTop, previewHeight;
   private String notice = "Preview only. No settings changed.";

   public ThemeStudioScreen(Screen parent) {
      super(Component.literal("Schizo Theme Studio"));
      this.parent = parent;
      reload();
      switchMotion.setValue(1.0F);
   }

   private void reload() {
      UIModule ui = UIModule.INSTANCE;
      draft = new ThemePreviewDraft(ui.paletteSetting.getValue(), ui.snapshotPalette());
   }

   @Override protected void init() {
      super.init();
      panelWidth = Math.max(1, Math.min(520, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(4, (height - 250) / 2);
      int w = Math.max(1, (panelWidth - 8) / 3);
      int presetWidth = Math.max(1, (panelWidth - 12) / 4);
      PaletteChoice[] choices = {PaletteChoice.SCHIZO, PaletteChoice.MIDNIGHT, PaletteChoice.FROST, PaletteChoice.AMETHYST};
      for (int i = 0; i < choices.length; i++) {
         PaletteChoice choice = choices[i];
         button(choice.getName(), left + i * (presetWidth + 4), top + 29, presetWidth, () -> choose(choice));
      }
      button("Current", left, top + 52, w, () -> {
         reload();
         notice = "Current colors loaded.";
         refreshApply();
      });
      button("Test switch", left + w + 4, top + 52, w, () -> {
         sampleEnabled = !sampleEnabled;
         switchMotion.setTarget(sampleEnabled, 160L);
      });
      button("Edit colors", left + 2 * (w + 4), top + 52, w, () -> {
         minecraft.setScreen(new CustomPaletteScreen(this, draft.preview(), colors -> {
            draft.selectCustom(colors);
            notice = "Custom preview. Apply to save six fixed colors.";
         }));
      });
      button("Saved themes", left, top + 75, w, () -> {
         minecraft.setScreen(new SavedThemesScreen(this, draft.preview(), colors -> {
            draft.selectCustom(colors);
            notice = "Saved theme preview. Apply to change live colors.";
         }));
      });
      button("Copy theme", left + w + 4, top + 75, w, this::copyTheme);
      button("Paste theme", left + 2 * (w + 4), top + 75, w, this::pasteTheme);
      previewTop = top + 103;
      int bottom = Math.min(height - 25, top + 226);
      previewHeight = Math.max(1, bottom - previewTop - 15);
      button("Back", left, bottom, w, this::onClose);
      apply = button("Apply palette", left + w + 4, bottom, w, this::commit);
      button("Discard preview", left + 2 * (w + 4), bottom, w, () -> {
         reload();
         notice = "Preview discarded. Current colors preserved.";
         refreshApply();
      });
      refreshApply();
   }

   private Button button(String label, int x, int y, int w, Runnable action) {
      return addRenderableWidget(new StudioButton(x, y, w, 20, Component.literal(label), ignored -> action.run()));
   }

   private void copyTheme() {
      try {
         minecraft.keyboardHandler.setClipboard(ThemeShareCode.encode(draft.preview()));
         notice = "Theme code copied. Only six palette colors are included.";
      } catch (RuntimeException failure) {
         notice = "Clipboard unavailable.";
      }
   }

   private void pasteTheme() {
      try {
         UiPalette imported = ThemeShareCode.decode(minecraft.keyboardHandler.getClipboard());
         draft.selectCustom(imported);
         notice = "Imported preview. Apply to change live colors.";
         refreshApply();
      } catch (IllegalArgumentException invalid) {
         notice = "Invalid theme code. Existing preview preserved.";
      } catch (RuntimeException failure) {
         notice = "Clipboard unavailable. Existing preview preserved.";
      }
   }

   private void choose(PaletteChoice choice) {
      draft.select(choice);
      notice = "Preview: " + choice.getName() + " (not applied)";
      refreshApply();
   }

   private void refreshApply() {
      if (apply == null) return;
      UIModule ui = UIModule.INSTANCE;
      apply.active = draft.canApply(ui.paletteSetting.getValue(), ui.snapshotPalette())
         && (draft.selected() != PaletteChoice.CUSTOM || ui.supportsFixedPaletteEditing());
   }

   private void commit() {
      UIModule ui = UIModule.INSTANCE;
      if (!draft.canApply(ui.paletteSetting.getValue(), ui.snapshotPalette())) {
         notice = "Live colors changed. Reload Current before applying.";
         refreshApply();
         return;
      }
      if (draft.selected() == PaletteChoice.CUSTOM) {
         if (!draft.applyCustom(ui)) {
            notice = "Custom apply blocked: live colors or dynamic modes changed.";
            refreshApply();
            return;
         }
         notice = "Custom palette applied. Notification colors unchanged.";
      } else {
         ui.paletteSetting.setValue(draft.selected());
         notice = "Palette applied, including notification accents. Normal save lifecycle follows.";
      }
      reload();
      refreshApply();
   }

   @Override public void tick() {
      UIModule ui = UIModule.INSTANCE;
      if (draft.isStale(ui.paletteSetting.getValue(), ui.snapshotPalette())) {
         notice = "Live colors changed. Reload Current before applying.";
      }
      refreshApply();
   }

   private static int opaque(Color color) { return color.getRGB() | 0xff000000; }

   private void line(GuiGraphics graphics, String text, int x, int y, int available, int color) {
      graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(0, available)), x, y, color, false);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      graphics.fill(left - 4, top, left + panelWidth + 4, Math.min(height, previewTop + previewHeight + 40), theme.panel());
      graphics.fill(left - 4, top, left + panelWidth + 4, top + 2, theme.accent());
      line(graphics, "SCHIZO / THEME STUDIO", left, top + 8, panelWidth, theme.accent());
      line(graphics, draft.selected().getName() + "  |  Interactive palette sample", left, top + 19, panelWidth, 0xffb8b9c7);
      UiPalette palette = draft.preview();
      int accent = opaque(palette.accent()), surface = opaque(palette.background()), buttonColor = opaque(palette.button());
      int x = left, y = previewTop, right = left + panelWidth;
      graphics.fill(x, y, right, y + previewHeight, surface);
      graphics.fill(x, y, right, y + 2, accent);
      if (previewHeight >= 45) {
         int nav = Math.max(22, panelWidth / 4);
         graphics.fill(x + 2, y + 3, x + nav, y + previewHeight - 2, opaque(palette.tint()));
         line(graphics, "STUDIO", x + 8, y + 8, nav - 12, accent);
         line(graphics, "Modules", x + 8, y + 25, nav - 12, 0xffeeeeef);
         int contentX = x + nav + 7;
         graphics.fill(contentX, y + 8, right - 6, y + 24, buttonColor);
         line(graphics, "Search settings", contentX + 5, y + 12, right - contentX - 12, 0xffb8b9c7);
         graphics.fill(contentX, y + 29, right - 6, y + 43, sampleEnabled ? opaque(palette.enabled()) : buttonColor);
         line(graphics, "Sample setting", contentX + 5, y + 32, right - contentX - 39, 0xffeeeeef);
         float progress = Math.max(0, Math.min(1, switchMotion.getValue()));
         int tx = right - 32;
         graphics.fill(tx, y + 31, tx + 22, y + 41, sampleEnabled ? accent : 0xff42444d);
         int thumb = tx + 2 + Math.round(12 * progress);
         graphics.fill(thumb, y + 33, thumb + 6, y + 39, 0xffeeeeef);
         if (previewHeight >= 66) {
            graphics.fill(contentX, y + 51, right - 6, y + 53, buttonColor);
            graphics.fill(contentX, y + 51, contentX + Math.max(1, (right - 6 - contentX) * 2 / 3), y + 53, accent);
            line(graphics, "Accent / surface / controls", contentX, y + 57, right - contentX - 6, 0xffb8b9c7);
         }
      }
      line(graphics, notice, left, previewTop + previewHeight + 3, panelWidth, 0xffa1a3af);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
