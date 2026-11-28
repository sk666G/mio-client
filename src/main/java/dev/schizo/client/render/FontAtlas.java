/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.schizo.client.ClientAccess;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharIterator;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import net.minecraft.resources.Identifier;

public final class FontAtlas {
   public static final Identifier ATLAS_TEXTURE = Identifier.fromNamespaceAndPath("schizo", "textures/font.png");
   public static final String ATLAS_LABEL = ATLAS_TEXTURE.toLanguageKey();
   public static final int LAYOUT_VALUE = 4;
   public static final int SECONDARY_LAYOUT_VALUE = 1;
   public static final float LAYOUT_SCALE = 0.25F;
   private final Char2ObjectOpenHashMap<Glyph> glyphs = new Char2ObjectOpenHashMap<>();
   private final Font font;
   private int fontHeight = -1;
   private int atlasSize = -1;

   public FontAtlas(Font font) {
      this.font = font;
   }

   public void release() {
      this.glyphs.clear();
      ClientAccess.schizoMinecraft.getTextureManager().release(ATLAS_TEXTURE);
   }

   public void buildAtlas(Collection<Character> characters) {
      buildAtlas(characters, characters.isEmpty() ? 2 : align(this.calculateAtlasSize(characters), 2));
   }

   // Packing accepts an initial estimate independently of how it was calculated.
   void buildAtlas(Collection<Character> characters, int initialSize) {
      if (initialSize < 2 || initialSize % 2 != 0) {
         throw new IllegalArgumentException("Atlas estimate must be positive and mip-aligned");
      }
      this.glyphs.clear();
      this.fontHeight = -1;
      if (characters.isEmpty()) {
         this.fontHeight = 0;
         this.atlasSize = 2;
         return;
      }
      this.atlasSize = initialSize;
      BufferedImage metricsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = metricsImage.createGraphics();
      try {
         graphics.setFont(this.font);
         configureGraphics(graphics);
         FontMetrics metrics = graphics.getFontMetrics();
         int rowHeight = 0;
         int cursorX = 0;
         int cursorY = 0;
         int requiredSize = this.atlasSize;
         for (char character : characters) {
            Rectangle2D bounds = metrics.getStringBounds(Character.toString(character), graphics);
            int width = align(bounds.getBounds().width + 8, 2);
            int height = align(bounds.getBounds().height, 2);
            if (cursorX + width >= this.atlasSize) {
               cursorX = 0;
               cursorY += rowHeight + 2;
               rowHeight = 0;
            }
            requiredSize = Math.max(requiredSize, Math.max(cursorX + width, cursorY + height));
            Uv minUv = new Uv((float)cursorX / this.atlasSize, (float)cursorY / this.atlasSize);
            Uv maxUv = new Uv((float)(cursorX + width) / this.atlasSize, (float)(cursorY + height) / this.atlasSize);
            this.glyphs.put(character, new Glyph(cursorX, cursorY, width, height, minUv, maxUv));
            this.fontHeight = Math.max(this.fontHeight, height);
            rowHeight = Math.max(rowHeight, height);
            cursorX += width;
         }
         // The estimate uses different metrics from packing and can understate
         // the last row's extent. Preserve positions, grow the square if needed,
         // and normalize every UV against the final texture dimensions.
         if (requiredSize > this.atlasSize) {
            this.atlasSize = align(requiredSize, 2);
            this.glyphs.replaceAll((character, glyph) -> new Glyph(
               glyph.x, glyph.y, glyph.width, glyph.height,
               new Uv((float)glyph.x / this.atlasSize, (float)glyph.y / this.atlasSize),
               new Uv((float)(glyph.x + glyph.width) / this.atlasSize,
                  (float)(glyph.y + glyph.height) / this.atlasSize)
            ));
         }
         if ("Dialog".equals(this.font.getFamily())) {
            this.fontHeight -= 2;
         }
      } finally {
         graphics.dispose();
         metricsImage.flush();
      }
   }

   private int calculateAtlasSize(Collection<Character> characters) {
      double maxWidth = -1.0D;
      double maxHeight = -1.0D;
      FontRenderContext context = new FontRenderContext(new AffineTransform(), true, true);
      for (char character : characters) {
         Rectangle2D bounds = this.font.getStringBounds(Character.toString(character), context);
         maxWidth = Math.max(maxWidth, bounds.getWidth());
         maxHeight = Math.max(maxHeight, bounds.getHeight());
      }
      maxWidth += 8.0D;
      maxHeight += 4.0D;
      double width = Math.ceil(Math.sqrt(maxWidth * maxWidth * characters.size()) / maxWidth);
      double height = Math.ceil(Math.sqrt(maxHeight * maxHeight * characters.size()) / maxHeight);
      return (int)Math.ceil(Math.max(width, height) * Math.max(maxWidth, maxHeight)) + 1;
   }

