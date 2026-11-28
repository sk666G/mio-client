package dev.schizo.client.gui.screens;

import com.google.gson.JsonObject;
import dev.schizo.client.ClientContext;
import dev.schizo.client.ClientAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudLayoutPreviewScreen extends Screen {
   private final JsonObject layout;
   private HudEditorScreen.LayoutPreview preview;
   private boolean attempted;
   private StudioViewportLayouts.HudPreview geometry;
   private String status = "Preview HUD layout";

   public HudLayoutPreviewScreen(JsonObject layout) {
      super(Component.literal("HUD layout preview"));
      this.layout = layout.deepCopy();
   }

   @Override
   protected void init() {
      if (!this.attempted) {
         this.attempted = true;
         try {
            this.preview = ClientContext.getHudEditor().beginLayoutPreview(this.layout);
         } catch (IllegalArgumentException invalidLayout) {
            this.status=invalidLayout.getSuppressed().length==0?"Invalid HUD layout.":"Invalid layout; rollback incomplete.";
            if(invalidLayout.getSuppressed().length>0) ClientAccess.logger.warn("HUD preview rollback failed",invalidLayout);
         } catch(RuntimeException failure) {
            this.status="Preview failed; inspect HUD layout.";
            ClientAccess.logger.warn("HUD preview failed",failure);
         }
      }
      geometry=StudioViewportLayouts.hudPreview(width,height);
      if(!geometry.compact()) {
         Button keep=addRenderableWidget(Button.builder(Component.literal("Keep layout"),button->{
            boolean committed=preview.commit();
            if(committed) minecraft.setScreen(null);
            else {
               status="Another layout replaced this preview. Close to continue.";
               button.active=false;
            }
         }).bounds(geometry.left(),geometry.buttonY(),geometry.buttonWidth(),20).build());
         keep.active=preview!=null;
      }
      int cancelX=geometry.compact()?geometry.left():width/2+2;
      addRenderableWidget(Button.builder(Component.literal("Cancel"),button->onClose())
         .bounds(cancelX,geometry.buttonY(),geometry.buttonWidth(),20).build());
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
   public void renderBackground(GuiGraphics graphics, int x, int y, float partialTick) {
      // A layout preview needs the actual HUD to remain visible, without the menu blur.
   }

   @Override
   public void render(GuiGraphics graphics, int x, int y, float partialTick) {
      graphics.fill(0,0,width,geometry.stripBottom(),0xb0080c12);
      if(geometry.buttonY()>=font.lineHeight+8) {
         String message=geometry.compact()?"Resize to keep layout | Cancel to exit":status;
         graphics.drawString(font,font.plainSubstrByWidth(message,Math.max(0,width-16)),8,8,0xffffffff);
      }
      super.render(graphics, x, y, partialTick);
   }

   @Override
   public boolean isPauseScreen() { return false; }
}
