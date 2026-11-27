/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ServerStateManager;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.ClampedInterpolatedFloat;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.Minecraft;

public class LagOMeterModule extends HudWidgetModule {
   private final Setting<Boolean> foliaSetting;
   private final Setting<Color> colorSetting;
   private final ClampedInterpolatedFloat visibilityAnimation;

   private WelcomerModule getWelcomerModule() {
      Class<WelcomerModule> welcomerType = WelcomerModule.class;
      ModuleRegistry registry = ClientContext.moduleRegistry;
      return (WelcomerModule)registry.getModule(welcomerType);
   }

   private String formatUnresponsiveDuration() {
      Boolean useEntityPackets = this.foliaSetting.getValue();
      ServerStateManager.PacketActivity packetKind = useEntityPackets || RotationManager.is2b2tMode()
         ? ServerStateManager.PacketActivity.ENTITY
         : ServerStateManager.PacketActivity.ANY;
      long nowMillis = System.currentTimeMillis();
      ServerStateManager packetTracker = ClientContext.serverStateManager;
      float elapsedMillis = (float)(nowMillis - packetTracker.getLastPacketTime(packetKind));
      float elapsedSeconds = elapsedMillis / 1000.0F;
      if (elapsedSeconds >= 3600.0F) {
         return "more than an hour.";
      } else {
         return "%.1fs".formatted(elapsedSeconds);
      }
   }

   @Override
   public float[] calculateDimensions() {
      String warningText = this.getWarningText();
      if (warningText != null) {
         WelcomerModule welcomer = this.getWelcomerModule();
         if (!welcomer.shouldHideWidget(this)) {
            float width = FontRenderer.INSTANCE.getWidth(warningText);
            float height = FontRenderer.INSTANCE.getHeight() + 1;
            return this.dimensions(width, height);
         }
      }
      return this.dimensions(0.0F, 0.0F);
   }

   @Override
   public void renderContent(PoseStack poses) {
      String warningText = this.getWarningText();
      ClampedInterpolatedFloat animationReceiver = this.visibilityAnimation;
      boolean visible = warningText != null;
      animationReceiver.setTarget(visible);
      if (warningText != null) {
         WelcomerModule welcomer = this.getWelcomerModule();
         if (!welcomer.shouldHideWidget(this)) {
            float visibility = this.visibilityAnimation.updateAndGet();
            FontRenderer font = FontRenderer.INSTANCE;
            float slideOffsetY = (1.0F - visibility)
               * -FontRenderer.INSTANCE
                  .getHeight();
            Setting<Color> colorOption = this.colorSetting;
            font.drawWithShadow(
               poses, warningText, 0.0F, slideOffsetY, colorOption.getValue()
            );
            return;
         }
      }
   }

   private String getWarningText() {
      if (this.isWorldUnavailable()) {
         return this.isInHudEditor()
            ? "Server is not responding for 2.5s"
            : null;
      }

      Boolean useEntityPackets = this.foliaSetting.getValue();
      ServerStateManager.PacketActivity packetKind = useEntityPackets || RotationManager.is2b2tMode()
         ? ServerStateManager.PacketActivity.ENTITY
         : ServerStateManager.PacketActivity.ANY;
      long nowMillis = System.currentTimeMillis();
      ServerStateManager packetTracker = ClientContext.serverStateManager;
      long elapsedMillis = nowMillis - packetTracker.getLastPacketTime(packetKind);
      long clampedAgeMillis = Math.max(elapsedMillis, 0L);
      boolean remoteServerUnresponsive = false;
      if (clampedAgeMillis > 1000L) {
         Minecraft client = ClientAccess.schizoMinecraft;
         remoteServerUnresponsive = !client.isLocalServer();
      }
      if (!remoteServerUnresponsive && !this.isInHudEditor()) {
         return null;
      }
      String durationText = this.formatUnresponsiveDuration();
      return "Server is not responding for " + durationText;
   }

   private static String[] createAliases() {
      String[] aliasArray = new String[2];
      aliasArray[0] = "lag";
      aliasArray[1] = "lagometer";
      return aliasArray;
   }

   public LagOMeterModule() {
      super("Lag'O'Meter", createAliases());
      boolean defaultFolia = false;
      this.foliaSetting = this.add(new BooleanSetting("Folia", defaultFolia));
      this.colorSetting = this.add(new ColorSetting("Color", Color.gray));
      this.visibilityAnimation = new ClampedInterpolatedFloat(1.0F);
      ResizableHudWidgetRenderer renderer = new ResizableHudWidgetRenderer(this);
      renderer.enableHudScale();
      this.setRenderer(renderer);
   }
}