   private BufferedImage createMipLevel(int mipLevel) {
      int scale = 1 << mipLevel;
      int size = Math.max(1, this.atlasSize / scale);
      BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = image.createGraphics();
      try {
         Font mipFont = mipLevel == 0 ? this.font : this.font.deriveFont(this.font.getSize2D() / scale);
         graphics.setFont(mipFont);
         graphics.setColor(Color.WHITE);
         configureGraphics(graphics);
         FontMetrics metrics = graphics.getFontMetrics();
         int padding = Math.max(1, 2 / scale);
         CharIterator characters = this.glyphs.keySet().iterator();
         while (characters.hasNext()) {
            char character = characters.nextChar();
            Glyph glyph = this.glyphs.get(character);
            graphics.drawString(Character.toString(character), glyph.x / scale + padding, glyph.y / scale + metrics.getAscent());
         }
      } catch (RuntimeException | Error failure) {
         image.flush();
         throw failure;
      } finally {
         graphics.dispose();
      }
      return image;
   }

   private static void configureGraphics(Graphics2D graphics) {
      graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
   }

   private static int align(int value, int alignment) {
      return (value + alignment - 1) / alignment * alignment;
   }

   public void uploadTexture() {
      NativeImage[] mipImages = new NativeImage[2];
      try {
         for (int level = 0; level < mipImages.length; level++) {
            mipImages[level] = toNativeImage(this.createMipLevel(level));
         }
         ClientAccess.schizoMinecraft.getTextureManager().register(ATLAS_TEXTURE, new MipmapFontTexture(() -> ATLAS_LABEL, mipImages));
      } catch (RuntimeException | Error failure) {
         for (NativeImage image : mipImages) {
            if (image != null) {
               try {
                  image.close();
               } catch (RuntimeException | Error cleanupFailure) {
                  if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
               }
            }
         }
         throw failure;
      }
   }

   static NativeImage toNativeImage(BufferedImage image) {
      NativeImage nativeImage = null;
      try {
         int width = image.getWidth();
         int height = image.getHeight();
         nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);
         int[] row = new int[width];
         for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
               // NativeImage.setPixel accepts ARGB, as does BufferedImage.getRGB.
               nativeImage.setPixel(x, y, row[x]);
            }
         }
         return nativeImage;
      } catch (RuntimeException | Error failure) {
         if (nativeImage != null) {
            try {
               nativeImage.close();
            } catch (RuntimeException | Error cleanupFailure) {
               if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
            }
         }
         throw failure;
      } finally {
         image.flush();
      }
   }

   public float drawGlyph(PoseStack poses, VertexConsumer consumer, char character, float x, float y, int argb) {
      return this.drawGlyph(poses, consumer, (int)character, x, y, argb);
   }

   public float drawGlyph(PoseStack poses, VertexConsumer consumer, int codePoint, float x, float y, int argb) {
      Glyph glyph = this.resolveGlyph(codePoint);
      if (glyph == null) {
         return 0.0F;
      }
      float z = FontRenderer.getZOffset();
      consumer.addVertex(poses.last(), x, y + glyph.height, z).setColor(argb).setUv(glyph.minimumUv.u, glyph.maximumUv.v);
      consumer.addVertex(poses.last(), x + glyph.width, y + glyph.height, z).setColor(argb).setUv(glyph.maximumUv.u, glyph.maximumUv.v);
      consumer.addVertex(poses.last(), x + glyph.width, y, z).setColor(argb).setUv(glyph.maximumUv.u, glyph.minimumUv.v);
      consumer.addVertex(poses.last(), x, y, z).setColor(argb).setUv(glyph.minimumUv.u, glyph.minimumUv.v);
      return glyph.width - 8.0F;
   }

   public float getGlyphWidth(char character) {
      return this.getGlyphWidth((int)character);
   }

   public float getGlyphWidth(int codePoint) {
      Glyph glyph = this.resolveGlyph(codePoint);
      return glyph == null ? 0.0F : glyph.width - 8.0F;
   }

   Glyph resolveGlyph(int codePoint) {
      Glyph glyph = Character.isBmpCodePoint(codePoint) && !Character.isSurrogate((char)codePoint)
         ? this.glyphs.get((char)codePoint) : null;
      return glyph != null ? glyph : this.glyphs.get('?');
   }

   public int getHeight() {
      return this.fontHeight;
   }

   int atlasSize() {
      return this.atlasSize;
   }

   record Uv(float u, float v) {}
   record Glyph(int x, int y, int width, int height, Uv minimumUv, Uv maximumUv) {}
}
