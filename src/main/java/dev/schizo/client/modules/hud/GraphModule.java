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
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.render.MeshBuffer;
import dev.schizo.client.render.MeshDrawContext;
import dev.schizo.client.render.VertexGeometry;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.ClientRenderTypes;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.Arrays;
import net.minecraft.util.Mth;

public class GraphModule extends HudWidgetModule {
   private final Setting<Float> ceilSetting;
   private final Setting<Integer> heightSetting;
   private final Setting<Integer> widthSetting;
   private double[] speedSamples;
   private int sampleCount;
   private int nextSampleIndex;

   @Override
   public void onEnable() {
      this.ensureSampleCapacity(this.widthSetting.getValue());
      Arrays.fill(this.speedSamples, 0.0D);
      this.sampleCount = this.speedSamples.length;
      this.nextSampleIndex = 0;
   }

   @Override
   public float[] calculateDimensions() {
      float[] dimensions = new float[2];
      dimensions[0] = this.widthSetting.getValue();
      dimensions[1] = this.heightSetting.getValue();
      return dimensions;
   }

   @Override
   public void renderContent(PoseStack poses) {
      this.ensureSampleCapacity(this.widthSetting.getValue());
      if (this.sampleCount < 2) {
         return;
      }

      MeshBuffer buffer = MeshDrawContext.getBuffer();
      buffer.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
      for (int sampleIndex = 1; sampleIndex < this.sampleCount; sampleIndex++) {
         float previousY = this.getSampleY(this.getSample(sampleIndex - 1));
         float currentY = this.getSampleY(this.getSample(sampleIndex));
         VertexGeometry.appendLine(
            buffer, poses.last(), sampleIndex - 1, previousY, 0.0F, sampleIndex, currentY, 0.0F,
            1.0F, this.getSampleColor(previousY, sampleIndex - 1), this.getSampleColor(currentY, sampleIndex)
         );
      }
      buffer.draw(ClientRenderTypes.worldLines());
   }

   private double getSample(int chronologicalIndex) {
      int oldestSampleIndex = Math.floorMod(this.nextSampleIndex - this.sampleCount, this.speedSamples.length);
      return this.speedSamples[(oldestSampleIndex + chronologicalIndex) % this.speedSamples.length];
   }

   private void appendSample(double sample) {
      this.speedSamples[this.nextSampleIndex] = sample;
      this.nextSampleIndex = (this.nextSampleIndex + 1) % this.speedSamples.length;
      if (this.sampleCount < this.speedSamples.length) {
         this.sampleCount++;
      }
   }

   private void ensureSampleCapacity(int capacity) {
      int requestedCapacity = Math.max(2, capacity);
      if (this.speedSamples != null && this.speedSamples.length == requestedCapacity) {
         return;
      }

      double[] resizedSamples = new double[requestedCapacity];
      if (this.speedSamples != null && this.sampleCount > 0) {
         int retainedSamples = Math.min(this.sampleCount, requestedCapacity);
         int oldestSampleIndex = Math.floorMod(this.nextSampleIndex - this.sampleCount, this.speedSamples.length);
         int firstRetainedIndex = this.sampleCount - retainedSamples;
         for (int index = 0; index < retainedSamples; index++) {
            resizedSamples[index] = this.speedSamples[
               (oldestSampleIndex + firstRetainedIndex + index) % this.speedSamples.length
            ];
         }
         this.sampleCount = retainedSamples;
         this.nextSampleIndex = retainedSamples % requestedCapacity;
      } else {
         this.sampleCount = 0;
         this.nextSampleIndex = 0;
      }

      this.speedSamples = resizedSamples;
   }

   private float getSampleY(double sample) {
      int height = this.heightSetting.getValue();
      double clippedSample = Math.min(sample, this.ceilSetting.getValue());
      return 1.0F + (float)((height - 2) * (1.0D - clippedSample / this.ceilSetting.getValue()));
   }

   private int getSampleColor(float sampleY, int sampleIndex) {
      Color baseColor = this.getHudColor(sampleY + this.renderer.getY());
      return ColorUtils.withAlpha(baseColor, this.getSampleAlpha(sampleIndex)).hashCode();
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent ignoredEvent) {
      this.ensureSampleCapacity(this.widthSetting.getValue());
      this.appendSample(ClientContext.movementSpeedTracker.getDisplaySpeed());
   }

   private int getSampleAlpha(int sampleIndex) {
      if (sampleIndex <= 10) {
         float leadingAlpha = sampleIndex / 10.0F * 255.0F;
         return (int)Mth.clamp(leadingAlpha, 0.0F, 255.0F);
      }

      int width = this.widthSetting.getValue();
      if (sampleIndex >= width - 10) {
         float trailingPosition = sampleIndex - width + 10;
         float trailingAlpha = (1.0F - trailingPosition / 10.0F) * 255.0F;
         return (int)Mth.clamp(trailingAlpha, 0.0F, 255.0F);
      }

      return 255;
   }

   public GraphModule() {
      super("Graph", new String[0]);

      this.ceilSetting = this.add(new NumberSetting<>(
         "Ceil",
         26.0F,
         10.0F,
         100.0F
      ));
      this.heightSetting = this.add(new NumberSetting<>(
         "Height",
         25,
         10,
         30
      ));
      this.widthSetting = this.add(new NumberSetting<>(
         "Width",
         75,
         50,
         100
      ));
      this.speedSamples = new double[this.widthSetting.getValue()];
      this.setRenderer(new ResizableHudWidgetRenderer(this));
   }
}
