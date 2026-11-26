package dev.schizo.client.gui.screens;

import com.google.gson.JsonObject;
import dev.schizo.client.ClientContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudLayoutPreviewScreen extends Screen {
   private final JsonObject layout;
   private HudEditorScreen.LayoutPreview preview;
   private String status = "Preview HUD layout";

   public HudLayoutPreviewScreen(JsonObject layout) {
      super(Component.literal("HUD layout preview"));
      this.layout = layout.deepCopy();
   }

   @Override
   protected void init() {
      if (this.preview == null) this.preview = ClientContext.getHudEditor().beginLayoutPreview(this.layout);
      int width = Math.max(1, Math.min(100, (this.width - 24) / 2));
      this.addRenderableWidget(Button.builder(Component.literal("Keep layout"), button -> {
         boolean committed = this.preview.commit();
         if (committed) this.minecraft.setScreen(null);
         else this.status = "Another layout replaced this preview. Close to continue.";
      }).bounds(this.width / 2 - width - 2, 30, width, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
         .bounds(this.width / 2 + 2, 30, width, 20).build());
   }

   @Override
   public void removed() {
      try {
         if (this.preview != null) this.preview.cancel();
      } finally {
         super.removed();
      }
   }

   @Override
   public void render(GuiGraphics graphics, int x, int y, float partialTick) {
      graphics.fill(0, 0, this.width, 55, 0xb0080c12);
      graphics.drawString(this.font, this.font.plainSubstrByWidth(this.status, Math.max(0, this.width - 16)), 8, 10, 0xffffffff);
      super.render(graphics, x, y, partialTick);
   }

   @Override
   public boolean isPauseScreen() { return false; }
}
