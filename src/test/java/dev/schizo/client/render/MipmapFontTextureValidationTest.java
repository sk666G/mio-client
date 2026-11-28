package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

class MipmapFontTextureValidationTest {
   @Test
   void validChainIsSnapshottedWithoutCopyingNativeImages() {
      try (NativeImage base = new NativeImage(7, 3, false);
           NativeImage second = new NativeImage(3, 1, false);
           NativeImage third = new NativeImage(1, 1, false)) {
         NativeImage[] input = {base, second, third};
         NativeImage[] validated = MipmapFontTexture.validateMipImages(input);
         assertNotSame(input, validated);
         assertArrayEquals(input, validated);
         input[0] = null;
         assertSame(base, validated[0]);
      }
   }

   @Test
   void rejectsInvalidDimensionsAndDuplicateOwnershipBeforeGpuAllocation() {
      try (NativeImage base = new NativeImage(8, 4, false);
           NativeImage wrong = new NativeImage(4, 3, false);
           NativeImage pixel = new NativeImage(1, 1, false)) {
         assertThrows(IllegalArgumentException.class,
            () -> MipmapFontTexture.validateMipImages(new NativeImage[]{base, wrong}));
         assertThrows(IllegalArgumentException.class,
            () -> MipmapFontTexture.validateMipImages(new NativeImage[]{base, base}));
         assertThrows(IllegalArgumentException.class,
            () -> MipmapFontTexture.validateMipImages(new NativeImage[]{pixel, pixel}));
         base.setPixel(0, 0, 0xFF112233);
         assertEquals(0xFF112233, base.getPixel(0, 0));
      }
   }

   @Test
   void rejectsMissingMipImages() {
      assertThrows(NullPointerException.class, () -> MipmapFontTexture.validateMipImages(null));
      assertThrows(IllegalArgumentException.class,
         () -> MipmapFontTexture.validateMipImages(new NativeImage[0]));
      assertThrows(NullPointerException.class,
         () -> MipmapFontTexture.validateMipImages(new NativeImage[]{null}));
   }
}
