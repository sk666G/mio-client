/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.schizo.client.ClientAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public final class GuiProjectionUtils implements ClientAccess {
   public static final CachedOrthoProjectionMatrixBuffer GUI_PROJECTION_CACHE =
      new CachedOrthoProjectionMatrixBuffer("gui_mio", 1000.0F, 11000.0F, true);
   private static GpuBufferSlice savedProjectionBuffer;
   private static ProjectionType savedProjectionType;
   private static Matrix4f savedModelView;
   private static int projectionDepth;

   /** One successful acquisition, released independently of the active screen. */
   public static ProjectionScope openGuiProjection() {
      ProjectionScope scope = new ProjectionScope();
      beginGuiProjection();
      return scope;
   }

   public static final class ProjectionScope implements AutoCloseable {
      private boolean closed;

      private ProjectionScope() {
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            restoreProjection();
         }
      }
   }

   public static void restoreProjection() {
      if (projectionDepth == 0 || --projectionDepth != 0) {
         return;
      }
      GpuBufferSlice buffer = savedProjectionBuffer;
      ProjectionType type = savedProjectionType;
      Matrix4f modelView = savedModelView;
      savedProjectionBuffer = null;
      savedProjectionType = null;
      savedModelView = null;
      restoreState(buffer, type, modelView);
   }

   private static void restoreState(GpuBufferSlice buffer, ProjectionType type, Matrix4f modelView) {
      try {
         RenderSystem.setProjectionMatrix(buffer, type);
      } catch (RuntimeException | Error failure) {
         try {
            RenderSystem.getModelViewMatrix().set(modelView);
         } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
         }
         throw failure;
      }
      RenderSystem.getModelViewMatrix().set(modelView);
   }

   public static void beginGuiProjection() {
      // All nested callers share the already-installed GUI projection.
      if (projectionDepth != 0) {
         projectionDepth++;
         return;
      }
      Window window = schizoMinecraft.getWindow();
      float guiWidth = (float)window.getWidth() / (float)window.getGuiScale();
      float guiHeight = (float)window.getHeight() / (float)window.getGuiScale();
      GpuBufferSlice previousBuffer = RenderSystem.getProjectionMatrixBuffer();
      ProjectionType previousType = RenderSystem.getProjectionType();
      Matrix4f modelView = RenderSystem.getModelViewMatrix();
      Matrix4f previousModelView = new Matrix4f(modelView);
      try {
         RenderSystem.setProjectionMatrix(
            GUI_PROJECTION_CACHE.getBuffer(guiWidth, guiHeight), ProjectionType.ORTHOGRAPHIC
         );
         modelView.identity().translate(0.0F, 0.0F, -11000.0F);
      } catch (RuntimeException | Error failure) {
         try {
            restoreState(previousBuffer, previousType, previousModelView);
         } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
         }
         throw failure;
      }
      savedProjectionBuffer = previousBuffer;
      savedProjectionType = previousType;
      savedModelView = previousModelView;
      projectionDepth = 1;
   }

   public static PoseStack toPoseStack(Matrix3x2fStack guiPose) {
      PoseStack poseStack = new PoseStack();
      Pose pose = poseStack.last();
      Matrix4f matrix = pose.pose();
      matrix.set(
         guiPose.m00(), guiPose.m01(), 0.0F, 0.0F,
         guiPose.m10(), guiPose.m11(), 0.0F, 0.0F,
         0.0F, 0.0F, 1.0F, 0.0F,
         guiPose.m20(), guiPose.m21(), 0.0F, 1.0F
      );
      return poseStack;
   }

   public static PoseStack toPoseStack(GuiGraphics graphics) {
      return toPoseStack(graphics.pose());
   }

   private GuiProjectionUtils() {
   }
}
