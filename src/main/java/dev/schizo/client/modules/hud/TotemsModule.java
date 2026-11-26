/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.render.ResizableHudWidgetRenderer;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TotemsModule extends HudWidgetModule {
   private ItemStack totemStack;
   private final Setting<Boolean> whiteSetting;

   @Override
   public float[] calculateDimensions() {
      return this.dimensions(16.0F, 16.0F);
   }

   @Override
   public void onDisable() {
      this.totemStack = null;
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.totemStack = null;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent ignoredEvent) {
      LocalPlayer localPlayer = ClientAccess.schizoMinecraft.player;
      if (!localPlayer.isSpectator()) {
         this.totemStack = new ItemStack(
            Items.TOTEM_OF_UNDYING,
            InventoryUtils.countItems((Predicate<ItemStack>)candidateStack -> {
               Item totemItemForMatch = Items.TOTEM_OF_UNDYING;
               return candidateStack.is(totemItemForMatch);
            })
         );
      }
   }

   @Override
   public void renderContent(PoseStack poses) {
      if (this.totemStack != null) {
         ItemStack stackForEmptyCheck = this.totemStack;
         if (stackForEmptyCheck.getCount() != 0) {
            ItemStack stackToRender = this.totemStack;

            GuiDrawUtils.drawItem(
               poses, stackToRender, 0.0F, 0.0F, -150.0F, true
            );
            ItemStack stackForOverlayCheck = this.totemStack;
            if (stackForOverlayCheck.getCount() > 1) {
               int totemCount = this.totemStack.getCount();
               String countText = String.valueOf(totemCount);
               poses.pushPose();
               try {
                  double textDepth = 0.1D;
                  double textTranslateY = 0.0;
                  double textTranslateX = 0.0;
                  poses.translate(textTranslateX, textTranslateY, textDepth);
                  FontRenderer font = FontRenderer.INSTANCE;

                  float textX = 17.0F
                     - FontRenderer.INSTANCE.getWidth(countText);
                  float textY = 9.0F;
                  Setting<Boolean> whiteOption = this.whiteSetting;
                  Boolean useWhite = whiteOption.getValue();
                  Color textColor;
                  if (useWhite) {
                     textColor = Color.white;
                  } else {

                     textColor = this.getHudColor(9.0F);
                  }

                  font.drawWithShadow(poses, countText, textX, textY, textColor);
               } finally {
                  poses.popPose();
               }
            }

            return;
         }
      }
   }

   public TotemsModule() {
      super("Totems", new String[0]);

      this.whiteSetting = this.add(new BooleanSetting("White", false));
      this.setRenderer(new ResizableHudWidgetRenderer(this));
   }

}
