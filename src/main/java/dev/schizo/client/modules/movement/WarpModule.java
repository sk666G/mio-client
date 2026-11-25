/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;


import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.LocalPlayerSendPositionEvent;
import dev.schizo.client.managers.TickRateController;
import dev.schizo.client.events.EventStage;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.WarpRechargeChoice;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;

public class WarpModule extends Module {
   private static FakeLagModule fakeLag;

   protected final Setting<ModeChoice> modeSetting;
   public final Setting<Integer> boostSetting;
   public final Setting<Integer> chargeSetting;
   protected final Setting<Integer> chargeSpeedSetting;
   protected final Setting<WarpRechargeChoice> rechargeSetting;
   protected final Setting<Boolean> fakeLagSetting;
   protected final Setting<Boolean> disableGroundSetting;
   protected final Setting<Boolean> autoDisableSetting;
   protected final WarpRechargeController rechargeController;
   protected volatile boolean receivedCorrection;
   protected volatile int plainCharge;

   public int getCharge() {
      return this.modeSetting.getValue() == WarpModule.ModeChoice.ALTERNATIVE
         ? this.rechargeController.pendingPongs.size()
         : this.plainCharge;
   }

   private void updatePlainWarp() {
      if (!this.hasDisplacement()) {
         ClientContext.tickRateController.releaseMultiplier(this);
         this.setFakeLagEnabled(false);
         this.receivedCorrection = false;
      } else if (this.plainCharge == 0
         || this.receivedCorrection) {
         ClientContext.tickRateController.releaseMultiplier(this);
         if (this.autoDisableSetting.getValue()) {
            this.toggle();
         }
         this.setFakeLagEnabled(false);
      } else if (this.plainCharge > 0) {
         if (this.fakeLagSetting.getValue()) {
            boolean wasFakeLagEnabled = fakeLag.isToggled();
            LocalPlayer groundPlayer = ClientAccess.schizoMinecraft.player;
            boolean enableFakeLag = true;
            if (!groundPlayer.onGround()) {
               Boolean requireGround = this.disableGroundSetting.getValue();
               enableFakeLag = !requireGround;
            }
            this.setFakeLagEnabled(enableFakeLag);
            if (!wasFakeLagEnabled) {
               return;
            }
         }

         TickRateController boostControllerSnapshot = ClientContext.tickRateController;
         float boost = (this.boostSetting.getValue()).intValue();
         boostControllerSnapshot.requestMultiplier(this, boost);
      }
   }

   protected boolean hasMovementInputOrDisplacement() {
      if (ClientAccess.schizoMinecraft.player.input.getMoveVector().length() > 0.0F) {
         return true;
      }
      if (ClientAccess.schizoMinecraft.player.getX() - ClientAccess.schizoMinecraft.player.xo != 0.0) {
         return true;
      }
      if (ClientAccess.schizoMinecraft.player.getY() - ClientAccess.schizoMinecraft.player.yo != 0.0) {
         return true;
      }
      return ClientAccess.schizoMinecraft.player.getZ() - ClientAccess.schizoMinecraft.player.zo != 0.0;
   }

