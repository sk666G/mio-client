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
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.utils.collections.IntPair;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.settings.SettingLabelCondition;
import dev.schizo.client.utils.world.WorldUtils;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.DimensionType;
import dev.schizo.client.settings.options.HudAnchor;
import dev.schizo.client.utils.math.MathUtils;

import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

public class PositionModule extends HudWidgetModule {
   private static FreecamModule freecam() {
      return ClientContext.getModule(FreecamModule.class);
   }
   private final Setting<Float> safeRangeSetting;
   private final Setting<Boolean> netherSetting;
   private final Setting<Boolean> prefixSetting;
   private final Setting<Boolean> fakeSetting;
   private final ElapsedTimer redactionTimer;
   private IntPair fakeOffset;

   private boolean isRedactionGraceActive() {
      int playerTicks = ClientAccess.schizoMinecraft.player.tickCount;

      if (playerTicks
         >= 200) {
         ElapsedTimer timer = this.redactionTimer;

         double delaySeconds = 5.0D;
         TimeUnit delayUnit = TimeUnit.SECONDS;
         if (timer.hasElapsed(delaySeconds, delayUnit)) {
            return false;
         }
      }

      return true;
   }

   

   private String getCoordinatePrefix() {
      Setting<Boolean> prefixOption = this.prefixSetting;
      Boolean showPrefix = prefixOption.getValue();
      if (!showPrefix) {
         return "";
      }

      return "XYZ ";
   }

   private void refreshFakeOffset() {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player == null) {
         this.fakeOffset = null;
         return;
      }

