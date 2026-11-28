package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.ThemePreviewDraft;
import dev.schizo.client.gui.ThemeShareCode;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.utils.FloatAnimation;
import java.util.Locale;
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
   private boolean lightBackdrop;
   private UiPalette cachedPalette;
   private ThemeSampleColors cachedSample;
   private boolean cachedLight;
   private Button apply, undoPreview, redoPreview;
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
      int half = Math.max(1, (w - 4) / 2);
      button("Test switch", left + w + 4, top + 52, half, () -> {
         sampleEnabled = !sampleEnabled;
         switchMotion.setTarget(sampleEnabled, 160L);
      });
      button("Backdrop", left + w + 8 + half, top + 52, Math.max(1,w-half-4), () -> lightBackdrop = !lightBackdrop);
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
      previewHeight = Math.max(1, bottom - previewTop - 27);
      int small = Math.max(1, (panelWidth - 16) / 5);
      button("Back", left, bottom, small, this::onClose);
      undoPreview = button("Undo", left + small + 4, bottom, small, () -> changeHistory(false));
      redoPreview = button("Redo", left + 2 * (small + 4), bottom, small, () -> changeHistory(true));
      apply = button("Apply palette", left + 3 * (small + 4), bottom, small, this::commit);
      button("Discard preview", left + 4 * (small + 4), bottom, small, () -> {
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

   private void changeHistory(boolean redo) {
      boolean changed = redo ? draft.redo() : draft.undo();
      if (changed) notice = redo ? "Preview redone. Live colors unchanged." : "Preview undone. Live colors unchanged.";
      refreshApply();
   }

   @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
      if (dev.schizo.client.utils.KeyUtils.isControlDown()) {
         if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_Z) {
            changeHistory(dev.schizo.client.utils.KeyUtils.isShiftDown());
            return true;
         }
         if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_Y) {
            changeHistory(true);
            return true;
         }
      }
      return super.keyPressed(event);
   }

   private void refreshApply() {
      if (apply == null) return;
      undoPreview.active = draft.canUndo();
      redoPreview.active = draft.canRedo();
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

   private ThemeSampleColors sampleColors() {
      UiPalette palette = draft.preview();
      if (!palette.equals(cachedPalette) || cachedLight != lightBackdrop) {
         cachedSample = ThemeSampleColors.from(palette, lightBackdrop);
         cachedPalette = palette;
         cachedLight = lightBackdrop;
      }
      return cachedSample;
   }

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
      ThemeSampleColors sample = sampleColors();
      int accent = sample.accent(), surface = sample.surface(), buttonColor = sample.button();
      int x = left, y = previewTop, right = left + panelWidth;
      graphics.fill(x, y, right, y + previewHeight, surface);
      graphics.fill(x, y, right, y + 2, accent);
      if (previewHeight >= 45) {
         int nav = Math.max(22, panelWidth / 4);
         graphics.fill(x + 2, y + 3, x + nav, y + previewHeight - 2, sample.nav());
         line(graphics, "STUDIO", x + 8, y + 8, nav - 12, sample.navAccent());
         line(graphics, "Modules", x + 8, y + 25, nav - 12, 0xffeeeeef);
         int contentX = x + nav + 7;
         graphics.fill(contentX, y + 8, right - 6, y + 24, buttonColor);
         line(graphics, "Search settings", contentX + 5, y + 12, right - contentX - 12, 0xffb8b9c7);
         graphics.fill(contentX, y + 29, right - 6, y + 43, sampleEnabled ? sample.enabled() : buttonColor);
         line(graphics, "Sample setting", contentX + 5, y + 32, right - contentX - 39, 0xffeeeeef);
         float progress = Math.max(0, Math.min(1, switchMotion.getValue()));
         int tx = right - 32;
         graphics.fill(tx, y + 31, tx + 22, y + 41, sampleEnabled ? sample.switchAccent() : 0xff42444d);
         int thumb = tx + 2 + Math.round(12 * progress);
         graphics.fill(thumb, y + 33, thumb + 6, y + 39, 0xffeeeeef);
         if (previewHeight >= 66) {
            graphics.fill(contentX, y + 51, right - 6, y + 53, buttonColor);
            graphics.fill(contentX, y + 51, contentX + Math.max(1, (right - 6 - contentX) * 2 / 3), y + 53, accent);
            int stripWidth = Math.max(0, right - contentX - 6);
            for (int i = 0; i < 12; i++) {
               int sx = contentX + i * stripWidth / 12, ex = contentX + (i + 1) * stripWidth / 12;
               graphics.fill(sx, y + 56, ex, y + 64, sample.gradient(i / 11.0));
            }
         }
      }
      line(graphics, notice, left, previewTop + previewHeight + 3, panelWidth, 0xffa1a3af);
      double contrast = sample.minimumTextContrast();
      String diagnostic = String.format(Locale.ROOT, "%s backdrop | sample contrast %.2f:1 | %s",
         lightBackdrop ? "Light" : "Dark", contrast, contrast >= 4.5 ? "readable target met" : "low contrast");
      line(graphics, diagnostic, left, previewTop + previewHeight + 14, panelWidth, contrast >= 4.5 ? 0xffb8b9c7 : 0xffffa66b);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
