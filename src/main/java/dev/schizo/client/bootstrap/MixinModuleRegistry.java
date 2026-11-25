/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.bootstrap;

import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.combat.ArrowsModule;
import dev.schizo.client.modules.combat.AutoClickerModule;
import dev.schizo.client.modules.combat.AutoCrystalModule;
import dev.schizo.client.modules.combat.NoHitDelayModule;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.modules.exploit.PingSpoofModule;
import dev.schizo.client.modules.exploit.ReachModule;
import dev.schizo.client.modules.exploit.TridentModule;
import dev.schizo.client.modules.misc.AntiQuitModule;
import dev.schizo.client.modules.misc.NoPacketKickModule;
import dev.schizo.client.modules.misc.SwingModule;
import dev.schizo.client.modules.movement.ElytraFlyModule;
import dev.schizo.client.modules.movement.EntityControlModule;
import dev.schizo.client.modules.movement.FastClimbModule;
import dev.schizo.client.modules.movement.FastWebModule;
import dev.schizo.client.modules.movement.HighJumpModule;
import dev.schizo.client.modules.movement.JesusModule;
import dev.schizo.client.modules.movement.NoJumpDelayModule;
import dev.schizo.client.modules.movement.NoSlowModule;
import dev.schizo.client.modules.movement.SprintModule;
import dev.schizo.client.modules.movement.VelocityModule;
import dev.schizo.client.modules.player.AutoEatModule;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.player.ScaffoldModule;
import dev.schizo.client.modules.render.AmbienceModule;
import dev.schizo.client.modules.render.ChamsModule;
import dev.schizo.client.modules.render.FreeLookModule;
import dev.schizo.client.modules.render.GlintModule;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.TooltipsModule;
import java.util.Objects;

/** Cached module references consumed by mixin hooks without adding synthetic methods to Minecraft classes. */
public final class MixinModuleRegistry {
   private static volatile ModuleRegistry boundRegistry;

   private MixinModuleRegistry() {}

   public static <T extends Module> T require(Class<T> moduleType) {
      Objects.requireNonNull(moduleType, "Module type");
      ModuleRegistry registry = boundRegistry;
      if (registry == null) {
         throw new IllegalStateException("Mixin module registry is not bound");
      }
      T module = moduleType.cast(registry.getModule(moduleType));
      return Objects.requireNonNull(
         module,
         "Required module is not registered: " + moduleType.getSimpleName()
      );
   }

   public static volatile NoSlowModule blockNoslow;
   public static volatile PingSpoofModule connectionPingSpoofModule;
   public static volatile NoPacketKickModule connectionNoPacketKickModule;
   public static volatile ReachModule liquidBlockReach;
   public static volatile FastWebModule webBlockFastWebModule;
   public static volatile NoSlowModule slimeBlockNoslow;
   public static volatile NoSlowModule sweetBerryBushBlockNoSlowModule;
   public static volatile NoSlowModule honeyBlockNoSlowModule;
   public static volatile UIModule minecraftUiModule;
   public static volatile MultiTaskModule minecraftMultiTaskModule;
   public static volatile AutoEatModule minecraftAutoEatModule;
   public static volatile AntiQuitModule minecraftAntiQuitModule;
   public static volatile NoHitDelayModule minecraftNoHitDelayModule;
   public static volatile AutoClickerModule minecraftAutoClickerModule;
   public static volatile ArrowsModule minecraftArrowsModule;
   public static volatile AutoCrystalModule minecraftAutoCrystalModule;
   public static volatile NoRenderModule minecraftNoRenderModule;
   public static volatile DiscordModule minecraftDiscordModule;
   public static volatile TooltipsModule bundleContentsTooltipsModule;
   public static volatile AmbienceModule levelAmbience;
   public static volatile NoSlowModule entityNoSlowModule;
   public static volatile ElytraFlyModule entityElytraFlyModule;
   public static volatile FreecamModule entityFreecamModule;
   public static volatile VelocityModule entityVelocityModule;
   public static volatile FreeLookModule entityFreelook;
   public static volatile NoRenderModule entityNoRenderModule;
   public static volatile ElytraFlyModule livingEntityEfly;
   public static volatile NoJumpDelayModule livingEntityNoJumpDelayModule;
   public static volatile JesusModule livingEntityJesusModule;
   public static volatile SwingModule livingEntitySwing;
   public static volatile FastClimbModule livingEntityFastClimbModule;
   public static volatile HighJumpModule livingEntityHighJumpModule;
   public static volatile TridentModule tridentItemTridentModule;
   public static volatile ChamsModule textureTransformChamsModule;
   public static volatile GlintModule renderTypeColorGlintModule;
   public static volatile ChamsModule renderTypeColorChamsGlintModule;
   public static volatile NoRenderModule materialNoRenderModule;
   public static volatile EntityControlModule mobEntityControlModule;
   public static volatile ScaffoldModule playerScaffoldModule;
   public static volatile SprintModule playerSprintModule;
   public static volatile ReachModule playerReachModule;
   public static volatile NoSlowModule playerNoSlowModule;

