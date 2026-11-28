/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui;

import dev.schizo.client.render.GuiScaleUtils;

import dev.schizo.client.ClientAccess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import java.util.ArrayDeque;
import java.util.Deque;
import org.joml.Vector4i;

public final class ScissorStack implements ClientAccess {
   private static final Deque<ScissorRegion> regions = new ArrayDeque<>();

   private ScissorStack() {
   }

   public static void pop() {
      regions.removeLast();
      getScissorState().disable();
      if (!regions.isEmpty()) {
         ScissorStack.ScissorRegion previousRegion = regions.getLast();
         Vector4i framebufferBounds = previousRegion.framebufferBounds();
         enableFramebufferScissor(framebufferBounds.x, framebufferBounds.y, framebufferBounds.z, framebufferBounds.w);
      }
   }

   public static void push(
      int x, int y, int width, int height
   ) {
      ScissorStack.ScissorRegion region = ScissorStack.ScissorRegion.create(
         x, y, Math.max(0, width), Math.max(0, height)
      );
      Vector4i framebufferBounds = region.framebufferBounds();
      enableFramebufferScissor(framebufferBounds.x, framebufferBounds.y, framebufferBounds.z, framebufferBounds.w);
      regions.addLast(region);
   }

   private static ScissorState getScissorState() {
      return RenderSystem.getScissorStateForRenderTypeDraws();
   }

   public static void pushIntersection(
      int x, int y, int width, int height
   ) {
      ScissorStack.ScissorRegion previousRegion = regions.peekLast();
      ScissorBounds requested = new ScissorBounds(x, y, width, height);
      if (previousRegion != null) {
         Vector4i previousBounds = previousRegion.guiBounds();
         requested = requested.intersect(new ScissorBounds(
            previousBounds.x, previousBounds.y, previousBounds.z, previousBounds.w
         ));
      }
      push(requested.x(), requested.y(), requested.width(), requested.height());
   }

   public static void enableFramebufferScissor(
      int x, int y, int width, int height
   ) {
      getScissorState()
         .enable(x, y, Math.max(width, 0), Math.max(height, 0));
   }

   record ScissorRegion(
      Vector4i guiBounds,
      Vector4i framebufferBounds
   ) {

      public static ScissorStack.ScissorRegion create(
         int x, int y, int width, int height
      ) {
         double framebufferScale = ClientAccess.schizoMinecraft.getWindow().getGuiScale() * GuiScaleUtils.getScaleMultiplier();
         var window=ClientAccess.schizoMinecraft.getWindow();
         var bounds=new ScissorBounds(x,y,width,height).toFramebuffer(framebufferScale,window.getWidth(),window.getHeight());
         Vector4i guiBounds = new Vector4i(x,y,width,height);
         Vector4i framebufferBounds = new Vector4i(bounds.x(),bounds.y(),bounds.width(),bounds.height());
         return new ScissorStack.ScissorRegion(guiBounds, framebufferBounds);
      }
   }
}