   private void updateAlternativeWarp() {
      if (!this.rechargeController.pendingPongs.isEmpty()) {
         if (this.hasMovementInputOrDisplacement()) {
            TickRateController.PlayerTicks boostControllerSnapshot = ClientContext.playerTickController;
            float boost = (this.boostSetting.getValue()).intValue();
            boostControllerSnapshot.requestMultiplier(this, boost);
            ClientCommonPacketListenerImpl connection = ClientAccess.schizoMinecraft.player.connection;
            Packet<?> packet = this.rechargeController.pendingPongs.poll();
            connection.send(packet);
            return;
         }
      }

      if (!this.rechargeController.pendingPongs.isEmpty()
         && !this.receivedCorrection) {
         ClientContext.playerTickController.releaseMultiplier(this);
      } else {
         ClientContext.playerTickController.releaseMultiplier(this);
         if (this.autoDisableSetting.getValue()) {
            this.toggle();
         }
      }
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.getCharge());
   }

   @Override
   public void onEnable() {
      this.receivedCorrection = false;
      if (this.rechargeSetting.getValue() == WarpRechargeChoice.INSTANT) {
         Integer instantCharge = this.chargeSetting.getValue();
         this.plainCharge = instantCharge;
      }
   }

   @Override
   public void onDisable() {
      this.releaseTickRateRequests();
      this.setFakeLagEnabled(false);
      this.flushPendingPongs();
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.releaseTickRateRequests();
      this.setFakeLagEnabled(false);
      this.rechargeController.pendingPongs.clear();
      this.plainCharge = 0;
      this.receivedCorrection = false;
   }

   private void onModeChanged() {
      this.releaseTickRateRequests();
      this.setFakeLagEnabled(false);
      this.flushPendingPongs();
   }

   private void flushPendingPongs() {
      if (this.isWorldUnavailable()) {
         this.rechargeController.pendingPongs.clear();
         return;
      }

      try {
         while (!this.rechargeController.pendingPongs.isEmpty()) {
            ClientCommonPacketListenerImpl connection = ClientAccess.schizoMinecraft.player.connection;
            Packet<?> packet = this.rechargeController.pendingPongs.poll();
            connection.send(packet);
         }
      } catch (Exception ignored) {
      } finally {
         this.rechargeController.pendingPongs.clear();
      }
   }

   private void setFakeLagEnabled(boolean enabled) {
      if (this.fakeLagSetting.getValue() && ClientAccess.schizoMinecraft.player != null) {
         fakeLag.setToggled(enabled);
      }
   }

   boolean hasDisplacement() {
      Vec3 positionSnapshot = ClientAccess.schizoMinecraft.player.position();
      double previousX = ClientAccess.schizoMinecraft.player.xo;
      double previousY = ClientAccess.schizoMinecraft.player.yo;
      double previousZ = ClientAccess.schizoMinecraft.player.zo;
      return positionSnapshot.distanceToSqr(previousX, previousY, previousZ) > 0.0;
   }

   @EventHandler
   private void onIncomingPacket(
      PacketEvent.Receive event
   ) {
      if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
         this.receivedCorrection = true;
      }
   }

   @EventHandler
   private void onSendPosition(LocalPlayerSendPositionEvent event) {
      if (EventStage.Pre == event.getStage()) {
         if (this.modeSetting.getValue() == WarpModule.ModeChoice.ALTERNATIVE) {
            this.updateAlternativeWarp();
         } else {
            this.updatePlainWarp();
         }
      }
   }

   public static void bindReferencedModules(ModuleRegistry registry) {
      fakeLag = (FakeLagModule)registry.getModule(FakeLagModule.class);
   }

   public WarpModule() {
      super("Warp", "Allows you to dash forward after standing still for a certain amount of time.", ModuleCategory.MOVEMENT, new String[]{"tickshift"});
      this.modeSetting = this.add(new EnumSetting<>("Mode", WarpModule.ModeChoice.PLAIN));
      this.boostSetting = this.add(new NumberSetting<>("Boost", 2, 1, 50));
      this.chargeSetting = this.add(new NumberSetting<>("Charge", 20, 1, 100));
      Predicate<Object> chargeSpeedVisibility = ignored -> this.modeSetting.getValue() == ModeChoice.PLAIN;
      this.chargeSpeedSetting = this.add(new NumberSetting<>("ChargeSpeed", 1, 1, 4, chargeSpeedVisibility));
      Predicate<Object> rechargeVisibility = ignored -> this.modeSetting.getValue() == ModeChoice.PLAIN;
      this.rechargeSetting = this.add(new EnumSetting<>("Recharge", WarpRechargeChoice.NONE, rechargeVisibility));
      this.fakeLagSetting = this.add(new BooleanSetting("FakeLag", false).markVisibilityDependency());
      this.disableGroundSetting = this.add(new BooleanSetting("DisableGround", false).dependsOn(this.fakeLagSetting));
      this.autoDisableSetting = this.add(new BooleanSetting("AutoDisable", false));
      this.rechargeController = new WarpRechargeController(this);
      this.modeSetting.setChangeCallback(this::onModeChanged);
   }

   private void releaseTickRateRequests() {
      ClientContext.tickRateController.releaseMultiplier(this);
      ClientContext.playerTickController.releaseMultiplier(this);
   }

   public enum ModeChoice implements NamedEntry {
      PLAIN("Plain"),
      ALTERNATIVE("Alternative");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      ModeChoice(String name) {
         this.displayName = name;
      }
   }
}
