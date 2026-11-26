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
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.inventory.CustomItemRegistries;
import dev.schizo.client.settings.SettingLabelCondition;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.combat.MainhandModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.RegistrySetSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.ListFilterMode;

import java.util.Collection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.SuspiciousStewEffects.Entry;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

public class AutoEatModule extends Module {
   private static MainhandModule mainhandModule;
   private final Setting<Set<Item>> itemsSetting;
   private final Setting<ListFilterMode> modeSetting;
   private final Setting<Integer> healthSetting;
   private final Setting<Integer> hungerSetting;
   private final Setting<Boolean> untilFullSetting;
   private final Setting<Boolean> pauseBaritoneSetting;
   private final Setting<Boolean> autoSwapSetting;
   private final Setting<SortChoice> sortSetting;
   private final Setting<Boolean> swapBackSetting;
   private final Setting<Boolean> autoEscapeSetting;
   private final Setting<Boolean> autoDisableSetting;
   private final dev.schizo.client.utils.inventory.TemporaryHotbarSelection selectionOwner = new dev.schizo.client.utils.inventory.TemporaryHotbarSelection();
   private boolean eating;
   private boolean continueUntilFull;
   private int swappedContainerSlot = -1;
   private int swappedContainerHotbarSlot = -1;
   private int swappedContainerId = -1;
   private LocalPlayer swappedContainerOwner;
   private AbstractContainerMenu swappedContainerMenu;
   private ItemStack expectedContainerStack = ItemStack.EMPTY;
   private ItemStack expectedHotbarStack = ItemStack.EMPTY;

   @Override
   public void onEnable() {
      this.setEating(false);
      this.continueUntilFull = false;
      this.clearSwapState();
   }

   @Override
   public void onDisable() {
      ClientAccess.schizoMinecraft.options.keyUse.setDown(false);
      ClientContext.baritoneBridge.releasePause(this);
      try {
         if (!this.isWorldUnavailable()) {
            try {
               this.restoreTemporaryHotbarSelection();
            } finally {
               this.restoreSwappedContainerSlot();
            }
         } else {
            this.clearSwapState();
         }
      } finally {
         this.setEating(false);
         this.continueUntilFull = false;
         this.clearSwapState();
      }
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      ClientAccess.schizoMinecraft.options.keyUse.setDown(false);
      ClientContext.baritoneBridge.releasePause(this);
      this.setEating(false);
      this.continueUntilFull = false;
      this.clearSwapState();
   }

   private void restoreTemporaryHotbarSelection() {
      this.selectionOwner.restore(this.autoSwapSetting.getValue() && this.swapBackSetting.getValue());
   }

   private void restoreSwappedContainerSlot() {
      int containerSlot = this.swappedContainerSlot;
      int hotbarSlot = this.swappedContainerHotbarSlot;
      int containerId = this.swappedContainerId;
      LocalPlayer owner = this.swappedContainerOwner;
      AbstractContainerMenu menu = this.swappedContainerMenu;
      try {
         LocalPlayer currentPlayer = ClientAccess.schizoMinecraft.player;
         if (containerSlot >= 0 && hotbarSlot >= 0 && hotbarSlot < 9
            && currentPlayer != null && currentPlayer == owner
            && currentPlayer.containerMenu == menu && menu != null
            && menu.containerId == containerId && containerSlot < menu.slots.size()
            && ItemStack.matches(menu.getSlot(containerSlot).getItem(), this.expectedContainerStack)
            && (owner.getInventory().getItem(hotbarSlot).isEmpty()
               || ItemStack.isSameItemSameComponents(owner.getInventory().getItem(hotbarSlot), this.expectedHotbarStack))) {
            this.swapWithHotbarSlot(owner, menu, containerSlot, hotbarSlot);
         }
      } finally {
         this.swappedContainerSlot = -1;
         this.swappedContainerHotbarSlot = -1;
         this.swappedContainerId = -1;
         this.swappedContainerOwner = null;
         this.swappedContainerMenu = null;
         this.expectedContainerStack = ItemStack.EMPTY;
         this.expectedHotbarStack = ItemStack.EMPTY;
      }
   }

   private void clearSwapState() {
      this.selectionOwner.clear();
      this.swappedContainerSlot = -1;
      this.swappedContainerHotbarSlot = -1;
      this.swappedContainerId = -1;
      this.swappedContainerOwner = null;
      this.swappedContainerMenu = null;
      this.expectedContainerStack = ItemStack.EMPTY;
      this.expectedHotbarStack = ItemStack.EMPTY;
   }

