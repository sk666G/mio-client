package dev.schizo.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.render.GuiDrawUtils;
import java.awt.Color;

/** Low-cost docked control primitives; no textures, shaders or separate render targets. */
final class StudioControlStyle {
   private static final Color TRACK = new Color(58, 60, 69, 240);
   private static final Color THUMB = new Color(242, 243, 246);
   private StudioControlStyle() {}

   static void softRect(PoseStack pose, float x, float y, float w, float h, Color color) {
      if (w <= 0 || h <= 0) return;
      float inset = Math.min(2.0F, Math.min(w, h) / 3.0F);
      GuiDrawUtils.fillRect(pose, x + inset, y, x + w - inset, y + h, color);
      GuiDrawUtils.fillRect(pose, x, y + inset, x + inset, y + h - inset, color);
      GuiDrawUtils.fillRect(pose, x + w - inset, y + inset, x + w, y + h - inset, color);
   }

   static void toggle(PoseStack pose, float x, float y, float progress, Color accent) {
      float p = StudioControlGeometry.clampProgress(progress);
      softRect(pose, x, y, 25, 10, ColorUtils.blend(accent, TRACK, p));
      float thumb = StudioControlGeometry.thumbLeft(x + 2, 21, 6, p);
      softRect(pose, thumb, y + 2, 6, 6, THUMB);
   }

   static void slider(PoseStack pose, float x, float y, float width, float progress, Color accent) {
      if (width <= 0) return;
      float p = StudioControlGeometry.clampProgress(progress);
      softRect(pose, x, y, width, 2, TRACK);
      softRect(pose, x, y, width * p, 2, accent);
      float thumbWidth = Math.min(4, width);
      softRect(pose, StudioControlGeometry.thumbLeft(x, width, thumbWidth, p), y - 1, thumbWidth, 4, THUMB);
   }

   static String fitText(String text, float width) {
      FontRenderer font = FontRenderer.INSTANCE;
      if (width <= 0) return "";
      if (font.getWidth(text) <= width) return text;
      if (font.getWidth("...") > width) return "";
      int end = text.length();
      while (end > 0) {
         end = text.offsetByCodePoints(end, -1);
         String candidate = text.substring(0, end) + "...";
         if (font.getWidth(candidate) <= width) return candidate;
      }
      return "...";
   }
}
