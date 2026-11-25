/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.schizo.client.events.AbstractContainerScreenEvent;
import dev.schizo.client.events.AbstractContainerScreenRenderTooltipEvent;
import dev.schizo.client.modules.misc.ChestSearchBarModule;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.inventory.ContainerTooltipComponent;
import dev.schizo.client.access.TooltipComponentCapture;
import dev.schizo.client.utils.KeyUtils;
import dev.schizo.client.utils.inventory.ChestSearchController;
import dev.schizo.client.modules.player.InventoryTweaksModule;
import dev.schizo.client.modules.render.TooltipsModule;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin implements ClientAccess {



   @Shadow
   @Nullable
   protected Slot hoveredSlot;
   @Unique
   private int schizo$mouseX;
   @Unique
   private int schizo$mouseY;
   @Unique
   private int schizo$tooltipX;
   @Unique
   private int schizo$tooltipY;
   @Unique
   private Slot schizo$cachedHoveredSlot;
   @Unique
   private MapItemSavedData schizo$slotMapData;
   @Unique
   private int schizo$slotMapId;
   @Unique
   private final AbstractContainerScreenEvent schizo$containerScreenEvent = new AbstractContainerScreenEvent(
      (AbstractContainerScreen)(Object)this
   );

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   public void schizo$preventInventoryCloseDuringSearch(
      KeyEvent keyEvent, CallbackInfoReturnable<Boolean> callback
   ) {
      if (schizo$chestSearchBar != null
         && schizo$chestSearchBar.isToggled()
         && ChestSearchController.searchBox != null
         && ChestSearchController.searchBox.isFocused()
         && ClientAccess.schizoMinecraft.options.keyInventory.matches(keyEvent)) {
         callback.setReturnValue(true);
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void schizo$runContainerTickCallback(CallbackInfo callback) {
      this.schizo$containerScreenEvent
         .runTickCallback();
   }

   @Inject(method = "renderTooltip", at = @At("HEAD"))
   private void schizo$postContainerScreenEvent(
      GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback
   ) {
      this.schizo$containerScreenEvent
         .setGraphics(graphics);
      this.schizo$containerScreenEvent
         .setMouseX(mouseX);
      this.schizo$containerScreenEvent
         .setMouseY(mouseY);
      ClientAccess.eventBus.post(
         this.schizo$containerScreenEvent
      );
   }

   @Inject(method = "renderContents", at = @At("HEAD"))
   private void schizo$captureHoveredSlotAndMouse(
      GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback
   ) {
      if (!KeyUtils.isPhysicalKeyDown(342)) {
         this.schizo$cachedHoveredSlot = this.hoveredSlot;
      }

      this.schizo$mouseX = mouseX;
      this.schizo$mouseY = mouseY;
   }

   @Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
   private void schizo$useInspectedHoveredSlot(
      double mouseX, double mouseY, CallbackInfoReturnable<Slot> callback
   ) {
      if (this.schizo$cachedHoveredSlot != null
         && schizo$tooltips.shouldInspectContents(
            this.schizo$cachedHoveredSlot.getItem()
         )) {
         callback.setReturnValue(this.schizo$cachedHoveredSlot);
      }
   }

   @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasClickedOutside(DDII)Z"))
   public boolean schizo$keepInspectedClickInside(boolean originalInside) {
      return (this.schizo$cachedHoveredSlot == null
         || !schizo$tooltips.shouldInspectContents(this.schizo$cachedHoveredSlot.getItem()))
         && originalInside;
   }

   @ModifyArgs(
      method = "mouseClicked",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", ordinal = 1)
   )
   private void schizo$useBundleInspectButton(Args arguments) {
      int mouseButton = arguments.<Integer>get(2);
      if (this.schizo$cachedHoveredSlot != null
         && schizo$tooltips.shouldInspectContents(
            this.schizo$cachedHoveredSlot.getItem()
         )
         && mouseButton == 0
         && this.schizo$cachedHoveredSlot
            .getItem()
            .is(ItemTags.BUNDLES)) {
         arguments.set(2, 1);
      }
   }

   @ModifyArgs(
      method = "mouseReleased",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", ordinal = 9)
   )
   private void schizo$swapBundleReleaseButton(Args arguments) {
      Slot slot = arguments.get(0);
      int mouseButton = arguments.<Integer>get(2);
      if (schizo$inventoryTweaks.isToggled()
         && schizo$inventoryTweaks.shouldSwapBundleClickButton(
            slot, mouseButton
         )) {
         arguments.set(
            2,
            mouseButton == 0 ? 1 : 0
         );
      }
   }

   @WrapWithCondition(
      method = "renderTooltip",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
      )
   )
   private boolean schizo$renderTooltipWithEvent(
      GuiGraphics graphics, Font font, List text, Optional image, int tooltipX, int tooltipY, Identifier background
   ) {
      if (this.schizo$cachedHoveredSlot != null
         && schizo$tooltips.shouldInspectContents(
            this.schizo$cachedHoveredSlot.getItem()
         )) {
         tooltipX = this.schizo$tooltipX;
         tooltipY = this.schizo$tooltipY;
      } else {
         this.schizo$tooltipX = tooltipX;
         this.schizo$tooltipY = tooltipY;
      }

      List components = TooltipComponentCapture.captureComponents(graphics, text, image);
      AbstractContainerScreenRenderTooltipEvent event = new AbstractContainerScreenRenderTooltipEvent(
         (Screen)(Object)this,
         components,
         tooltipX,
         tooltipY,
         this.schizo$mouseX,
         this.schizo$mouseY
      );
      ClientAccess.eventBus
         .post(event);
      if (!event.isCancelled()) {
         ((GuiGraphicsAccessor)graphics)
            .drawTooltipsHook(
               font,
               event.getTooltipComponents(),
               tooltipX,
               tooltipY,
               DefaultTooltipPositioner.INSTANCE,
               null
            );
      }

      return false;
   }

   @Inject(method = "renderSlot", at = @At("HEAD"))
   private void schizo$captureSlotMapData(
      GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo callback
   ) {
      this.schizo$slotMapData = null;
      ItemStack stack = slot.getItem();
      if (stack.is(Items.FILLED_MAP)) {
         int mapId = stack.getOrDefault(DataComponents.MAP_ID, new MapId(-1)).id();
         if (mapId != -1) {
            this.schizo$slotMapId = mapId;
            this.schizo$slotMapData = ClientContext.mapDataCache
               .getMapData(stack, mapId);
         }
      }
   }

   @WrapWithCondition(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
   private boolean schizo$shouldRenderSlotItem(
      GuiGraphics graphics, ItemStack stack, int itemX, int itemY, int seed
   ) {
      return !schizo$tooltips.isToggled()
         || !schizo$tooltips.mapOverlaySetting.getValue()
         || this.schizo$slotMapData == null;
   }

   @WrapWithCondition(
      method = "renderSlot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V")
   )
   private boolean schizo$renderMapOverlay(
      GuiGraphics graphics, Font font, ItemStack stack, int itemX, int itemY, String countText, @Local(argsOnly = true) Slot slot
   ) {
      if (!schizo$tooltips.isToggled() || !schizo$tooltips.mapOverlaySetting.getValue() || this.schizo$slotMapData == null) {
         return true;
      }
      ContainerTooltipComponent.renderMapOverlay(
         graphics,
         this.schizo$slotMapData,
         this.schizo$slotMapId,
         itemX,
         itemY
      );
      if (!KeyUtils.isPhysicalKeyDown(342)) {
         graphics.renderItemDecorations(font, stack, itemX, itemY);
      }

      return false;
   }

   @org.spongepowered.asm.mixin.Unique
   private static ChestSearchBarModule schizo$schizo$chestSearchBar() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(ChestSearchBarModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static TooltipsModule schizo$schizo$tooltips() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(TooltipsModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static InventoryTweaksModule schizo$schizo$inventoryTweaks() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(InventoryTweaksModule.class);
   }
}