   private void swapWithHotbarSlot(
      LocalPlayer player, AbstractContainerMenu menu, int containerSlot, int hotbarSlot
   ) {
      MultiPlayerGameMode gameMode = ClientAccess.schizoMinecraft.gameMode;
      gameMode.handleInventoryMouseClick(
         menu.containerId, containerSlot, hotbarSlot, ClickType.SWAP, player
      );
   }

   @EventHandler
   private void onLocalPlayerSendPositionEvent(LocalPlayerSendPositionEvent event) {
      EventStage postStage = EventStage.Post;
      if (event.getStage() == postStage) {
         return;
      }
      if (this.continueUntilFull) {
         FoodData foodData = ClientAccess.schizoMinecraft.player.getFoodData();
         if (!foodData.needsFood()) {
            this.continueUntilFull = false;
         }
      }

      ClientContext.baritoneBridge.releasePause(this);
      Boolean initialAutoSwapEnabled = this.autoSwapSetting.getValue();
      if (!initialAutoSwapEnabled && this.eating) {
         ItemStack heldMainHandStack = ClientAccess.schizoMinecraft.player.getMainHandItem();
         if (!this.canEat(heldMainHandStack)) {
            MultiPlayerGameMode invalidStackGameMode = ClientAccess.schizoMinecraft.gameMode;
            LocalPlayer invalidStackPlayer = ClientAccess.schizoMinecraft.player;
            invalidStackGameMode.releaseUsingItem(invalidStackPlayer);
            this.setEating(false);
            return;
         }
      }

      int foodSlot;
      escapeFoodSelection: {
         foodSlot = this.findFoodSlot();
         if (foodSlot != -1) {
            ItemStack candidateStack = ClientAccess.schizoMinecraft.player.getInventory().getItem(foodSlot);
            Item chorusFruitToCompare = Items.CHORUS_FRUIT;
            if (candidateStack.is(chorusFruitToCompare)) {
               break escapeFoodSelection;
            }
         }

         if (this.shouldEscape()) {
            Item chorusFruitToFind = Items.CHORUS_FRUIT;
            int chorusContainerSlot = InventoryUtils.findMenuSlot(chorusFruitToFind);
            if (chorusContainerSlot != -1) {
               foodSlot = -1;
               LocalPlayer escapePlayer = ClientAccess.schizoMinecraft.player;
               AbstractContainerMenu escapeMenu = escapePlayer.containerMenu;
               int escapeHotbarSlot = InventoryUtils.getSelectedSlot();
               this.swapWithHotbarSlot(
                  escapePlayer, escapeMenu, chorusContainerSlot, escapeHotbarSlot
               );
               this.swappedContainerSlot = chorusContainerSlot;
               this.swappedContainerHotbarSlot = escapeHotbarSlot;
               this.swappedContainerId = escapeMenu.containerId;
               this.swappedContainerOwner = escapePlayer;
               this.swappedContainerMenu = escapeMenu;
               this.expectedContainerStack = escapeMenu.getSlot(chorusContainerSlot).getItem().copy();
               this.expectedHotbarStack = escapePlayer.getInventory().getItem(escapeHotbarSlot).copy();
            }
         }
      }

      if (this.shouldStopEating()) {
         if (!this.shouldEscape()) {
            boolean useTicksAtThreshold = ClientAccess.schizoMinecraft.player.getTicksUsingItem() >= (2);
            if (this.eating && !useTicksAtThreshold) {
               MultiPlayerGameMode stopUseGameMode = ClientAccess.schizoMinecraft.gameMode;
               LocalPlayer stopUsePlayer = ClientAccess.schizoMinecraft.player;
               try {
                  stopUseGameMode.releaseUsingItem(stopUsePlayer);
               } finally {
                  this.setEating(false);
                  try {
                     this.restoreTemporaryHotbarSelection();
                  } finally {
                     this.restoreSwappedContainerSlot();
                  }
               }

               if (this.autoDisableSetting.getValue()) {
                  if (this.autoEscapeSetting.getValue()) {
                     this.disable();
                  }
               }
            }
            return;
         }
      }

      boolean useOffHand = foodSlot == (40);
      this.clearPauseReason();
      if (dev.schizo.client.utils.inventory.InventoryActionPriority.pauseFood(
            mainhandModule != null && mainhandModule.requiresEmergencyProtection(), useOffHand)) {
         if (this.eating && ClientAccess.schizoMinecraft.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            ClientAccess.schizoMinecraft.gameMode.releaseUsingItem(ClientAccess.schizoMinecraft.player);
            this.setEating(false);
         }
         this.selectionOwner.clear();
         this.pauseWithReason("Mainhand emergency protection");
         return;
      }
      if ((this.autoSwapSetting.getValue()) && !useOffHand && foodSlot != -1) {
         if (ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot() != foodSlot) {
            this.selectionOwner.select(foodSlot);
         }
      }

      ItemStack mainHandStack = ClientAccess.schizoMinecraft.player.getMainHandItem();
      LocalPlayer useStatePlayer = ClientAccess.schizoMinecraft.player;
      if (!useStatePlayer.isUsingItem() && (useOffHand || this.canEat(mainHandStack))) {
         MultiPlayerGameMode useItemGameMode = ClientAccess.schizoMinecraft.gameMode;
         LocalPlayer useItemPlayer = ClientAccess.schizoMinecraft.player;
         InteractionHand useHand = useOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         useItemGameMode.useItem(useItemPlayer, useHand);
         this.setEating(true);
      }

      if (this.pauseBaritoneSetting.getValue()) {
         if (ClientAccess.schizoMinecraft.player.isUsingItem()) {
            ClientContext.baritoneBridge.requestPause(this);
         }
      }
   }

