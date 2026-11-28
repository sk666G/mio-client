package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FontNativeImageTest {
   @Test
   void directConversionMatchesPngDecoderForArgbPixels() throws Exception {
      int[] pixels = {0, 0x00123456, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00,
         0xFF0000FF, 0x804080C0, 0x01020304, 0xC0ABCDEF, 0xFF112233};
      BufferedImage image = new BufferedImage(5, 2, BufferedImage.TYPE_INT_ARGB);
      image.setRGB(0, 0, 5, 2, pixels, 0, 5);
      ByteArrayOutputStream png = new ByteArrayOutputStream();
      assertTrue(ImageIO.write(image, "png", png));
      try (NativeImage legacy = NativeImage.read(png.toByteArray());
           NativeImage direct = FontAtlas.toNativeImage(image)) {
         assertEquals(NativeImage.Format.RGBA, direct.format());
         assertEquals(5, direct.getWidth());
         assertEquals(2, direct.getHeight());
         for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 5; x++) {
               assertEquals(pixels[y * 5 + x], direct.getPixel(x, y));
               assertEquals(legacy.getPixel(x, y), direct.getPixel(x, y));
            }
         }
      }
   }
}
