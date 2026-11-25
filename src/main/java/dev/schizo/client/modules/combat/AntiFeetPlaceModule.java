/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;


import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.managers.InventoryStateManager;
import dev.schizo.client.utils.inventory.ItemUtils;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.entity.SwingAction;
import dev.schizo.client.render.FeatureBoxRenderer;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.utils.network.ServerSideDeathState;
import dev.schizo.client.managers.WorldStateManager;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.DimensionType;

import java.awt.Color;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AntiFeetPlaceModule extends Module {


   private final Setting<ModeChoice> modeSetting;
   private final Setting<Integer> delaySetting;
   private final Setting<Boolean> airPlaceSetting;
   private final Setting<Boolean> raytraceSetting;
   private final Setting<Boolean> rotateSetting;
   final Setting<Boolean> renderSetting;
   private final Setting<Color> fillSetting;
   private final Setting<Color> outlineSetting;
   private final Setting<Float> lineWidthSetting;
   private final Setting<Boolean> fadeSetting;
   private final Setting<Float> fadeTimeSetting;
   private final ElapsedTimer placementTimer;

   private boolean shouldPauseReplacement() {
      SpeedMineModule positionCheckSource = schizo$speedMine();

      BlockPos miningPosition;
      if (positionCheckSource.getMiningPos() != null) {
         miningPosition = schizo$speedMine().getMiningPos();
      } else {
         miningPosition = schizo$speedMine().getRebreakPos();
      }

      if (!schizo$speedMine().isToggled() || miningPosition == null) {
         return true;
      }

      double miningProgress = schizo$speedMine().getRenderProgress();
      if (miningProgress < 0.95D && !WorldUtils.isAir(miningPosition)) {
         return true;
      }

      if (schizo$multiTask().shouldPauseModule(this)) {
         return true;
      }
      ElapsedTimer timerSnapshot = this.placementTimer;

      long delayMillis = (this.delaySetting.getValue()).intValue();

      if (!timerSnapshot.hasElapsed(delayMillis)) {
         return true;
      }
      if (!BlockInteractionUtils.canPlayerInteract() || PlayerStateUtils.isFallFlying()) {
         return true;
      }
      return OffhandModule.isRecentSwapWindowActive() || SpeedMineModule.isSwapPending();
   }

   private void replaceMinedBlock() {
      if (!this.shouldPauseReplacement()) {
         SpeedMineModule positionCheckSource = schizo$speedMine();

         BlockPos miningPosition;
         if (positionCheckSource.getMiningPos() != null) {
            miningPosition = schizo$speedMine().getMiningPos();
         } else {
            miningPosition = schizo$speedMine().getRebreakPos();
         }
         AABB placementBounds = new AABB(miningPosition);

         if (!this.hasNoEligibleEnemy(miningPosition)) {
            if (!BlockInteractionUtils.isOutsidePlacementBounds(miningPosition)) {
               if (!BlockInteractionUtils.hasBlockingEntity(placementBounds, true, true)) {
                  BlockPos belowPositionSnapshot = miningPosition.below();

                  if (!WorldUtils.isBedrockOrObsidian(belowPositionSnapshot)) {
                     return;
                  }

                  boolean strictDirectionEnabled = BlockInteractionUtils.isStrictDirectionEnabled();

                  boolean raytraceFlag = this.raytraceSetting.getValue();
                  boolean supportStrictFlag = strictDirectionEnabled;

                  Direction supportDirection = BlockInteractionUtils.findSupportDirection(
                     miningPosition, supportStrictFlag, raytraceFlag
                  );
                  if (supportDirection == null) {
                     if (!this.airPlaceSetting.getValue()) {
                        return;
                     }
                  }

                  int previousSlot = InventoryUtils.getSelectedSlot();

                  InventoryUtils.SwapSlot selectedItem = this.selectPlacementItem();
                  Runnable placementCallback = () -> {
                        InteractionHand interactionHand = selectedItem.hand();

                        Direction callbackDirection = supportDirection;
                        this.placeReplacement(miningPosition, callbackDirection, interactionHand);
                     };
                  InventoryUtils.runWithSwap(previousSlot, selectedItem, placementCallback);
                  return;
               }
            }
         }
      }
   }

   private void placeReplacement(
      BlockPos position, Direction supportDirection, InteractionHand hand
   ) {
      this.placementTimer.reset();

      boolean airPlaceEnabled = this.airPlaceSetting.getValue();
      BlockInteractionUtils.sendPlacement(position, null, supportDirection, airPlaceEnabled, hand);
      SwingAction swingKind = SwingAction.INTERACT;
      InteractionHand swingHand = InteractionHand.MAIN_HAND;

      ClientContext.swingManager.swing(swingHand, swingKind);

      if (this.renderSetting.getValue()) {
         ClientContext.featureBoxRenderer.addBox(this, position);
      }

      if (this.rotateSetting.getValue()) {
         ClientContext.rotationManager.requestRotation(RotationManager.getRotationsToBlock(position), 350);
      }
   }

   private boolean hasNoEligibleEnemy(BlockPos position) {
      Iterator<AbstractClientPlayer> players = ClientAccess.schizoMinecraft.level.players().iterator();

      while (players.hasNext()) {
         AbstractClientPlayer target = players.next();
         if (!CombatTargetPolicy.isEligible(
            target.isAlive(),
            target.isDeadOrDying(),
            target == ClientAccess.schizoMinecraft.player,
            target.isSpectator(),
            target.isCreative(),
            ClientContext.socialRegistry.isFriendPlayer(target),
            ((ServerSideDeathState)target).isServerSideDead()
         )) {
            continue;
         }

         double distanceSquared = target.distanceToSqr(position.getX(), position.getY(), position.getZ());
         if (distanceSquared > 36.0D || !PlayerStateUtils.hasArmor(target)) {
            continue;
         }

         Set<BlockPos> candidatePositions = PlayerStateUtils.getSurroundPositions(target);
         if (!candidatePositions.contains(position)) {
            continue;
         }

         boolean hasAdjacentAir = false;
         for (BlockPos feetPosition : PlayerStateUtils.getOccupiedBlockPositions(target)) {
            BlockState state = ClientAccess.schizoMinecraft.level.getBlockState(feetPosition);
            if (state.isAir() && feetPosition.distManhattan(position) == 1) {
               hasAdjacentAir = true;
               break;
            }
         }

         if (hasAdjacentAir) {
            return false;
         }
      }

      return true;
   }

   private InventoryUtils.SwapSlot selectPlacementItem() {
      AntiFeetPlaceModule.ModeChoice mode = this.modeSetting.getValue();

      int hotbarSlot = InventoryUtils.findHotbarSlot(mode.itemPredicate);

      if (this.isHoldingPlacementItemOffhand()) {
         return new InventoryUtils.SwapSlot(
            InventoryUtils.getSelectedSlot(),
            true,
            InteractionHand.OFF_HAND
         );
      }

      if (hotbarSlot == -1) {
         if (!InventoryStateManager.wouldReachClickLimit(ClientContext.inventoryStateManager, 3)) {
            return new InventoryUtils.SwapSlot(
               InventoryUtils.findInventorySlot(mode.itemPredicate, true),
               false,
               InteractionHand.MAIN_HAND
            );
         }
      }

      return new InventoryUtils.SwapSlot(hotbarSlot, true, InteractionHand.MAIN_HAND);
   }

   private boolean isHoldingPlacementItemOffhand() {
      return (this.modeSetting.getValue()).itemPredicate.test(ClientAccess.schizoMinecraft.player.getOffhandItem());
   }

   @EventHandler(priority = -100)
   public void onPlayerUpdate(RotationRequestEvent event) {
      this.replaceMinedBlock();
   }

   public static void bindModules(ModuleRegistry modules) {
      schizo$speedMine() = (SpeedMineModule)modules.getModule(
         SpeedMineModule.class
      );
      schizo$multiTask() = (MultiTaskModule)modules.getModule(
         MultiTaskModule.class
      );
   }

   private static String createDescription() {
      String warningColor = String.valueOf(ChatFormatting.RED);
      return "Tries to replace the enemy's feet block with a faster-breaking one.\n" + warningColor + "Requires SpeedMine module enabled.";
   }

   public AntiFeetPlaceModule() {
      super("AntiFeetPlace", createDescription(), ModuleCategory.COMBAT, new String[0]);
      this.modeSetting = this.add(new EnumSetting<>("Mode", ModeChoice.GLASS));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 0, 0, 500));
      this.airPlaceSetting = this.add(new BooleanSetting("AirPlace", false));
      this.raytraceSetting = this.add(new BooleanSetting("Raytrace", false));
      this.rotateSetting = this.add(new BooleanSetting("Rotate", false));
      this.renderSetting = this.add(new BooleanSetting("Render", false).markVisibilityDependency());
      Predicate fillVisibility = unused -> this.renderSetting.isExpanded();
      this.fillSetting = this.add(new ColorSetting("Fill", new Color(146643455, true), fillVisibility));
      Predicate outlineVisibility = unused -> this.renderSetting.isExpanded();
      this.outlineSetting = this.add(new ColorSetting("Outline", new Color(1539152383, true), outlineVisibility));
      this.lineWidthSetting = this.add(new NumberSetting<>("LineWidth", 1.0F, 0.1F, 3.0F).dependsOn(this.renderSetting));
      this.fadeSetting = this.add(new BooleanSetting("Fade", true).dependsOn(this.renderSetting));
      this.fadeTimeSetting = this.add(new NumberSetting<>("FadeTime", 1.0F, 0.1F, 1.0F)
         .withUnit("s").dependsOn(this.renderSetting, this.fadeSetting));
      this.placementTimer = new ElapsedTimer();
      FeatureBoxRenderer rendererSnapshot = ClientContext.featureBoxRenderer;
      FeatureBoxRenderer.RenderConfiguration renderConfiguration = new FeatureBoxRenderer.RenderConfiguration(
         this, this.fillSetting, this.outlineSetting, this.lineWidthSetting, this.fadeTimeSetting,
         () -> true, this.fadeSetting, 450
      );

      rendererSnapshot.register(renderConfiguration);
   }

   private enum ModeChoice implements NamedEntry {
      GLASS("Glass", (java.util.function.Predicate<net.minecraft.world.item.ItemStack>)(stack -> {
         Item itemSnapshot = stack.getItem();

         return ItemUtils.isGlass(itemSnapshot);
      })),
      ICE("Ice", (java.util.function.Predicate<net.minecraft.world.item.ItemStack>)(stack -> {
         Item packedIce = Items.PACKED_ICE;

         if (!stack.is(packedIce)) {
            WorldStateManager trackerSnapshot = ClientContext.worldStateManager;

            DimensionType dimension = trackerSnapshot.getDimension();
            if (!dimension.isNether()) {
               Item ice = Items.ICE;
               if (stack.is(ice)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      })),
      ANVIL("Anvil", (java.util.function.Predicate<net.minecraft.world.item.ItemStack>)(stack -> {
         Item anvil = Items.ANVIL;

         return stack.is(anvil);
      }));

      private final String displayName;
      private final Predicate itemPredicate;

      @Override
      public String getName() {
         return this.displayName;
      }

      ModeChoice(String displayName, Predicate itemPredicate) {
         this.displayName = displayName;
         this.itemPredicate = itemPredicate;
      }
   }

   private static SpeedMineModule schizo$speedMine() {
      return ClientContext.requireModule(SpeedMineModule.class);
   }

   private static MultiTaskModule schizo$multiTask() {
      return ClientContext.requireModule(MultiTaskModule.class);
   }
}