   public boolean isEating() {
      return this.eating;
   }

   private void setEating(boolean requestedEating) {
      if (this.eating == requestedEating) {
         return;
      }
      if (requestedEating && this.isUntilFullEnabled()) {
         if (ClientAccess.schizoMinecraft.player.getFoodData().needsFood()) {
            this.continueUntilFull = true;
         }
      }
      this.eating = requestedEating;
   }

   private int findFoodSlot() {
      double bestScore = -1.0D;
      int selectedFoodSlot = -1;
      for (int scanSlot = 0; ; scanSlot++) {
         if (scanSlot >= (10)) {
            break;
         }
         if (scanSlot == (9)) {
            scanSlot = 40;
         }
         ItemStack stack = ClientAccess.schizoMinecraft.player.getInventory().getItem(scanSlot);
         FoodProperties foodProperties = (FoodProperties)stack.get(DataComponents.FOOD);
         if (!this.canEat(stack)) {
            continue;
         }
         double candidateScore = (this.sortSetting.getValue()).score(stack, foodProperties);
         boolean betterScore = candidateScore > bestScore;
         if (!betterScore && candidateScore == bestScore) {
            Inventory tieBreakInventory = ClientAccess.schizoMinecraft.player.getInventory();
            betterScore = scanSlot == tieBreakInventory.getSelectedSlot();
         }
         if (betterScore) {
            bestScore = candidateScore;
            selectedFoodSlot = scanSlot;
         }
         if (stack.getItem() == Items.CHORUS_FRUIT) {
            if (this.shouldEscape()) {
               selectedFoodSlot = scanSlot;
               break;
            }
         }

      }
      return selectedFoodSlot;
   }

   private boolean canEat(ItemStack stack) {
      FoodProperties foodProperties = (FoodProperties)stack.get(DataComponents.FOOD);
      if (foodProperties == null) {
         return false;
      }
      if (!foodProperties.canAlwaysEat()) {
         if (ClientAccess.schizoMinecraft.player.getFoodData().getFoodLevel() >= (20)) {
            return false;
         }
      }
      Item chorusFruit = Items.CHORUS_FRUIT;
      if (stack.is(chorusFruit)) {
         if (!this.shouldEscape()) {
            return false;
         }
      }
      ListFilterMode selectionMode = this.modeSetting.getValue();
      Item item = stack.getItem();
      if (!ListFilterMode.matches(selectionMode, item, this.itemsSetting.getValue())) {
         return false;
      }
      SuspiciousStewEffects stewEffects = (SuspiciousStewEffects)stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
      if (stewEffects != null) {
         for (Entry stewEffect : stewEffects.effects()) {
            MobEffect mobEffect = (MobEffect)stewEffect.effect().value();
            if (!mobEffect.isBeneficial()) {
               return false;
            }
         }
      }
      return true;
   }

   private boolean shouldEscape() {
      Boolean autoEscapeEnabled = this.autoEscapeSetting.getValue();
      if (!autoEscapeEnabled) {
         return false;
      }
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      for (Player candidatePlayer : level.players()) {
         LocalPlayer localPlayer = ClientAccess.schizoMinecraft.player;
         float distanceToLocalPlayer = candidatePlayer.distanceTo(localPlayer);
         float escapeRadius = 12.0F;
         if (distanceToLocalPlayer <= escapeRadius) {
            SocialRegistry playerExclusionFilter = ClientContext.socialRegistry;
            if (!playerExclusionFilter.isFriendPlayer(candidatePlayer)
               && candidatePlayer != ClientAccess.schizoMinecraft.player) {
               return ClientAccess.schizoMinecraft.player.tickCount <= (50);
            }
         }
      }
      return false;
   }

