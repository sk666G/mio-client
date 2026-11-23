/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.jtracy.MemoryPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {
   @Accessor("MEMORY_POOL")
   static MemoryPool schizo$getPool() {
      return null;
   }

   @Accessor("pixels")
   long schizo$getPointer();

   @Accessor("size")
   long schizo$getSize();
}
