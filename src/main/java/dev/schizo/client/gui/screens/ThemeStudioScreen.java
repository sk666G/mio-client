package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.ThemePreviewDraft;
import dev.schizo.client.gui.AppliedPaletteHistory;
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
   private String cachedDiagnostic;
   private double cachedContrast;
   private Button apply, undoPreview, redoPreview, undoApplied, redoApplied;
   private final AppliedPaletteHistory appliedHistory = new AppliedPaletteHistory();
   private int left, top, panelWidth, previewTop, previewHeight, bottom;
   private boolean compact;
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
      var layout = StudioViewportLayouts.theme(width,height);
      panelWidth=layout.width(); left=layout.left(); top=layout.top();
      compact=layout.compact(); previewTop=layout.previewTop(); previewHeight=layout.previewHeight(); bottom=layout.bottom();
      undoApplied = null;
      redoApplied = null;
      apply = undoPreview = redoPreview = null;
      if (compact) {
         button("Back",left,Math.max(0,height-24),panelWidth,this::onClose);
         return;
      }
      int w = Math.max(1, (panelWidth - 8) / 3);
      int presetWidth = Math.max(1, (panelWidth - 12) / 4);
      PaletteChoice[] choices = {PaletteChoice.SCHIZO, PaletteChoice.MIDNIGHT, PaletteChoice.FROST, PaletteChoice.AMETHYST};
      for (int i = 0; i < choices.length; i++) {
         PaletteChoice choice = choices[i];
         button(choice.getName(), left + i * (presetWidth + 4), top + 29, presetWidth, () -> choose(choice));
      }
      button("Gallery",left,top+52,w,()->openStudioChild(new ThemeGalleryScreen(this,draft.preview(),lightBackdrop)))
         .help("Inspect detached palette and Studio component color samples with contrast diagnostics.");
      int half = Math.max(1, (w - 4) / 2);
      button("Test switch", left + w + 4, top + 52, half, () -> {
         sampleEnabled = !sampleEnabled;
         switchMotion.setTarget(sampleEnabled, 160L);
      });
      button("Backdrop", left + w + 8 + half, top + 52, Math.max(1,w-half-4), () -> lightBackdrop = !lightBackdrop);
      button("Edit colors", left + 2 * (w + 4), top + 52, w, () -> {
         openStudioChild(new CustomPaletteScreen(this, draft.preview(), colors -> {
            draft.selectCustom(colors);
            notice = "Custom preview. Apply to save six fixed colors.";
         }));
      });
      button("Saved themes", left, top + 75, w, () -> {
         openStudioChild(new SavedThemesScreen(this, draft.preview(), colors -> {
            draft.selectCustom(colors);
            notice = "Saved theme preview. Apply to change live colors.";
         }));
      });
      button("Copy theme", left + w + 4, top + 75, w, this::copyTheme).help("Copy the detached preview palette to the clipboard.");
      button("Paste theme", left + 2 * (w + 4), top + 75, w, this::pasteTheme).help("Import a palette into the preview without applying live colors.");
      boolean showApplied = layout.appliedControls();
      if (showApplied) {
         undoApplied = button("Undo applied", left, top + 98, w, () -> changeApplied(false));
         redoApplied = button("Redo applied", left + w + 4, top + 98, w, () -> changeApplied(true));
      }
      int small = Math.max(1, (panelWidth - 16) / 5);
      button("Back", left, bottom, small, this::onClose);
      undoPreview = button("Undo", left + small + 4, bottom, small, () -> changeHistory(false));
      redoPreview = button("Redo", left + 2 * (small + 4), bottom, small, () -> changeHistory(true));
      apply = button("Apply palette", left + 3 * (small + 4), bottom, small, this::commit).help("Apply the preview palette to the live interface.");
      button("Reload current", left + 4 * (small + 4), bottom, small, () -> {
         reload();
         notice = "Preview discarded. Current colors loaded.";
         refreshApply();
      }).help("Discard the detached preview and reload the current live palette.");
      refreshApply();
   }

   private StudioButton button(String label, int x, int y, int w, Runnable action) {
      return trackResizeFocus("button:" + label, addRenderableWidget(new StudioButton(x, y, w, 20, Component.literal(label), ignored -> action.run())));
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
      if (!compact && dev.schizo.client.utils.KeyUtils.isControlDown()) {
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

   private AppliedPaletteHistory.State liveState(UIModule ui) {
      return new AppliedPaletteHistory.State(ui.paletteSetting.getValue(), ui.snapshotPalette());
   }

   private boolean canChangeApplied(UIModule ui, AppliedPaletteHistory.State live) {
      return ui.supportsFixedPaletteEditing() && !draft.isStale(live.choice(), live.colors())
         && !draft.canApply(live.choice(), live.colors());
   }

   private void changeApplied(boolean redo) {
      UIModule ui = UIModule.INSTANCE;
      var live = liveState(ui);
      if (!canChangeApplied(ui, live)) {
         notice = "Discard or reload the preview before restoring an applied palette.";
         refreshApply();
         return;
      }
      boolean restored = appliedHistory.move(live, redo, target ->
         ui.restoreFixedPalette(live.choice(), live.colors(), target.choice(), target.colors()));
      if (restored) {
         reload();
         notice = redo ? "Applied palette redone." : "Applied palette undone.";
      } else notice = "Applied history unavailable or live colors changed.";
      refreshApply();
   }

   private void refreshApply() {
      if (apply == null) return;
      undoPreview.active = draft.canUndo();
      redoPreview.active = draft.canRedo();
      UIModule ui = UIModule.INSTANCE;
      var live = liveState(ui);
      apply.active = draft.canApply(live.choice(), live.colors())
         && (draft.selected() != PaletteChoice.CUSTOM || ui.supportsFixedPaletteEditing());
      if (!ui.supportsFixedPaletteEditing()) appliedHistory.clear();
      else appliedHistory.invalidateIfStale(live);
      if (undoApplied != null) {
         boolean clean = canChangeApplied(ui, live);
         undoApplied.active = clean && appliedHistory.canMove(live, false);
         redoApplied.active = clean && appliedHistory.canMove(live, true);
      }
   }

   private void commit() {
      UIModule ui = UIModule.INSTANCE;
      if (!draft.canApply(ui.paletteSetting.getValue(), ui.snapshotPalette())) {
         notice = "Live colors changed. Reload Current before applying.";
         refreshApply();
         return;
      }
      if (draft.selected() == PaletteChoice.CUSTOM) {
         var before = liveState(ui);
         if (!draft.applyCustom(ui)) {
            notice = "Custom apply blocked: live colors or dynamic modes changed.";
            refreshApply();
            return;
         }
         appliedHistory.record(before, liveState(ui));
         notice = "Custom palette applied. Notification colors unchanged.";
      } else {
         ui.paletteSetting.setValue(draft.selected());
         appliedHistory.clear();
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
         cachedContrast=cachedSample.minimumTextContrast();
         cachedDiagnostic=String.format(Locale.ROOT,"%s backdrop | sample contrast %.2f:1 | %s",
            lightBackdrop?"Light":"Dark",cachedContrast,cachedContrast>=4.5?"readable target met":"low contrast");
      }
      return cachedSample;
   }

   private void line(GuiGraphics graphics, String text, int x, int y, int available, int color) {
      graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(0, available)), x, y, color, false);
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      if (compact) {
         line(graphics,"Reduce GUI scale to use Theme Studio",left,top,panelWidth,0xffeeeeef);
         super.render(graphics,mouseX,mouseY,partialTick);
         return;
      }
      graphics.fill(left - 4, top, left + panelWidth + 4, Math.min(height, bottom + 24), theme.panel());
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
      line(graphics,cachedDiagnostic,left,previewTop+previewHeight+14,panelWidth,cachedContrast>=4.5?0xffb8b9c7:0xffffa66b);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { returnToParent(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
