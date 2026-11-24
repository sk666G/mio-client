/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.player;

import java.util.Set;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.RegistrySetSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.ListFilterMode;
import dev.schizo.client.settings.options.ItemSwapMode;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class AutoNameTagModule extends Module {
   public final Setting<Set<EntityType<?>>> entitiesSetting;
   private final Setting<ListFilterMode> modeSetting;
   private final Setting<Float> rangeSetting;
   private final Setting<Integer> frequencySetting;
   private final Setting<Float> delaySetting;
   private final Setting<Boolean> rotateSetting;
   private final Setting<ItemSwapMode> swapSetting;
   private final ElapsedTimer nameTagTimer;

   private boolean isNamedNameTag(ItemStack stack) {
      Item nameTagItem = Items.NAME_TAG;

      if (!stack.is(nameTagItem)) {
         return false;
      }
      DataComponentType<?> customNameType = DataComponents.CUSTOM_NAME;
      return stack.has(customNameType);
   }

   @EventHandler
   private void onLocalPlayerSendPositionEvent(LocalPlayerSendPositionEvent event) {
      if (event.getStage() == EventStage.Post) {
         return;
      }
      ElapsedTimer timerSnapshot = this.nameTagTimer;
      double delaySecondsSnapshot = (this.delaySetting.getValue()).floatValue();
      TimeUnit delayUnit = TimeUnit.SECONDS;

      if (!timerSnapshot.hasElapsed(delaySecondsSnapshot, delayUnit)) {
         return;
      }
      Predicate<ItemStack> nameTagPredicate = this::isNamedNameTag;

      int nameTagSlot = InventoryUtils.findHotbarSlot(nameTagPredicate);
      int previousSlot = ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot();
      ItemSwapMode swapMode = this.swapSetting.getValue();
      boolean selectedNameTagSlot = swapMode != ItemSwapMode.NONE && nameTagSlot != -1;
      int taggedCount = 0;

      try {
         if (selectedNameTagSlot) {
            InventoryUtils.selectHotbarSlotForPlacement(nameTagSlot);
         }
         ClientLevel level = ClientAccess.schizoMinecraft.level;
         for (Entity entity : level.entitiesForRendering()) {
            ItemStack heldStack = ClientAccess.schizoMinecraft.player.getMainHandItem();
            if (!this.isNamedNameTag(heldStack)) {
               break;
            }

            ListFilterMode selectedMode = this.modeSetting.getValue();

            EntityType selectedEntityType = entity.getType();
            Setting<Set<EntityType<?>>> entitiesSettingSnapshot = this.entitiesSetting;

            if (!ListFilterMode.matches(selectedMode, selectedEntityType, entitiesSettingSnapshot)
               || entity instanceof Player) {
               continue;
            }
            String heldNameText = ClientAccess.schizoMinecraft.player.getMainHandItem().getHoverName().getString();

            String selectedEntityText;
            if (entity.hasCustomName()) {
               selectedEntityText = entity.getCustomName().getString();
            } else {
               selectedEntityText = "";
            }

            if (heldNameText.equalsIgnoreCase(selectedEntityText)) {
               continue;
            }
            Vec3 playerEyePosition = ClientAccess.schizoMinecraft.player.getEyePosition();

            Vec3 entityPosition = entity.position();

            double distance = playerEyePosition.distanceTo(entityPosition);
            Float range = this.rangeSetting.getValue();
            if (distance > range.floatValue()) {
               continue;
            }
            InteractionHand interactionHand = InteractionHand.MAIN_HAND;

            BlockInteractionUtils.interactWithEntity(entity, interactionHand);
            taggedCount++;
            if (this.rotateSetting.getValue()) {
               Vec3 targetPosition = entity.position();

               float[] rotations = RotationManager.getRotationsToPosition(
                  targetPosition);

               event.setRotation(rotations);
            }

            Integer frequency = this.frequencySetting.getValue();
            if (taggedCount >= frequency) {
               break;
            }

         }
      } finally {
         if (selectedNameTagSlot && swapMode == ItemSwapMode.SILENT) {
            InventoryUtils.selectHotbarSlot(previousSlot);
         }
      }

      timerSnapshot = this.nameTagTimer;

      timerSnapshot.reset();
   }

   public AutoNameTagModule() {
      super("AutoNameTag", "Will use name tags on nearby entities.", ModuleCategory.PLAYER, new String[0]);
      DefaultedRegistry entityRegistry = BuiltInRegistries.ENTITY_TYPE;
      EntityType[] defaultEntities = new EntityType[1];
      EntityType witherType = EntityType.WITHER;
      defaultEntities[0] = witherType;
      this.entitiesSetting = this.add(new RegistrySetSetting<>("Entities", entityRegistry, (Object[]) defaultEntities));
      ListFilterMode defaultMode = ListFilterMode.WHITELIST;
      this.modeSetting = this.add(new EnumSetting<>("Mode", defaultMode));
      this.rangeSetting = this.add(new NumberSetting<>("Range", 4.0F, 1.0F, 6.0F).withUnit("m"));
      this.frequencySetting = this.add(new NumberSetting<>("Frequency", 1, 1, 8));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 0.0F, 0.1F, 5.0F).withUnit("s"));
      Boolean defaultRotate = true;
      this.rotateSetting = this.add(new BooleanSetting("Rotate", defaultRotate));
      ItemSwapMode defaultSwap = ItemSwapMode.NONE;
      this.swapSetting = this.add(new EnumSetting<>("Swap", defaultSwap));
      this.nameTagTimer = new ElapsedTimer();
   }

}
