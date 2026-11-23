/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.utils.network.ImportantPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerboundUseItemPacket.class)
public class ServerboundUseItemPacketMixin implements ImportantPacket {
   @Unique
   private boolean important;

   @Override
   public void schizo$markImportant() {
      this.important = true;
   }

   @Override
   public boolean schizo$isImportant() {
      return this.important;
   }
}
