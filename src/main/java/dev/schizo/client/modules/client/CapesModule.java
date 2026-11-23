/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.resources.Identifier;

public final class CapesModule extends Module {
   private final EnumSetting<Cape> capeSetting;

   public CapesModule() {
      super("Capes", "Displays a cape on your player.", ModuleCategory.CLIENT);
      this.capeSetting = this.add(new EnumSetting<>("Cape", Cape.ZIO));
   }

   public Texture getCapeTexture() {
      return this.capeSetting.getValue().texture;
   }

   private enum Cape implements NamedEntry {
      ZIO("Schizo", "schizo.png"),
      PEPSI("Pepsi", "pepsi.png"),
      TETRIS("Tetris", "tetris.png"),
      SUCKMY("SuckMy", "suckmy.png");

      private final String displayName;
      private final ResourceTexture texture;

      Cape(String displayName, String fileName) {
         this.displayName = displayName;
         Identifier textureId = Identifier.fromNamespaceAndPath("schizo", "capes/" + fileName);
         this.texture = new ResourceTexture(textureId, textureId);
      }

      @Override
      public String getName() {
         return this.displayName;
      }
   }
}
