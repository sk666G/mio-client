/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.registry;

import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.gui.components.ComponentFlag;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.utils.ClampedInterpolatedFloat;
import dev.schizo.client.gui.components.ButtonComponent;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.gui.ScissorStack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

class RegistryEntryComponent<E> extends ButtonComponent {
   public final ClampedInterpolatedFloat visibilityAnimation = new ClampedInterpolatedFloat(
      () -> this.getUiModule().animSpeedSetting.getValue()
         * 2.0F,
      true
   );
   protected final FloatAnimation hoverScrollAnimation = new FloatAnimation();
   private final RegistrySetScreen<E> screen;
   private final E entry;
   private final Identifier identifier;
   private final String normalizedLabel,normalizedId;
   private ItemStack iconStack;
   private final boolean availableEntry;
   private boolean resetVisibilityAnimation = true;
   private boolean visible;

   public RegistryEntryComponent(RegistrySetScreen<E> screen, Panel panel, Identifier identifier, E entry, boolean availableEntry) {
      super(
         panel,
         createLabel(identifier)
      );
      this.screen = screen;
      this.entry = entry;
      this.availableEntry = availableEntry;
      this.identifier=identifier;
      this.normalizedLabel=RegistrySearchText.normalize(getLabel());
      this.normalizedId=RegistrySearchText.normalize(identifier.toString());
      this.addFlags(new ComponentFlag[]{ComponentFlag.SILENT_HOVER});
   }

   @Override
   public void update(double mouseX, double mouseY) {
      this.visible = this.screen
            .registrySetting
            .getValue()
         .contains(this.entry);
      if (this.availableEntry) {
         this.visible = !this.visible;
         if (!RegistrySearchText.matches(normalizedLabel,normalizedId,screen.normalizedSearch())) {
            this.visible = false;
            this.resetVisibilityAnimation = true;
            this.visibilityAnimation
               .setValue(false);
            return;
         }
      }

      if (this.resetVisibilityAnimation) {
         this.visibilityAnimation
            .setValue(this.visible);
         this.resetVisibilityAnimation = false;
      } else {
         this.visibilityAnimation
            .setTarget(this.visible);
         this.visibilityAnimation
            .updateAndGet();
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      super.mouseClicked(mouseX, mouseY, button);
      if (this.visible && this.isMouseOver(mouseX, mouseY) && button == 0
         && this.screen.registrySetting.getValue().contains(entry)!=availableEntry
         && (!availableEntry || RegistrySearchText.matches(normalizedLabel,normalizedId,screen.normalizedSearch()))) {
         if (this.availableEntry) {
            this.screen
               .registrySetting
               .addEntry(
                  this.entry
               );
         } else {
            this.screen
               .registrySetting
               .removeEntry(this.entry);
         }
      }
   }

   @Override
   public void render(PoseStack poseStack, double mouseX, double mouseY) {
      if (this.visible) {
         float iconOffset = 0.0F;
         if(iconStack==null) iconStack=createIconStack(identifier,entry);
         if (!iconStack.isEmpty() && iconStack.getItem()!=Items.AIR) {
            GuiDrawUtils.drawItem(
               poseStack,
               this.iconStack,
               this.panel
                     .getX()
                  + (3),
               (int)(
                  this.panel
                           .getY()
                        + this.yOffset
                     + this.getTextYOffset()
                     - 2.0F
               ),
               0.0F
            );
            iconOffset = 2e+01F;
         }

         poseStack.pushPose();
         try {
            poseStack.translate(
               iconOffset,
               1.5F,
               0.0F
            );
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            this.hoverScrollAnimation
               .setTarget(hovered, 500L);
            this.renderClippedLabel(
               poseStack,
               iconOffset
                  + 3.0F,
               mouseX,
               mouseY
            );
         } finally {
            poseStack.popPose();
         }
      }
   }

   @Override
   public int getHeight() {
      return Math.max(
         (int)(
            16.0F
               * this.visibilityAnimation
                  .getValue()
         ),
         this.visible ? 1 : 0
      );
   }

   private void renderClippedLabel(
      PoseStack poseStack, float textOffset, double mouseX, double mouseY
   ) {
      float textWidth = FontRenderer.INSTANCE
            .getWidth(
               this.getLabel()
            )
         + textOffset;
      float scrollProgress = this.hoverScrollAnimation
         .getValue();
      int rightInset = scrollProgress > 0.0F ? 0 : 2;
      if (textWidth
            >= this.panel
               .getWidth()
         && scrollProgress > 0.0F) {
         GuiDrawUtils.flush();
         ScissorStack.pushIntersection(
            (int)(
               this.panel
                     .getX()
                  + textOffset
            ),
            this.panel
                  .getY()
               + this.yOffset,
            (int)(
               this.panel
                     .getWidth()
                  - textOffset
                  - rightInset
            ),
            this.getHeight()
         );
         try {
            poseStack.translate(
               -(
                     textWidth
                        - this.panel
                           .getWidth()
                  )
                  * scrollProgress,
               0.0F,
               0.0F
            );
            super.render(poseStack, mouseX, mouseY);
         } finally {
            try {
               GuiDrawUtils.flush();
            } finally {
               ScissorStack.pop();
            }
         }
      } else {
         super.render(poseStack, mouseX, mouseY);
      }
   }

   private static ItemStack createIconStack(Identifier identifier, Object entry) {
      if (entry instanceof EntityType entityType) {
         if (entityType == EntityType.ITEM) {
            return Items.STICK.getDefaultInstance();
         }

         SpawnEggItem spawnEgg = SpawnEggItem.byId(entityType);
         if (spawnEgg != null) {
            return spawnEgg.getDefaultInstance();
         }
      }

      if (entry instanceof Potion potion) {
         Holder potionHolder = BuiltInRegistries.POTION.wrapAsHolder(potion);
         return PotionContents.createItemStack(Items.SPLASH_POTION, potionHolder);
      } else {
         Item item=BuiltInRegistries.ITEM.getValue(identifier);
         return item==null?ItemStack.EMPTY:item.getDefaultInstance();
      }
   }

   private static String createLabel(Identifier identifier) {
      return FontRenderer.capitalizeWords(
         identifier.toShortLanguageKey().replace("_", " ")
      );
   }

}
