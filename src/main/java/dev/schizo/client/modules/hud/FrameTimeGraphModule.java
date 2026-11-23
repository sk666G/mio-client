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
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.ClientRenderTypes;
import dev.schizo.client.render.MeshBuffer;
import dev.schizo.client.render.MeshDrawContext;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.render.VertexGeometry;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.FrameTiming;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.Arrays;

public class FrameTimeGraphModule extends HudWidgetModule {
   private static final float DEFAULT_FRAME_TIME_MILLIS = 1000.0F / 60.0F;

   private final Setting<Float> ceilingSetting;
   private final Setting<Integer> heightSetting;
   private final Setting<Integer> widthSetting;
   private float[] frameTimeSamples;
   private int sampleCount;
   private int nextSampleIndex;
   private long lastFrameSequence = -1L;

   @Override
   public void onEnable() {
      this.ensureSampleCapacity(this.widthSetting.getValue());

      FrameTiming frameTiming = ClientContext.frameTiming;
      float initialFrameTime = frameTiming.getFrameTimeMillis();
      if (!(initialFrameTime > 0.0F)) {
         initialFrameTime = DEFAULT_FRAME_TIME_MILLIS;
      }

      Arrays.fill(this.frameTimeSamples, initialFrameTime);
      this.sampleCount = this.frameTimeSamples.length;
      this.nextSampleIndex = 0;
      this.lastFrameSequence = frameTiming.getFrameSequence();
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

      FrameTiming frameTiming = ClientContext.frameTiming;
      long currentFrameSequence = frameTiming.getFrameSequence();
      if (currentFrameSequence != this.lastFrameSequence) {
         this.appendSample(frameTiming.getFrameTimeMillis());
         this.lastFrameSequence = currentFrameSequence;
      }

      if (this.sampleCount < 2) {
         return;
      }

      Color graphBaseColor = this.getHudColor(this.getRenderer().getY() + this.heightSetting.getValue() / 2.0F);
      int graphColor = ColorUtils.withAlpha(graphBaseColor, 255).hashCode();

      MeshBuffer buffer = MeshDrawContext.getBuffer();
      buffer.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
      for (int sampleIndex = 1; sampleIndex < this.sampleCount; sampleIndex++) {
         float previousY = this.getSampleY(this.getSample(sampleIndex - 1));
         float currentY = this.getSampleY(this.getSample(sampleIndex));
         VertexGeometry.appendLine(
            buffer, poses.last(), sampleIndex - 1, previousY, 0.0F, sampleIndex, currentY, 0.0F,
            1.0F, graphColor, graphColor
         );
      }
      buffer.draw(ClientRenderTypes.worldLines());
   }

   private float getSample(int chronologicalIndex) {
      int oldestSampleIndex = Math.floorMod(this.nextSampleIndex - this.sampleCount, this.frameTimeSamples.length);
      return this.frameTimeSamples[(oldestSampleIndex + chronologicalIndex) % this.frameTimeSamples.length];
   }

   private void appendSample(float frameTimeMillis) {
      this.frameTimeSamples[this.nextSampleIndex] = Math.max(0.0F, frameTimeMillis);
      this.nextSampleIndex = (this.nextSampleIndex + 1) % this.frameTimeSamples.length;
      if (this.sampleCount < this.frameTimeSamples.length) {
         this.sampleCount++;
      }
   }

   private void ensureSampleCapacity(int capacity) {
      int requestedCapacity = Math.max(2, capacity);
      if (this.frameTimeSamples != null && this.frameTimeSamples.length == requestedCapacity) {
         return;
      }

      float[] resizedSamples = new float[requestedCapacity];
      if (this.frameTimeSamples != null && this.sampleCount > 0) {
         int retainedSamples = Math.min(this.sampleCount, requestedCapacity);
         int oldestSampleIndex = Math.floorMod(this.nextSampleIndex - this.sampleCount, this.frameTimeSamples.length);
         int firstRetainedIndex = this.sampleCount - retainedSamples;
         for (int index = 0; index < retainedSamples; index++) {
            resizedSamples[index] = this.frameTimeSamples[
               (oldestSampleIndex + firstRetainedIndex + index) % this.frameTimeSamples.length
            ];
         }
         this.sampleCount = retainedSamples;
         this.nextSampleIndex = retainedSamples % requestedCapacity;
      } else {
         this.sampleCount = 0;
         this.nextSampleIndex = 0;
      }

      this.frameTimeSamples = resizedSamples;
   }

   private float getSampleY(float sample) {
      int height = this.heightSetting.getValue();
      float ceiling = this.ceilingSetting.getValue();
      float clippedSample = Math.min(sample, ceiling);
      return 1.0F + (height - 2) * (1.0F - clippedSample / ceiling);
   }

   public FrameTimeGraphModule() {
      super("Frame Time Graph", new String[]{"frametimegraph", "ftgraph"});

      this.ceilingSetting = this.add(new NumberSetting<>(
         "MaxFrameTime",
         50.0F,
         16.0F,
         100.0F
      ));
      this.heightSetting = this.add(new NumberSetting<>(
         "Height",
         32,
         10,
         80
      ));
      this.widthSetting = this.add(new NumberSetting<>(
         "Width",
         100,
         50,
         200
      ));
      this.frameTimeSamples = new float[this.widthSetting.getValue()];
      this.setRenderer(new ResizableHudWidgetRenderer(this));
   }
}
