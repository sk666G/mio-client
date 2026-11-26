/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.ClientContext;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.media.MediaSnapshot;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;

import com.mojang.blaze3d.vertex.PoseStack;

public class MusicModule extends HudWidgetModule {
   private final Setting<Boolean> prefixSetting;
   private final Setting<String> prefixTextSetting;

   private boolean shouldSkipMediaRendering() {
      if (!this.isInHudEditor()) {
         return MediaSessionBridge.snapshot().playbackStatus() != MediaSnapshot.PLAYING;
      }

      return false;
   }

   @Override
   public void onEnable() {

   }

   @Override
   public float[] calculateDimensions() {

      if (!this.shouldSkipMediaRendering()) {

         Class<WelcomerModule> welcomerType = WelcomerModule.class;
         ModuleRegistry moduleRegistry = ClientContext.moduleRegistry;
         WelcomerModule welcomer = (WelcomerModule)moduleRegistry.getModule(welcomerType);
         if (!welcomer.shouldHideWidget(this)) {

            FontRenderer textRenderer = FontRenderer.INSTANCE;
            float width = textRenderer.getWidth(this.getMusicText());
            float height = FontRenderer.INSTANCE.getHeight();
            return this.dimensions(width, height);
         }
      }

      return this.dimensions(0.0F, 0.0F);
   }

   @Override
   public void renderContent(PoseStack poses) {

      if (this.shouldSkipMediaRendering()) {
         return;
      }

      Class<WelcomerModule> welcomerType = WelcomerModule.class;
      ModuleRegistry moduleRegistry = ClientContext.moduleRegistry;
      WelcomerModule welcomer = (WelcomerModule)moduleRegistry.getModule(welcomerType);
      if (welcomer.shouldHideWidget(this)) {
         return;
      }

      FontRenderer.INSTANCE.drawWithShadow(
         poses,
         this.getMusicText(),
         0.0F,
         0.0F,
         this.getHudColor(
            this.renderer
               .getY()
         )
      );
   }

   private String getMusicText() {
      MediaSnapshot snapshot = MediaSessionBridge.snapshot();
      String text = MediaSessionBridge.getDisplayText(snapshot);
      if (this.isInHudEditor() && snapshot.playbackStatus() != MediaSnapshot.PLAYING) {
         text = "HydrachFM - f3dot";
      }

      return MusicDisplayText.format(
         text,
         this.prefixSetting.getValue(),
         this.prefixTextSetting.getValue()
      );
   }

   public MusicModule() {
      super("Music", new String[0]);

      this.prefixSetting = this.add(new BooleanSetting("Prefix", false).markVisibilityDependency());

      this.prefixTextSetting = this.add(new StringSetting("Value", "♪").dependsOn(new Setting<?>[]{this.prefixSetting}));
      ResizableHudWidgetRenderer widget = new ResizableHudWidgetRenderer(this);
      widget.enableHudScale();
      this.setRenderer(widget);
   }

}