   public static void bind(ModuleRegistry registry) {
      Objects.requireNonNull(registry, "Module registry is not initialized");
      boundRegistry = registry;
      blockNoslow = (NoSlowModule)Objects.requireNonNull(registry.getModule(NoSlowModule.class), "Required module is not registered: NoSlowModule");
      connectionPingSpoofModule = (PingSpoofModule)Objects.requireNonNull(registry.getModule(PingSpoofModule.class), "Required module is not registered: PingSpoofModule");
      connectionNoPacketKickModule = (NoPacketKickModule)Objects.requireNonNull(registry.getModule(NoPacketKickModule.class), "Required module is not registered: NoPacketKickModule");
      liquidBlockReach = (ReachModule)Objects.requireNonNull(registry.getModule(ReachModule.class), "Required module is not registered: ReachModule");
      webBlockFastWebModule = (FastWebModule)Objects.requireNonNull(registry.getModule(FastWebModule.class), "Required module is not registered: FastWebModule");
      slimeBlockNoslow = (NoSlowModule)Objects.requireNonNull(registry.getModule(NoSlowModule.class), "Required module is not registered: NoSlowModule");
      sweetBerryBushBlockNoSlowModule = (NoSlowModule)Objects.requireNonNull(registry.getModule(NoSlowModule.class), "Required module is not registered: NoSlowModule");
      honeyBlockNoSlowModule = (NoSlowModule)Objects.requireNonNull(registry.getModule(NoSlowModule.class), "Required module is not registered: NoSlowModule");
      minecraftUiModule = (UIModule)Objects.requireNonNull(registry.getModule(UIModule.class), "Required module is not registered: UIModule");
      minecraftMultiTaskModule = (MultiTaskModule)Objects.requireNonNull(registry.getModule(MultiTaskModule.class), "Required module is not registered: MultiTaskModule");
      minecraftAutoEatModule = (AutoEatModule)Objects.requireNonNull(registry.getModule(AutoEatModule.class), "Required module is not registered: AutoEatModule");
      minecraftAntiQuitModule = (AntiQuitModule)Objects.requireNonNull(registry.getModule(AntiQuitModule.class), "Required module is not registered: AntiQuitModule");
      minecraftNoHitDelayModule = (NoHitDelayModule)Objects.requireNonNull(registry.getModule(NoHitDelayModule.class), "Required module is not registered: NoHitDelayModule");
      minecraftAutoClickerModule = (AutoClickerModule)Objects.requireNonNull(registry.getModule(AutoClickerModule.class), "Required module is not registered: AutoClickerModule");
      minecraftArrowsModule = (ArrowsModule)Objects.requireNonNull(registry.getModule(ArrowsModule.class), "Required module is not registered: ArrowsModule");
      minecraftAutoCrystalModule = (AutoCrystalModule)Objects.requireNonNull(registry.getModule(AutoCrystalModule.class), "Required module is not registered: AutoCrystalModule");
      minecraftNoRenderModule = (NoRenderModule)Objects.requireNonNull(registry.getModule(NoRenderModule.class), "Required module is not registered: NoRenderModule");
      minecraftDiscordModule = (DiscordModule)Objects.requireNonNull(registry.getModule(DiscordModule.class), "Required module is not registered: DiscordModule");
      bundleContentsTooltipsModule = (TooltipsModule)Objects.requireNonNull(registry.getModule(TooltipsModule.class), "Required module is not registered: TooltipsModule");
      levelAmbience = (AmbienceModule)Objects.requireNonNull(registry.getModule(AmbienceModule.class), "Required module is not registered: AmbienceModule");
      entityNoSlowModule = (NoSlowModule)Objects.requireNonNull(registry.getModule(NoSlowModule.class), "Required module is not registered: NoSlowModule");
      entityElytraFlyModule = (ElytraFlyModule)Objects.requireNonNull(registry.getModule(ElytraFlyModule.class), "Required module is not registered: ElytraFlyModule");
      entityFreecamModule = (FreecamModule)Objects.requireNonNull(registry.getModule(FreecamModule.class), "Required module is not registered: FreecamModule");
      entityVelocityModule = (VelocityModule)Objects.requireNonNull(registry.getModule(VelocityModule.class), "Required module is not registered: VelocityModule");
      entityFreelook = (FreeLookModule)Objects.requireNonNull(registry.getModule(FreeLookModule.class), "Required module is not registered: FreeLookModule");
      entityNoRenderModule = (NoRenderModule)Objects.requireNonNull(registry.getModule(NoRenderModule.class), "Required module is not registered: NoRenderModule");
      livingEntityEfly = (ElytraFlyModule)Objects.requireNonNull(registry.getModule(ElytraFlyModule.class), "Required module is not registered: ElytraFlyModule");
      livingEntityNoJumpDelayModule = (NoJumpDelayModule)Objects.requireNonNull(registry.getModule(NoJumpDelayModule.class), "Required module is not registered: NoJumpDelayModule");
      livingEntityJesusModule = (JesusModule)Objects.requireNonNull(registry.getModule(JesusModule.class), "Required module is not registered: JesusModule");
      livingEntitySwing = (SwingModule)Objects.requireNonNull(registry.getModule(SwingModule.class), "Required module is not registered: SwingModule");
      livingEntityFastClimbModule = (FastClimbModule)Objects.requireNonNull(registry.getModule(FastClimbModule.class), "Required module is not registered: FastClimbModule");
      livingEntityHighJumpModule = (HighJumpModule)Objects.requireNonNull(registry.getModule(HighJumpModule.class), "Required module is not registered: HighJumpModule");
      tridentItemTridentModule = (TridentModule)Objects.requireNonNull(registry.getModule(TridentModule.class), "Required module is not registered: TridentModule");
      textureTransformChamsModule = (ChamsModule)Objects.requireNonNull(registry.getModule(ChamsModule.class), "Required module is not registered: ChamsModule");
      renderTypeColorGlintModule = (GlintModule)Objects.requireNonNull(registry.getModule(GlintModule.class), "Required module is not registered: GlintModule");
      renderTypeColorChamsGlintModule = (ChamsModule)Objects.requireNonNull(registry.getModule(ChamsModule.class), "Required module is not registered: ChamsModule");
      materialNoRenderModule = (NoRenderModule)Objects.requireNonNull(registry.getModule(NoRenderModule.class), "Required module is not registered: NoRenderModule");
      mobEntityControlModule = (EntityControlModule)Objects.requireNonNull(registry.getModule(EntityControlModule.class), "Required module is not registered: EntityControlModule");
      playerScaffoldModule = (ScaffoldModule)Objects.requireNonNull(registry.getModule(ScaffoldModule.class), "Required module is not registered: ScaffoldModule");
      playerSprintModule = (SprintModule)Objects.requireNonNull(registry.getModule(SprintModule.class), "Required module is not registered: SprintModule");
      playerReachModule = (ReachModule)Objects.requireNonNull(registry.getModule(ReachModule.class), "Required module is not registered: ReachModule");
      playerNoSlowModule = (NoSlowModule)Objects.requireNonNull(registry.getModule(NoSlowModule.class), "Required module is not registered: NoSlowModule");
   }
}
