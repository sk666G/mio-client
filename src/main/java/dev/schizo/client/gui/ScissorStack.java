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

public final class ScissorStack implements ClientAccess {
   private static final ScissorScopeStack regions = new ScissorScopeStack(
      ScissorStack::capture, ScissorStack::apply, ScissorStack::toFramebuffer);

   private ScissorStack() {
   }

   public static void pop() {
      regions.pop();
   }

   public static void push(int x, int y, int width, int height) {
      regions.push(new ScissorBounds(x, y, width, height), false);
   }

   public static void pushIntersection(int x, int y, int width, int height) {
      regions.push(new ScissorBounds(x, y, width, height), true);
   }

   private static ScissorState getScissorState() {
      return RenderSystem.getScissorStateForRenderTypeDraws();
   }

   private static ScissorScopeStack.Snapshot capture() {
      ScissorState state = getScissorState();
      return new ScissorScopeStack.Snapshot(state.enabled(),
         new ScissorBounds(state.x(), state.y(), state.width(), state.height()));
   }

   private static void apply(ScissorScopeStack.Snapshot snapshot) {
      ScissorState state = getScissorState();
      ScissorBounds bounds = snapshot.bounds();
      if (snapshot.enabled()) {
         state.enable(bounds.x(), bounds.y(), bounds.width(), bounds.height());
      } else {
         state.disable();
      }
   }

   private static ScissorBounds toFramebuffer(ScissorBounds bounds) {
      var window = ClientAccess.schizoMinecraft.getWindow();
      double scale = window.getGuiScale() * GuiScaleUtils.getScaleMultiplier();
      return bounds.toFramebuffer(scale, window.getWidth(), window.getHeight());
   }

   public static void enableFramebufferScissor(int x, int y, int width, int height) {
      getScissorState().enable(x, y, Math.max(width, 0), Math.max(height, 0));
   }
}