      boolean negateX = MathUtils.passesRandomThreshold(50);
      boolean negateZ = MathUtils.passesRandomThreshold(50);
      double playerX = player.getX();
      double playerZ = player.getZ();
      int fakeX = (int)(-playerX * 2.0D + Math.random() * playerX * 4.0D) + 13;
      int fakeZ = (int)(-playerZ * 2.0D + Math.random() * playerZ * 4.0D) - 37;
      if (negateX) {
         fakeX = -fakeX;
      }
      if (negateZ) {
         fakeZ = -fakeZ;
      }
      this.fakeOffset = new IntPair(fakeX, fakeZ);
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
      if (this.fakeSetting.getValue() && this.fakeOffset == null) {
         this.refreshFakeOffset();
      }
   }

   @EventHandler
   private void onMinecraftDisconnectEvent(MinecraftDisconnectEvent event) {
      this.fakeOffset = null;
   }

   @EventHandler
   private void onPlayerPositionPacket(
      PacketEvent.Receive event
   ) {
      if (!this.isWorldUnavailable()) {
         if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            PositionMoveRotation positionChange = packet.change();
            Vec3 packetPosition = positionChange.position();
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            Vec3 playerPosition = player.position();
            double distanceSquared = packetPosition.distanceToSqr(playerPosition);

            if (distanceSquared >= 10000.0D) {
               this.redactionTimer.reset();
            }
         }
      }
   }

   public PositionModule() {
      super("Position", new String[]{"coordinates"});

      NumberSetting<Float> safeRangeOption = new NumberSetting<>(
         "SafeRange", 0.0F, 0.0F, 100.0F
      );
      String safeRangeUnit = "k";
      NumberSetting<Float> safeRangeWithUnit = safeRangeOption.withUnit(safeRangeUnit);
      this.safeRangeSetting = this.add(safeRangeWithUnit);

      this.netherSetting = this.add(new BooleanSetting("Nether", true));

      this.prefixSetting = this.add(new BooleanSetting("Prefix", true));

      this.fakeSetting = this.add(new BooleanSetting("Fake", false));
      this.redactionTimer = new ElapsedTimer();
      Setting<Float> safeRangeForDisplay = this.safeRangeSetting;

      SettingLabelCondition minimumDisplayBound = SettingLabelCondition.MIN;
      String minimumDisplayText = "None";
      safeRangeForDisplay.setBoundaryLabel(minimumDisplayText, minimumDisplayBound);
      Setting<Boolean> fakeCallbackOption = this.fakeSetting;
      Runnable fakeCallback = () -> {
         if (!this.fakeSetting.getValue()) {
            this.fakeOffset = null;
            return;
         }

         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (!this.fakeSetting.getValue()) {
               this.fakeOffset = null;
               return;
            }
            this.refreshFakeOffset();
         });
      };
      fakeCallbackOption.setChangeCallback(fakeCallback);
      HudTextRow coordinateRow = new HudTextRow(
         () -> {
            coordinateVisibility:
            if (!this.isWorldUnavailable()) {
               Setting<Float> safeRangeOptionForEnabled = this.safeRangeSetting;
               Float safeRangeEnabledValue = safeRangeOptionForEnabled.getValue();
               if (safeRangeEnabledValue > 0.0F) {
                  Vec3 positionForSafeRange = ClientAccess.schizoMinecraft.player.position();
                  Vec3 origin = Vec3.ZERO;
                  Setting<Float> safeRangeOptionForDistance = this.safeRangeSetting;
                  Float safeRangeValue = safeRangeOptionForDistance.getValue();
                  float safeRangeThousands = safeRangeValue;

                  double safeRangeDistance = safeRangeThousands * 1000.0F;
                  if (!positionForSafeRange.closerThan(origin, safeRangeDistance)) {

                     if (this.isRedactionGraceActive()) {
                        break coordinateVisibility;
                     }
                  }
               }

               ClientLevel level = ClientAccess.schizoMinecraft.level;
               ResourceKey<?> dimension = level.dimension();
               Identifier dimensionIdentifier = dimension.identifier();
               String dimensionPath = dimensionIdentifier.getPath();

               boolean inNether = dimensionPath.contains("nether");
               Vec3 displayPosition = ClientAccess.schizoMinecraft.player.position();
               FreecamModule freecamForEnabledCheck = freecam();
               if (freecamForEnabledCheck.isToggled()) {
                  Camera camera = ClientAccess.schizoMinecraft.gameRenderer.getMainCamera();
                  Vec3 cameraPosition = camera.position();
                  double playerHeight = ClientAccess.schizoMinecraft.player.getBbHeight();
                  displayPosition = cameraPosition.subtract(0.0D, playerHeight, 0.0D);
               }

               Setting<Boolean> fakeDisplayOption = this.fakeSetting;
               Boolean useFakeOffset = fakeDisplayOption.getValue();
               if (useFakeOffset) {
                  IntPair fakeOffset = this.fakeOffset;
                  if (fakeOffset == null) {
                     break coordinateVisibility;
                  }
                  if (fakeOffset.first() == 0 && fakeOffset.second() == 0) {
                     this.fakeOffset = null;
                     this.fakeSetting.runChangeCallback();
                     break coordinateVisibility;
                  }

                  displayPosition = displayPosition.add(
                     fakeOffset.first(), 0.0D, fakeOffset.second()
                  );
               }

               BlockPos currentBlockPosition = BlockPos.containing(displayPosition);
               BlockPos convertedBlockPosition;
               if (inNether) {

                  Vec3 convertedPosition = displayPosition.multiply(8.0D, 1.0D, 8.0D);
                  convertedBlockPosition = BlockPos.containing(convertedPosition);
               } else {

                  Vec3 convertedPosition = displayPosition.multiply(0.125D, 1.0D, 0.125D);
                  convertedBlockPosition = BlockPos.containing(convertedPosition);
               }

               String coordinatePrefix = this.getCoordinatePrefix();
               ChatFormatting prefixColor = ChatFormatting.WHITE;
               String prefixColorText = String.valueOf(prefixColor);
               String formattedPrefix = coordinatePrefix + prefixColorText;
               int currentX = currentBlockPosition.getX();
               int currentY = currentBlockPosition.getY();
               int currentZ = currentBlockPosition.getZ();
               String coordinateText = currentX + ", " + currentY + ", " + currentZ;
               int convertedX = convertedBlockPosition.getX();
               int convertedZ = convertedBlockPosition.getZ();
               String convertedText = " (" + convertedX + ", " + convertedZ + ")";
               Setting<Boolean> netherDisplayOption = this.netherSetting;
               Boolean showConvertedPosition = netherDisplayOption.getValue();
               if (showConvertedPosition) {

                  if (WorldUtils.getDimension()
                     != DimensionType.THE_END) {
                     coordinateText = coordinateText + convertedText;
                  }
               }

               String positionText = formattedPrefix + coordinateText;
               return Component.literal(positionText);
            }

            Object[] redactedFormatArguments = new Object[2];
            redactedFormatArguments[0] = this.getCoordinatePrefix();
            redactedFormatArguments[1] = ChatFormatting.WHITE;
            String redactedText = "%s%sREDACTED".formatted(redactedFormatArguments);
            return Component.literal(redactedText);
         },
         () -> true
      );
      this.setRenderer(new HudTextListRenderer(this, coordinateRow));

      this.getRenderer()
         .setAnchor(HudAnchor.BOTTOM_LEFT);
   }

}
