/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.inventory;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

public interface PalettedContainerDataAccess {
   static BitStorage getStorage(PalettedContainer container) {
      PalettedContainerDataAccess data = ((PalettedContainerAccess)container).schizo$data();
      return data.schizo$storage();
   }

   static Palette getPalette(PalettedContainer container) {
      PalettedContainerDataAccess data = ((PalettedContainerAccess)container).schizo$data();
      return data.schizo$palette();
   }

   BitStorage schizo$storage();

   Palette schizo$palette();
}
