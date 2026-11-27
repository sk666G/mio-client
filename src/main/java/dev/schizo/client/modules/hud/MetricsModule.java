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
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.managers.ServerStateManager;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.utils.FrameTiming;
import dev.schizo.client.managers.WorldStateManager;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.HudAnchor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class MetricsModule extends HudWidgetModule {
   private static final Comparator<HudTextRow> METRIC_WIDTH_COMPARATOR =
      Comparator.comparingDouble(row -> measureMetricWidth(FontRenderer.INSTANCE, row));

   private final Setting<Boolean> speedSetting;
   private final Setting<Boolean> bpsSetting;
   private final Setting<Boolean> tpsSetting;
   private final Setting<Boolean> pingSetting;
   private final Setting<Boolean> fpsSetting;
   private final Setting<Boolean> averageFpsSetting;
   private final Setting<Boolean> frameTimeSetting;
   private final Setting<Boolean> averageFrameTimeSetting;
   private final Setting<Boolean> onePercentLowSetting;
   private final Setting<Boolean> oneTenthPercentLowSetting;
   private final Setting<Boolean> frameVarianceSetting;
   private final Setting<Boolean> hitchesSetting;
   private final Setting<Boolean> serverBrandSetting;
   private final Setting<Boolean> durabilitySetting;
   private final Setting<Boolean> shulkersSetting;
   private final Setting<Boolean> chestSetting;
   private final Setting<Boolean> doubleChestSetting;
   private final List<HudTextRow> metricRows;
   private Component cachedDurabilityText;
   private int chestAndBarrelCount;
   private int shulkerCount;

   private static Component formatStackDurability(ItemStack stack) {
      int maximumDamage = stack.getMaxDamage();
      int remainingDurability = maximumDamage - stack.getDamageValue();
      float remainingFraction = (float)remainingDurability / maximumDamage;
      Color durabilityColor = ColorUtils.redToGreen(remainingFraction);
      int textColor = durabilityColor.hashCode();
      double fractionForNaNCheck = remainingFraction;
      if (Double.isNaN(fractionForNaNCheck)) {
         textColor = -1;
      }
      String text = String.valueOf(remainingDurability);
      MutableComponent component = Component.literal(text);
      int colorArgument = textColor;
      return component.withColor(colorArgument);
   }

   private Component getHeldItemDurability() {
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      if (player == null) {
         return this.cachedDurabilityText != null ? this.cachedDurabilityText : Component.empty();
      }
      ItemStack heldStack = player.getMainHandItem();

      Component durabilityText = formatStackDurability(heldStack);
      if (heldStack.getMaxDamage() == 0 && this.cachedDurabilityText != null) {
         return this.cachedDurabilityText;
      }

      this.cachedDurabilityText = durabilityText;
      return durabilityText;
   }


   @Override
   public void onEnable() {
      this.metricRows.clear();
      List<HudTextRow> rowsForRegistration = this.metricRows;
      HudTextRow speedRow = new HudTextRow(() -> {
         Setting<Boolean> bpsOptionForSuffix = this.bpsSetting;
         Boolean useBpsSuffix = bpsOptionForSuffix.getValue();
         return formatSpeedMetric(this, useBpsSuffix);
      }, this.speedSetting::getValue);
      HudTextRow tpsRow = new HudTextRow(() -> {

         Object[] formatArguments = new Object[2];
         formatArguments[0] = ChatFormatting.WHITE;
         ServerStateManager networkTracker = ClientContext.serverStateManager;

         float ticksPerSecond = networkTracker.getTickRate();
         formatArguments[1] = ticksPerSecond;
         String format = "TPS %s%.2f";
         String formattedText = format.formatted(formatArguments);
         String text = formattedText.replace(",", ".");
         return Component.literal(text);
      }, this.tpsSetting::getValue);
      HudTextRow pingRow = new HudTextRow(() -> {
         ServerStateManager networkTracker = ClientContext.serverStateManager;
         return Component.literal(formatColoredInteger(
            "Ping ", networkTracker.getLatencyMillis(), "ms"
         ));
      }, this.pingSetting::getValue);
      HudTextRow fpsRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal(formatColoredInteger(
            "FPS ", frameTiming.getFramesPerSecond(), ""
         ));
      }, this.fpsSetting::getValue);
      HudTextRow averageFpsRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("AvgFPS %s%.1f".formatted(
            ChatFormatting.WHITE, frameTiming.getAverageFramesPerSecond()
         ));
      }, this.averageFpsSetting::getValue);
      HudTextRow frameTimeRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("Frame %s%.2fms".formatted(
            ChatFormatting.WHITE, frameTiming.getFrameTimeMillis()
         ));
      }, this.frameTimeSetting::getValue);
      HudTextRow averageFrameTimeRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("AvgFrame %s%.2fms".formatted(
            ChatFormatting.WHITE, frameTiming.getAverageFrameTimeMillis()
         ));
      }, this.averageFrameTimeSetting::getValue);
      HudTextRow onePercentLowRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("1%% Low %s%.0f".formatted(
            ChatFormatting.WHITE, frameTiming.getOnePercentLowFramesPerSecond()
         ));
      }, this.onePercentLowSetting::getValue);
      HudTextRow oneTenthPercentLowRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("0.1%% Low %s%.0f".formatted(
            ChatFormatting.WHITE, frameTiming.getOneTenthPercentLowFramesPerSecond()
         ));
      }, this.oneTenthPercentLowSetting::getValue);
      HudTextRow frameVarianceRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("FrameVariance %s%.2fms²".formatted(
            ChatFormatting.WHITE, frameTiming.getFrameTimeVarianceMillisSquared()
         ));
      }, this.frameVarianceSetting::getValue);
      HudTextRow hitchesRow = new HudTextRow(() -> {
         FrameTiming frameTiming = ClientContext.frameTiming;
         return Component.literal("Hitches %s%d/s".formatted(
            ChatFormatting.WHITE, frameTiming.getStuttersPerSecond()
         ));
      }, this.hitchesSetting::getValue);
      HudTextRow serverBrandRow = new HudTextRow(() -> {
         ClientPacketListener connection = ClientAccess.schizoMinecraft.getConnection();
         String serverBrand = connection == null || connection.serverBrand() == null
            ? "N/A"
            : connection.serverBrand();
         return Component.literal("ServerBrand " + ChatFormatting.WHITE + serverBrand);
      }, this.serverBrandSetting::getValue);
      HudTextRow durabilityRow = new HudTextRow(
         () -> formatDurabilityMetric(this),
         () -> {
            Setting<Boolean> durabilityOption = this.durabilitySetting;
            Boolean showDurability = durabilityOption.getValue();
            if (!showDurability) {
               return false;
            }
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            return player != null && player.getMainHandItem().isDamageableItem();
         }
      );
      HudTextRow chestRow = new HudTextRow(
         () -> Component.literal(formatColoredInteger(
            "Chests ", this.chestAndBarrelCount, ""
         )),
         this.chestSetting::getValue
      );
      HudTextRow shulkerRow = new HudTextRow(
         () -> Component.literal(formatColoredInteger(
            "Shulkers ", this.shulkerCount, ""
         )),
         this.shulkersSetting::getValue
      );

      rowsForRegistration.addAll(List.of(
         speedRow, tpsRow, pingRow, fpsRow, averageFpsRow, frameTimeRow, averageFrameTimeRow,
         onePercentLowRow, oneTenthPercentLowRow, frameVarianceRow, hitchesRow, serverBrandRow,
         durabilityRow, chestRow, shulkerRow
      ));
   }

   @Override
   public void onDisable() {
      this.clearSessionState();
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.clearSessionState();
   }

   private void clearSessionState() {
      this.cachedDurabilityText = null;
      this.chestAndBarrelCount = 0;
      this.shulkerCount = 0;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent ignoredEvent) {
      this.metricRows.sort(METRIC_WIDTH_COMPARATOR);
      this.shulkerCount = this.chestAndBarrelCount = 0;
      boolean countChests = this.chestSetting.getValue();
      boolean countShulkers = this.shulkersSetting.getValue();
      if (countChests || countShulkers) {
         WorldStateManager worldTracker = ClientContext.worldStateManager;
         countContainers(this, worldTracker, countChests, countShulkers);
      }
   }

   static String formatColoredInteger(String prefix, int value, String suffix) {
      return prefix + ChatFormatting.WHITE + value + suffix;
   }

   private static Component formatSpeedMetric(MetricsModule owner, boolean useBpsSuffix) {
      String speedTemplate = "Speed %s%.2f" + (useBpsSuffix ? "b/s" : "km/h");

      Object[] formatArguments = new Object[2];
      formatArguments[0] = ChatFormatting.WHITE;
      double displaySpeed = ClientContext.movementSpeedTracker.getDisplaySpeed();
      Setting<Boolean> bpsOptionForValue = owner.bpsSetting;
      Boolean useBpsValue = bpsOptionForValue.getValue();
      double unitDivisor;
      if (useBpsValue) {

         unitDivisor = 3.6D;
      } else {

         unitDivisor = 1.0D;
      }

      double speedValue = displaySpeed / unitDivisor;
      formatArguments[1] = speedValue;
      String formattedText = speedTemplate.formatted(formatArguments);
      String text = formattedText.replace(",", ".");
      return Component.literal(text);
   }

   private static Component formatDurabilityMetric(MetricsModule owner) {
      String label = "Durability ";
      MutableComponent labelComponent = Component.literal(label);

      Component durabilityText = owner.getHeldItemDurability();
      return labelComponent.append(durabilityText);
   }

   private static float measureMetricWidth(FontRenderer textRenderer, HudTextRow row) {
      Component rowText = row.getText();
      return -textRenderer.getWidth(rowText.getString());
   }

   private static void countContainers(
      MetricsModule owner, WorldStateManager worldTracker, boolean countChests, boolean countShulkers
   ) {
      for (BlockEntity blockEntity : worldTracker.getBlockEntities()) {
         if (countChests && (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity)) {
            owner.chestAndBarrelCount++;
         }

         if (countShulkers && blockEntity instanceof ShulkerBoxBlockEntity) {
            owner.shulkerCount++;
         }
      }

      if (countChests && owner.doubleChestSetting.getValue()) {
         int combinedContainerCount = owner.chestAndBarrelCount;
         owner.chestAndBarrelCount = combinedContainerCount / 2;
      }
   }

   private static String[] createAliases() {
      String[] aliases = new String[2];
      aliases[0] = "infohud";
      aliases[1] = "info";
      return aliases;
   }

   public MetricsModule() {
      super("Metrics", createAliases());

      this.speedSetting = this.add(new BooleanSetting("Speed", true).markVisibilityDependency());
      this.bpsSetting = this.add(new BooleanSetting("BPS", false).dependsOn(new Setting<?>[]{this.speedSetting}));
      this.tpsSetting = this.add(new BooleanSetting("TPS", true));
      this.pingSetting = this.add(new BooleanSetting("Ping", true));
      this.fpsSetting = this.add(new BooleanSetting("FPS", true));
      this.averageFpsSetting = this.add(new BooleanSetting("AverageFPS", false));
      this.frameTimeSetting = this.add(new BooleanSetting("FrameTime", false));
      this.averageFrameTimeSetting = this.add(new BooleanSetting("AverageFrameTime", false));
      this.onePercentLowSetting = this.add(new BooleanSetting("OnePercentLow", false));
      this.oneTenthPercentLowSetting = this.add(new BooleanSetting("OneTenthPercentLow", false));
      this.frameVarianceSetting = this.add(new BooleanSetting("FrameVariance", false));
      this.hitchesSetting = this.add(new BooleanSetting("Hitches", false));
      this.serverBrandSetting = this.add(new BooleanSetting("ServerBrand", true));
      this.durabilitySetting = this.add(new BooleanSetting("Durability", false));
      this.shulkersSetting = this.add(new BooleanSetting("Shulkers", false));
      this.chestSetting = this.add(new BooleanSetting("Chest", false).markVisibilityDependency());
      this.doubleChestSetting = this.add(new BooleanSetting("Double", false).dependsOn(new Setting<?>[]{this.chestSetting}));
      this.metricRows = new ArrayList<>();
      this.setRenderer(
         new HudTextListRenderer(this, this.metricRows)
      );

      this.getRenderer()
         .setAnchor(HudAnchor.BOTTOM_RIGHT);
   }
}
