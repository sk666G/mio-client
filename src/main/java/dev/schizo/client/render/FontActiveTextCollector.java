/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.client.FontsModule;

import dev.schizo.mixin.GuiGraphicsAccessor;
import java.util.function.Consumer;
import java.util.function.IntToDoubleFunction;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphics.HoveredTextEffects;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public class FontActiveTextCollector implements Consumer<Style>, ActiveTextCollector {
   public static boolean customChatTextEnabled;
   private Parameters parameters;
   private final GuiGraphicsAccessor graphicsAccessor;
   private final HoveredTextEffects hoveredTextEffects;
   private final Consumer<Style> styleConsumer;

   @Override
   public void accept(TextAlignment alignment, int x, int y, Parameters parameters, FormattedCharSequence text) {
      boolean checkHoveredStyle = this.hoveredTextEffects.allowCursorChanges
         || this.hoveredTextEffects.allowTooltip
         || this.styleConsumer != null;
      float width = FontRenderer.INSTANCE.getWidth(text);
      float left = alignedX(alignment, x, width);
      float height = FontRenderer.INSTANCE
         .getHeight();
      if (checkHoveredStyle) {
         float mouseX = this.graphicsAccessor.schizo$mouseX();
         float mouseY = this.graphicsAccessor.schizo$mouseY();
         visitHoveredStyle(
            text, mouseX, mouseY, left, y, width, height, parameters, this
         );
      }

      if (ARGB.as8BitChannel(parameters.opacity()) != 0) {
         int color = ARGB.color(parameters.opacity(), -1);
         fontsModule().enqueueText(
            parameters.pose(), text, left, y, color, true
         );
      }
   }

   @Override
   public void defaultParameters(Parameters parameters) {
      this.parameters = parameters;
   }

   @Override
   public Parameters defaultParameters() {
      return this.parameters;
   }

   @Override
   public void acceptScrolling(Component text, int centerX, int left, int right, int top, int bottom, Parameters parameters) {
      int width = (int)FontRenderer.INSTANCE
         .getWidth(text.getVisualOrderText());
      int height = FontRenderer.INSTANCE
         .getHeight();
      this.defaultScrollingHelper(text, centerX, left, right, top, bottom, width, height, parameters);
   }

   public FontActiveTextCollector(Parameters parameters, GuiGraphics graphics, HoveredTextEffects hoveredTextEffects, Consumer<Style> styleConsumer) {
      this.parameters = parameters;
      this.graphicsAccessor = (GuiGraphicsAccessor)graphics;
      this.hoveredTextEffects = hoveredTextEffects;
      this.styleConsumer = styleConsumer;
   }

   private static FontsModule fontsModule() {
      return ClientContext.getModule(FontsModule.class);
   }

   static void visitHoveredStyle(
      FormattedCharSequence text, float mouseX, float mouseY, float left, float top, float width, float height, Parameters parameters, Consumer<Style> styleConsumer
   ) {
      ScreenRectangle scissor = parameters.scissor();
      if (scissor != null && (mouseX < scissor.left() || mouseX >= scissor.right() || mouseY < scissor.top() || mouseY >= scissor.bottom())) {
         return;
      }
      Vector2f localMouse = parameters.pose().invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
      if (!containsPoint(localMouse.x(), localMouse.y(), left, top, left + width, top + height)) {
         return;
      }
      float relativeX = localMouse.x() - left;
      float relativeY = localMouse.y() - top;
      if (relativeY < 0.0F || relativeY >= height) {
         return;
      }
      visitGlyphStyleAt(text, relativeX, FontRenderer.INSTANCE.getRenderer()::getCharacterWidth, styleConsumer);
   }

   // Use the same unrounded advances as the formatted glyph renderer.
   static void visitGlyphStyleAt(FormattedCharSequence text, float relativeX,
      IntToDoubleFunction glyphAdvance, Consumer<Style> styleConsumer) {
      double[] advance = {0.0D};
      text.accept((index, style, codePoint) -> {
         double next = advance[0] + glyphAdvance.applyAsDouble(codePoint);
         if (relativeX >= advance[0] && relativeX < next) {
            styleConsumer.accept(style);
            return false;
         }
         advance[0] = next;
         return true;
      });
   }

   private static float alignedX(
      TextAlignment alignment, float x, float width
   ) {
      return switch (alignment) {
         case LEFT -> x;
         case CENTER -> x - width / 2.0F;
         case RIGHT -> x - width;
      };
   }

   private static boolean containsPoint(
      float x, float y, float left, float top, float right, float bottom
   ) {
      return x >= left && x < right && y >= top && y < bottom;
   }

   @Override
   public void accept(Style style) {
      if (this.hoveredTextEffects.allowTooltip && style.getHoverEvent() != null) {
         this.graphicsAccessor.schizo$hoverStyle(style);
      }

      if (this.hoveredTextEffects.allowCursorChanges && style.getClickEvent() != null) {
         this.graphicsAccessor.schizo$clickStyle(style);
      }

      if (this.styleConsumer != null) {
         this.styleConsumer.accept(style);
      }
   }

   public static class ClickableStyleFinder implements ActiveTextCollector {
      private static final Parameters DEFAULT_PARAMETERS;
      private final float mouseX;
      private final float mouseY;
      private final boolean includeInsertions;
      private Parameters parameters = DEFAULT_PARAMETERS;
      private Style result;
      private final Consumer<Style> clickableStyleConsumer;

      public ClickableStyleFinder(double mouseX, double mouseY, boolean includeInsertions) {
         this.includeInsertions = includeInsertions;
         this.clickableStyleConsumer = style -> {
            if (style.getClickEvent() != null
               || this.includeInsertions && style.getInsertion() != null) {
               this.result = style;
            }
         };
         this.mouseX = (float)mouseX;
         this.mouseY = (float)mouseY;
      }

      @Override
      public Parameters defaultParameters() {
         return this.parameters;
      }

      @Override
      public void defaultParameters(Parameters parameters) {
         this.parameters = parameters;
      }

      @Override
      public void accept(TextAlignment alignment, int x, int y, Parameters parameters, FormattedCharSequence text) {
         float width = FontRenderer.INSTANCE.getWidth(text);
         float left = FontActiveTextCollector.alignedX(alignment, x, width);
         FontActiveTextCollector.visitHoveredStyle(
            text,
            this.mouseX,
            this.mouseY,
            left,
            y,
            width,
            FontRenderer.INSTANCE
               .getHeight(),
            parameters,
            this.clickableStyleConsumer
         );
      }

      @Override
      public void acceptScrolling(Component text, int centerX, int left, int right, int top, int bottom, Parameters parameters) {
         int width = (int)FontRenderer.INSTANCE
            .getWidth(text);
         int height = FontRenderer.INSTANCE
            .getHeight();
         this.defaultScrollingHelper(text, centerX, left, right, top, bottom, width, height, parameters);
      }

      public Style result() {
         return this.result;
      }

      static {
         DEFAULT_PARAMETERS = new Parameters(new Matrix3x2f());
      }

   }
}
