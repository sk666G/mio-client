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
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class MipmapFontTexture extends AbstractTexture {

   private NativeImage[] mipImages;
   private final int mipLevelCount;
   private boolean closed;

   public MipmapFontTexture(Supplier<String> label, NativeImage[] mipImages) {
      this.mipImages = validateMipImages(mipImages);
      this.mipLevelCount = this.mipImages.length;
      try {
         this.createTexture(label);
         this.upload();
      } catch (RuntimeException | Error failure) {
         // The caller still owns the native images until construction succeeds.
         this.mipImages = null;
         try {
            super.close();
         } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
         }
         throw failure;
      }
   }

   // Validate and snapshot the array before allocating GPU resources. The caller
   // retains all image ownership until the constructor completes successfully.
   static NativeImage[] validateMipImages(NativeImage[] input) {
      NativeImage[] images = Objects.requireNonNull(input, "mip images").clone();
      if (images.length == 0) throw new IllegalArgumentException("Empty mip chain");
      NativeImage base = Objects.requireNonNull(images[0], "base image");
      int width = base.getWidth();
      int height = base.getHeight();
      if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid base dimensions");
      int maxLevels = Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(width, height));
      if (images.length > maxLevels) throw new IllegalArgumentException("Too many mip levels");
      var seen = new IdentityHashMap<NativeImage, Boolean>();
      for (int level = 0; level < images.length; level++) {
         NativeImage image = Objects.requireNonNull(images[level], "mip image");
         if (seen.put(image, Boolean.TRUE) != null) throw new IllegalArgumentException("Duplicate mip image");
         if (image.format() != NativeImage.Format.RGBA
            || image.getWidth() != Math.max(1, width >> level)
            || image.getHeight() != Math.max(1, height >> level)) {
            throw new IllegalArgumentException("Invalid mip image at level " + level);
         }
      }
      return images;
   }

   private void createTexture(Supplier<String> label) {
      GpuDevice device = RenderSystem.getDevice();
      NativeImage baseImage = this.mipImages[0];
      this.texture = device.createTexture(label, 5,
         TextureFormat.RGBA8, baseImage.getWidth(), baseImage.getHeight(), 1, this.mipLevelCount);
      this.sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
         FilterMode.LINEAR, FilterMode.LINEAR, true);
      this.textureView = device.createTextureView(this.texture, 0, this.mipLevelCount);
   }

   public void upload() {
      if (this.mipImages == null || this.texture == null) {
         return;
      }
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      for (int level = 0; level < this.mipImages.length; level++) {
         NativeImage image = this.mipImages[level];
         encoder.writeToTexture(this.texture, image, level, 0, 0, 0, image.getWidth(), image.getHeight(), 0, 0);
      }
   }

   public NativeImage getPixels() {
      return this.mipImages != null ? this.mipImages[0] : null;
   }

   public void setPixels(NativeImage image) {
      if (this.closed || this.mipLevelCount != 1) {
         throw new IllegalStateException("Single-level replacement requires an open single-level texture");
      }
      NativeImage previous = this.mipImages[0];
      if (image == previous) return;
      NativeImage[] replacement = validateMipImages(new NativeImage[]{image});
      if (image.getWidth() != previous.getWidth() || image.getHeight() != previous.getHeight()) {
         throw new IllegalArgumentException("Replacement dimensions must match the texture");
      }
      previous.close();
      this.mipImages = replacement;
   }

   @Override
   public void close() {
      if (this.closed) return;
      this.closed = true;
      NativeImage[] images = this.mipImages;
      this.mipImages = null;
      Throwable failure = null;
      if (images != null) {
         for (NativeImage image : images) {
            if (image == null) continue;
            try {
               image.close();
            } catch (RuntimeException | Error cleanupFailure) {
               if (failure == null) failure = cleanupFailure;
               else if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
            }
         }
      }
      try {
         super.close();
      } catch (RuntimeException | Error cleanupFailure) {
         if (failure == null) failure = cleanupFailure;
         else if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
      }
      if (failure instanceof RuntimeException runtime) throw runtime;
      if (failure instanceof Error error) throw error;
   }
}
