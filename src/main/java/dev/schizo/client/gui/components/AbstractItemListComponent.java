/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.gui.components.AbstractComponent;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractItemListComponent<T> extends AbstractComponent {
   protected final List<T> entries;
   protected final Predicate<T> filter;
   private T selection = null;
   private int scrollOffset;

   public AbstractItemListComponent(Panel panel, Predicate<T> filter, int initialCapacity) {
      super(panel);
      this.entries = new ArrayList<>(initialCapacity);
      this.filter = filter;
   }

   public abstract String getEntryLabel(T entry);

   public abstract ItemStack getEntryIcon(T entry);

   @Override
   public void render(PoseStack stack, double mouseX, double mouseY) {
      super.render(stack, mouseX, mouseY);
      stack.pushPose();
      try {
         stack.translate(this.panel.getX() + 1, this.panel.getY() + this.yOffset, 0.0F);
         this.clampScrollOffset();
         GuiDrawUtils.flush();

         for (int row = 0; row < 8 && this.scrollOffset + row < this.entries.size(); row++) {
            T entry = this.entries.get(this.scrollOffset + row);
            Color textColor = Color.white;
            String label = this.getEntryLabel(entry);
            int hoverTextOffset = this.isRowHovered(mouseX, mouseY, row) ? 0 : 1;
            GuiDrawUtils.drawItem(stack, this.getEntryIcon(entry), 1.0F, 16 * row, 0.0F);
            FontRenderer.INSTANCE.drawWithShadow(
               stack,
               this.truncateLabel(label),
               20.0F,
               (16 - FontRenderer.INSTANCE.getHeight()) / 2.0F + hoverTextOffset + 16 * row,
               textColor
            );
         }
      } finally {
         stack.popPose();
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      super.mouseClicked(mouseX, mouseY, button);
      this.selection = null;
      if (this.isMouseOver(mouseX, mouseY)) {
         this.clampScrollOffset();
         int index = ItemListLayout.entryAt(mouseY - this.panel.getY() - this.yOffset, this.scrollOffset, this.entries.size());
         if (index >= 0) {
            this.selection = this.entries.get(index);
         }
      }
   }

   @Override
   public void mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
      super.mouseScrolled(mouseX, mouseY, (double)scrollAmount);
      if (this.isMouseOver(mouseX, mouseY)) {
         this.scrollOffset -= (int)scrollAmount;
         this.clampScrollOffset();
      }
   }

   private boolean isRowHovered(double mouseX, double mouseY, int row) {
      int relativeMouseY = (int)(mouseY - this.panel.getY() - this.yOffset);
      return this.isMouseOver(mouseX, mouseY) && relativeMouseY / 16 == row;
   }

   private void clampScrollOffset() {
      this.scrollOffset = Mth.clamp(this.scrollOffset, 0, Math.max(this.entries.size() - 8, 0));
   }

   private String truncateLabel(String label) {
      return ItemListLayout.fitLabel(label, this.getPanel().getWidth() - 20.0F, text -> FontRenderer.INSTANCE.getWidth(text));
   }

   public T getSelectedEntry() {
      return this.selection;
   }

   @Override
   public int getBaseHeight() {
      return 128;
   }

}