   private boolean isUntilFullEnabled() {
      return this.untilFullSetting.getValue() && this.untilFullSetting.isVisible();
   }

   private boolean shouldStopEating() {
      if (this.continueUntilFull && this.isUntilFullEnabled()) {
         return false;
      }
      if ((this.hungerSetting.getValue()) != -1) {
         int foodLevel = ClientAccess.schizoMinecraft.player.getFoodData().getFoodLevel();
         Integer hungerThreshold = this.hungerSetting.getValue();
         if (foodLevel <= hungerThreshold) {
            return false;
         }
      }
      if ((this.healthSetting.getValue()) == 0) {
         return true;
      }
      float healthAndAbsorption = EntityUtils.getPlayerHealthWithAbsorption();
      Integer healthThreshold = this.healthSetting.getValue();
      return healthAndAbsorption > healthThreshold.intValue();
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      mainhandModule = (MainhandModule)registry.getModule(MainhandModule.class);
   }

   public AutoEatModule() {
      super("AutoEat", "Eats your food for you.", ModuleCategory.PLAYER, new String[0]);
      DefaultedRegistry itemRegistry = CustomItemRegistries.foodItems;
      Item[] defaultItems = new Item[]{Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE};
      this.itemsSetting = this.add(new RegistrySetSetting<>("Items", itemRegistry, (Object[]) defaultItems));
      this.modeSetting = this.add(new EnumSetting<>("Mode", ListFilterMode.ANY));
      NumberSetting<Integer> health = new NumberSetting<>("Health", 16, 0, 36);
      SettingLabelCondition healthOffBound = SettingLabelCondition.MIN;
      this.healthSetting = this.add(health.setBoundaryLabel("Off", healthOffBound));
      NumberSetting<Integer> hunger = new NumberSetting<>("Hunger", 6, -1, 20);
      SettingLabelCondition hungerOffBound = SettingLabelCondition.MIN;
      this.hungerSetting = this.add(hunger.setBoundaryLabel("Off", hungerOffBound));
      this.untilFullSetting = this.add(new BooleanSetting("UntilFull", false));
      this.pauseBaritoneSetting = this.add(new BooleanSetting("PauseBaritone", false));
      this.autoSwapSetting = this.add(new BooleanSetting("AutoSwap", true).markVisibilityDependency());
      AutoEatModule.SortChoice defaultSort = AutoEatModule.SortChoice.SATURATION;
      this.sortSetting = this.add(new EnumSetting<>("Sort", defaultSort).dependsOn(this.autoSwapSetting));
      this.swapBackSetting = this.add(new BooleanSetting("SwapBack", false).dependsOn(this.autoSwapSetting));
      this.autoEscapeSetting = this.add(new BooleanSetting("AutoEscape", false).markVisibilityDependency());
      this.autoDisableSetting = this.add(new BooleanSetting("AutoDisable", false).dependsOn(this.autoEscapeSetting));
      this.untilFullSetting.addVisibilityPredicate(ignored -> !this.hungerSetting.isAtMinimum());
      this.pauseBaritoneSetting.hideWithoutBaritone();
   }

   private enum SortChoice implements NamedEntry {
      HUNGER("Hunger") {
         @Override
         public double score(ItemStack unusedStack, FoodProperties foodProperties) {
            return foodProperties.nutrition();
         }
      },
      SATURATION("Saturation") {
         @Override
         public double score(ItemStack unusedStack, FoodProperties foodProperties) {
            return foodProperties.saturation();
         }
      },
      HEALTH("Health") {
         @Override
         public double score(ItemStack stack, FoodProperties unusedFoodProperties) {
            if ((Object)stack.get(DataComponents.CONSUMABLE) instanceof Consumable consumable) {
               for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
                  if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect statusEffects) {
                     int regenerationAmplifier = getRegenerationAmplifier(statusEffects);
                     if (regenerationAmplifier > 0) {
                        return regenerationAmplifier;
                     }
                  }
               }
            }

            return 0.0;
         }

         static int getRegenerationAmplifier(ApplyStatusEffectsConsumeEffect statusEffects) {
            for (MobEffectInstance effect : statusEffects.effects()) {
               if (effect.getEffect() == MobEffects.REGENERATION) {
                  return effect.getAmplifier();
               }
            }

            return 0;
         }
      };

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      SortChoice(String displayName) {
         this.displayName = displayName;
      }

      public abstract double score(
         ItemStack stack, FoodProperties foodProperties
      );
   }
}
