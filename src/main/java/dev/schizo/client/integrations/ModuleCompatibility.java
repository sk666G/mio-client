/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo compatibility inventory.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

import dev.schizo.client.modules.client.SettingsModule;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.client.ColorsModule;
import dev.schizo.client.modules.client.FontsModule;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.modules.movement.NoFallModule;
import dev.schizo.client.modules.exploit.ReachModule;
import dev.schizo.client.modules.exploit.PhaseModule;
import dev.schizo.client.modules.combat.AntiPhaseModule;
import dev.schizo.client.modules.movement.ElytraFlyModule;
import dev.schizo.client.modules.combat.CriticalsModule;
import dev.schizo.client.modules.movement.NoSlowModule;
import dev.schizo.client.modules.movement.FastClimbModule;
import dev.schizo.client.modules.player.FastPlaceModule;
import dev.schizo.client.modules.player.SpeedMineModule;
import dev.schizo.client.modules.exploit.AntiHungerModule;
import dev.schizo.client.modules.exploit.PingSpoofModule;
import dev.schizo.client.modules.movement.FakeLagModule;
import dev.schizo.client.modules.exploit.PortalGodModeModule;
import dev.schizo.client.modules.movement.WarpModule;
import dev.schizo.client.modules.render.AmbienceModule;
import dev.schizo.client.modules.render.AnimationsModule;
import dev.schizo.client.modules.render.ChamsModule;
import dev.schizo.client.modules.render.ESPModule;
import dev.schizo.client.modules.render.HitmarkerModule;
import dev.schizo.client.modules.render.NameTagsModule;
import dev.schizo.client.modules.render.ParticlesModule;
import dev.schizo.client.modules.render.ShaderModule;
import dev.schizo.client.modules.render.TracersModule;
import dev.schizo.client.modules.render.FreeLookModule;
import dev.schizo.client.modules.render.NoBobModule;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.ViewModelModule;
import dev.schizo.client.modules.render.XrayModule;
import dev.schizo.client.modules.render.LogoutSpotsModule;
import dev.schizo.client.modules.render.SearchModule;
import dev.schizo.client.modules.render.WaypointsModule;
import dev.schizo.client.modules.render.ViewClipModule;
import dev.schizo.client.modules.render.BlurModule;
import dev.schizo.client.modules.render.BordersModule;
import dev.schizo.client.modules.render.BreakHighlightModule;
import dev.schizo.client.modules.render.CrosshairModule;
import dev.schizo.client.modules.render.GlintModule;
import dev.schizo.client.modules.render.HighlightModule;
import dev.schizo.client.modules.render.HoleESPModule;
import dev.schizo.client.modules.render.PhaseESPModule;
import dev.schizo.client.modules.render.SkeletonModule;
import dev.schizo.client.modules.render.SkyboxModule;
import dev.schizo.client.modules.render.TooltipsModule;
import dev.schizo.client.modules.render.TrailsModule;
import dev.schizo.client.modules.render.TrajectoriesModule;
import dev.schizo.client.modules.render.VoidESPModule;
import dev.schizo.client.modules.render.ZoomModule;
import dev.schizo.client.modules.misc.BetterChatModule;
import dev.schizo.client.modules.misc.AntiCyrillicModule;
import dev.schizo.client.modules.misc.AntiSpamModule;
import dev.schizo.client.modules.misc.NoNarratorModule;
import dev.schizo.client.modules.misc.SoundBlockerModule;
import dev.schizo.client.modules.misc.VisualRangeModule;
import dev.schizo.client.modules.player.NameProtectModule;
import dev.schizo.client.modules.client.NotificationsModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.modules.misc.WebhookNotifsModule;
import dev.schizo.client.modules.misc.ExtraScreenshotModule;
import dev.schizo.client.modules.misc.ExtraTabModule;
import dev.schizo.client.modules.misc.ChestSearchBarModule;
import dev.schizo.client.modules.misc.CustomDeathTextModule;
import dev.schizo.client.modules.render.TunnelsModule;
import dev.schizo.client.modules.client.CapesModule;
import dev.schizo.client.modules.misc.ChatFilterModule;
import dev.schizo.client.modules.player.NoEffectsModule;
import dev.schizo.client.modules.player.NoGlitchBlocksModule;
import dev.schizo.client.modules.combat.NoHitDelayModule;
import dev.schizo.client.modules.player.NoInteractModule;
import dev.schizo.client.modules.movement.NoJumpDelayModule;
import dev.schizo.client.modules.exploit.NoMineAnimationModule;
import dev.schizo.client.modules.misc.NoRotateModule;
import dev.schizo.client.modules.misc.StashFinderModule;
import dev.schizo.client.modules.exploit.NewChunksModule;
import dev.schizo.client.modules.misc.KillEffectsModule;
import dev.schizo.client.modules.misc.SkinFlickerModule;
import dev.schizo.client.modules.misc.CoordLoggerModule;
import dev.schizo.client.modules.exploit.FakeVanillaModule;
import dev.schizo.client.modules.misc.AnnouncerModule;
import dev.schizo.client.modules.misc.AntiAFKModule;
import dev.schizo.client.modules.misc.AutoReconnectModule;
import dev.schizo.client.modules.misc.AutoRespawnModule;
import dev.schizo.client.modules.misc.SpammerModule;
import dev.schizo.client.modules.player.AutoFishModule;
import dev.schizo.client.modules.player.AutoFarmModule;
import dev.schizo.client.modules.player.InventorySorterModule;
import dev.schizo.client.modules.player.ChestStealerModule;
import dev.schizo.client.modules.player.InventoryCleanerModule;
import dev.schizo.client.modules.movement.FireworksModule;
import dev.schizo.client.modules.misc.MiddleClickModule;
import dev.schizo.client.modules.player.AutoBreedModule;
import dev.schizo.client.modules.player.AutoTameModule;
import dev.schizo.client.modules.player.AutoTradeModule;
import dev.schizo.client.modules.player.AutoCraftModule;
import dev.schizo.client.modules.combat.AnchorAuraModule;
import dev.schizo.client.modules.misc.AntiAimModule;
import dev.schizo.client.modules.combat.AntiBotModule;
import dev.schizo.client.modules.combat.AntiFeetPlaceModule;
import dev.schizo.client.modules.combat.AntiPearlModule;
import dev.schizo.client.modules.combat.AuraModule;
import dev.schizo.client.modules.combat.AutoArmorModule;
import dev.schizo.client.modules.combat.AutoBowReleaseModule;
import dev.schizo.client.modules.combat.AutoClickerModule;
import dev.schizo.client.modules.combat.AutoCrystalModule;
import dev.schizo.client.modules.combat.AutoDropModule;
import dev.schizo.client.modules.combat.AutoExpModule;
import dev.schizo.client.modules.combat.AutoLogModule;
import dev.schizo.client.modules.combat.AutoMaceModule;
import dev.schizo.client.modules.combat.AutoMineModule;
import dev.schizo.client.modules.combat.AutoPotModule;
import dev.schizo.client.modules.combat.AutoTrapModule;
import dev.schizo.client.modules.combat.BedAuraModule;
import dev.schizo.client.modules.combat.BlockerModule;
import dev.schizo.client.modules.combat.BowAimModule;
import dev.schizo.client.modules.misc.AntiQuitModule;
import dev.schizo.client.modules.exploit.AntiVanishModule;
import dev.schizo.client.modules.movement.AvoidModule;
import dev.schizo.client.modules.combat.ArrowsModule;
import dev.schizo.client.modules.player.AutoEatModule;
import dev.schizo.client.modules.player.AutoMountModule;
import dev.schizo.client.modules.player.AutoNameTagModule;
import dev.schizo.client.modules.misc.AutoSignModule;
import dev.schizo.client.modules.player.AutoToolModule;
import dev.schizo.client.modules.player.AutoTemplateModule;
import dev.schizo.client.modules.movement.AutoWalkModule;
import dev.schizo.client.modules.exploit.ChorusControlModule;
import dev.schizo.client.modules.movement.EntityControlModule;
import dev.schizo.client.modules.movement.FastFallModule;
import dev.schizo.client.modules.exploit.FastLatencyModule;
import dev.schizo.client.modules.movement.FastStairsModule;
import dev.schizo.client.modules.movement.FastSwimModule;
import dev.schizo.client.modules.movement.FastWebModule;
import dev.schizo.client.modules.combat.FeetPlaceModule;
import dev.schizo.client.modules.combat.FishTrapModule;
import dev.schizo.client.modules.movement.FlightModule;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.misc.HeavenModule;
import dev.schizo.client.modules.movement.HighJumpModule;
import dev.schizo.client.modules.exploit.HitboxDesyncModule;
import dev.schizo.client.modules.combat.HoleFillModule;
import dev.schizo.client.modules.movement.HoleSnapModule;
import dev.schizo.client.modules.exploit.IllegalDisconnectModule;
import dev.schizo.client.modules.player.InventoryTweaksModule;
import dev.schizo.client.modules.player.ItemSaverModule;
import dev.schizo.client.modules.movement.JesusModule;
import dev.schizo.client.modules.combat.KeyPearlModule;
import dev.schizo.client.modules.movement.LongJumpModule;
import dev.schizo.client.modules.combat.MainhandModule;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.modules.misc.NoPacketKickModule;
import dev.schizo.client.modules.player.NukerModule;
import dev.schizo.client.modules.combat.OffhandModule;
import dev.schizo.client.modules.combat.PistonCrystalModule;
import dev.schizo.client.modules.combat.PusherModule;
import dev.schizo.client.modules.player.ReplenishModule;
import dev.schizo.client.modules.exploit.RocketExtenderModule;
import dev.schizo.client.modules.player.RotationLockModule;
import dev.schizo.client.modules.movement.SafeWalkModule;
import dev.schizo.client.modules.player.ScaffoldModule;
import dev.schizo.client.modules.combat.SelfFillModule;
import dev.schizo.client.modules.combat.SelfTrapModule;
import dev.schizo.client.modules.combat.SelfWebModule;
import dev.schizo.client.modules.player.SourceFillModule;
import dev.schizo.client.modules.movement.SpeedModule;
import dev.schizo.client.modules.movement.SprintModule;
import dev.schizo.client.modules.movement.StepModule;
import dev.schizo.client.modules.misc.SwingModule;
import dev.schizo.client.modules.exploit.TimerModule;
import dev.schizo.client.modules.exploit.TridentModule;
import dev.schizo.client.modules.movement.VelocityModule;
import dev.schizo.client.modules.combat.WebAuraModule;
import dev.schizo.client.modules.exploit.XCarryModule;
import java.util.Map;

/**
 * Conservative source-review labels for module behavior. Unknown modules and every setting
 * without a dedicated review stay UNVERIFIED by default.
 */
public final class ModuleCompatibility {
   private static final Assessment UNVERIFIED = new Assessment(
      Classification.UNVERIFIED,
      false,
      "This module has not completed a source-level multiplayer compatibility review."
   );

   private static final Map<Class<?>, Assessment> REVIEWED = Map.ofEntries(
      Map.entry(AntiCheatModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Permanent movement/mining coordination service that can alter packet timing and placement/mining behavior through individual settings; setting-level review remains incomplete."
      )),
      Map.entry(SettingsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local preferences and client settings only; no gameplay packet or server-state mutation was found."
      )),
      Map.entry(ColorsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local theme colors and render-dependent color settings only; no gameplay packet path was found."
      )),
      Map.entry(FontsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local font selection and text rendering only; no gameplay packet or server-state mutation was found."
      )),
      Map.entry(HUDModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Local HUD rendering and editor presentation only; no outgoing gameplay packet path was found."
      )),
      Map.entry(BetterChatModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Observes outgoing chat sends only to correlate local chat presentation; formatting, timestamps, sounds and message display changes remain client-side."
      )),
      Map.entry(AntiSpamModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Filters, stacks and rewrites only the local displayed chat event; no outbound packet mutation or world interaction was found."
      )),
      Map.entry(NoNarratorModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Disables the local narrator feature only."
      )),
      Map.entry(SoundBlockerModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Cancels selected local sound playback events without changing server state or gameplay packets."
      )),
      Map.entry(VisualRangeModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Observes clientbound entity-add information to produce local enter/leave notifications; it does not modify packets."
      )),
      Map.entry(NameProtectModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Changes locally rendered/displayed player-name text without modifying authentication, player identity or gameplay packets."
      )),
      Map.entry(UIModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Owns the local ClickGUI lifecycle and reacts to disconnect state only to close/reset UI."
      )),
      Map.entry(DiscordModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Publishes local activity to Discord IPC; no Minecraft gameplay packet or server-state mutation path was found."
      )),
      Map.entry(WebhookNotifsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Forwards locally observed notification/chat information to configured webhooks without modifying Minecraft gameplay packets."
      )),
      Map.entry(ExtraScreenshotModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Extends local screenshot capture/export/upload behavior; no Minecraft gameplay packet path was found."
      )),
      Map.entry(ExtraTabModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Customizes the local player-list/TAB presentation only."
      )),
      Map.entry(ChestSearchBarModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Adds local inventory-container search/highlighting UI without sending container interactions."
      )),
      Map.entry(CustomDeathTextModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes only the local death-screen text."
      )),
      Map.entry(TunnelsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Incrementally scans already-loaded client block state and renders tunnel overlays; no outgoing packet or world interaction path was found."
      )),
      Map.entry(AntiCyrillicModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Rewrites outbound chat-command packet command text by normalizing Cyrillic characters before the packet is sent."
      )),
      Map.entry(NotificationsModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Most notifications are local, but configured totem-pop behavior can send chat text or commands through the normal client chat/command path."
      )),
      Map.entry(CapesModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local cape rendering/presentation without modifying Minecraft gameplay packets."
      )),
      Map.entry(ChatFilterModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Filters and cancels only local chat-display events according to user rules."
      )),
      Map.entry(StashFinderModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Scans locally available world/container information and records or notifies possible stash locations without sending gameplay actions."
      )),
      Map.entry(CoordLoggerModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Observes local death/disconnect state and records coordinates; no outgoing gameplay action was found."
      )),
      Map.entry(NewChunksModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Classifies locally observed chunk-generation state for rendering/inspection without modifying server packets or world interaction."
      )),
      Map.entry(KillEffectsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Observes clientbound combat-kill state and creates local visual/audio kill effects only."
      )),
      Map.entry(NoEffectsModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses selected client effect queries, which can make local movement/physics interpretation diverge from server-applied effects."
      )),
      Map.entry(NoHitDelayModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Removes local attack miss delay and shortens use-key timing, changing interaction cadence from vanilla client behavior."
      )),
      Map.entry(NoJumpDelayModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Forces the local living-entity jump delay to zero for the player, changing movement timing from vanilla behavior."
      )),
      Map.entry(NoInteractModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Cancels selected block interactions or temporarily changes sneaking/use-item flow around those interactions."
      )),
      Map.entry(NoGlitchBlocksModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Cancels selected local interaction handling and can explicitly resend use-item-on interaction packets."
      )),
      Map.entry(NoMineAnimationModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Observes mining action packets and emits additional player-action packets while hiding mining progress."
      )),
      Map.entry(NoRotateModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes handling of server position/rotation corrections and outgoing movement rotation state."
      )),
      Map.entry(SkinFlickerModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Toggles player model-part options and broadcasts changed client options, so cosmetic state is transmitted rather than remaining purely local."
      )),
      Map.entry(FakeVanillaModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Rewrites the outbound client brand payload to report 'vanilla'; this changes information presented to the server and is not a compatibility certification."
      )),
      Map.entry(AnnouncerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Observes gameplay/player events and sends configured chat announcements through the normal server chat path."
      )),
      Map.entry(AntiAFKModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates ordinary player activity such as attacks/input actions to avoid inactivity."
      )),
      Map.entry(AutoReconnectModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Automates the client connection-screen lifecycle after disconnect; no in-session gameplay packet mutation was found."
      )),
      Map.entry(AutoRespawnModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically requests player respawn and can send configured post-death chat or commands."
      )),
      Map.entry(SpammerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically sends configured chat messages through the normal client chat/command path."
      )),
      Map.entry(AutoFishModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Observes fishing feedback and automatically performs ordinary use-item actions to reel/cast."
      )),
      Map.entry(AutoFarmModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically breaks crops and performs use-item-on interactions for farming."
      )),
      Map.entry(InventorySorterModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs inventory/menu slot swaps according to the configured sorting strategy."
      )),
      Map.entry(ChestStealerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs container quick-move/drop clicks and can close the opened container screen."
      )),
      Map.entry(InventoryCleanerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs inventory/container click actions to remove configured items."
      )),
      Map.entry(FireworksModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Automates rocket/item use and can emit start-fall-flying/swing actions, making movement and interaction timing server-sensitive."
      )),
      Map.entry(MiddleClickModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Configured middle-click actions can perform ordinary item use in addition to local social actions."
      )),
      Map.entry(AutoBreedModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically interacts with nearby breedable entities using ordinary entity interaction paths."
      )),
      Map.entry(AutoTameModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically interacts with tameable entities according to configured action/frequency."
      )),
      Map.entry(AutoTradeModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates villager interaction, trade selection and inventory/container clicks."
      )),
      Map.entry(AutoCraftModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates crafting/container actions and ordinary placement/use-item steps required by its crafting flow."
      )),
      Map.entry(AnchorAuraModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates respawn-anchor placement/charging/detonation interactions and associated inventory/rotation coordination."
      )),
      Map.entry(AntiAimModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Requests non-vanilla server-side rotations and coordinates those rotations around outgoing use-item actions."
      )),
      Map.entry(AntiBotModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Filters suspected bot players by removing matching entities from the local client world; no outgoing packet mutation was found."
      )),
      Map.entry(AntiFeetPlaceModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically replaces mined blocks near eligible targets through ordinary placement interaction helpers."
      )),
      Map.entry(AntiPearlModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Observes pearl trajectories and automatically places blocking/support blocks through the placement-module path."
      )),
      Map.entry(AuraModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically selects and attacks nearby entities while coordinating rotation/sprint state."
      )),
      Map.entry(AutoArmorModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Automates armor/inventory changes and can emit start-fall-flying state while coordinating elytra equipment."
      )),
      Map.entry(AutoBowReleaseModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically releases and reuses bow/crossbow actions through ordinary release/use-item paths."
      )),
      Map.entry(AutoClickerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically invokes the client's attack and interact actions at configured cadence."
      )),
      Map.entry(AutoCrystalModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Automates crystal placement/attack and can emit predicted crystal attack IDs plus explicit player-action packets outside ordinary game-mode sequencing."
      )),
      Map.entry(AutoDropModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Computes placement positions and automatically places gravity/support blocks around eligible targets."
      )),
      Map.entry(AutoExpModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically throws experience bottles using normal use-item/swing interaction paths."
      )),
      Map.entry(AutoLogModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Automatically disconnects under configured danger conditions and can delegate to the existing IllegalDisconnect mode when that module is enabled."
      )),
      Map.entry(AutoMaceModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Coordinates mace attacks with movement/elytra state, forced rotations and automated item use."
      )),
      Map.entry(AutoMineModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically selects blocks and starts ordinary block-destroy interactions with optional rotation."
      )),
      Map.entry(AutoPotModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically selects and throws potions using the normal use-item interaction semantics with coordinated rotation."
      )),
      Map.entry(AutoTrapModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Placement-module automation that surrounds selected targets with configured blocks."
      )),
      Map.entry(BedAuraModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates bed placement/use interactions and rotation around eligible targets."
      )),
      Map.entry(BlockerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Placement-oriented defensive automation that places configured blocking positions in response to observed combat state."
      )),
      Map.entry(BowAimModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Calculates target-leading ballistic rotations and injects those rotations into bow/crossbow use-item actions."
      )),
      Map.entry(AntiQuitModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Prevents accidental local quit/disconnect UI actions; no gameplay packet mutation or world interaction was found."
      )),
      Map.entry(AntiVanishModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Observes player-info add/remove state and locally reveals/notifies suspected vanished players without modifying outgoing packets."
      )),
      Map.entry(AvoidModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Directly changes local player velocity/position to escape configured hazards, which changes movement reported to the server."
      )),
      Map.entry(ArrowsModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Coordinates arrow selection/release and can cancel normal handling while emitting explicit player-action/use-item sequences."
      )),
      Map.entry(AutoEatModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically selects food and performs ordinary inventory/use-item actions."
      )),
      Map.entry(AutoMountModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically rotates toward and interacts with rideable entities."
      )),
      Map.entry(AutoNameTagModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs ordinary entity interactions to apply nearby name tags."
      )),
      Map.entry(AutoSignModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically submits configured sign text by replacing the local sign-edit flow with the normal sign-update action."
      )),
      Map.entry(AutoToolModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Can cancel normal mining handling and emit explicit start/stop/abort player-action packets while switching tools."
      )),
      Map.entry(AutoTemplateModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs smithing-menu quick-move and pickup slot actions."
      )),
      Map.entry(AutoWalkModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Automates forward movement input rather than directly rewriting movement packets."
      )),
      Map.entry(ChorusControlModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Queues/cancels movement and teleport-ack packets while controlling when a chorus teleport correction is accepted."
      )),
      Map.entry(EntityControlModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes ridden-entity control/rotation behavior and reacts to server position corrections."
      )),
      Map.entry(FastFallModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Directly accelerates downward velocity and can reposition the local player to descend faster."
      )),
      Map.entry(FastLatencyModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Sends latency probe packets and intercepts/cancels selected ping/keepalive-related responses to measure latency."
      )),
      Map.entry(FastStairsModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Requests jump input while traversing stairs based on local speed/terrain state."
      )),
      Map.entry(FastSwimModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Directly rewrites local player vertical and horizontal velocity while swimming."
      )),
      Map.entry(FastWebModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Combines direct movement changes with explicit block-destroy action packets while in cobwebs."
      )),
      Map.entry(FeetPlaceModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Automates surround placement and can center/reposition the local player while coordinating server corrections."
      )),
      Map.entry(FishTrapModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Inherits AutoTrap placement automation and targets crawling-player positions with predicted placement offsets."
      )),
      Map.entry(FlightModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Directly rewrites local player velocity while flying and reacts to server position corrections."
      )),
      Map.entry(FreecamModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Separates camera/player interaction state and can cancel/rewrite use-item position handling while emitting explicit position/rotation packets."
      )),
      Map.entry(HeavenModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Only moves the local camera upward during the death screen and cancels the default camera setup."
      )),
      Map.entry(HighJumpModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes jump power beyond vanilla values and can issue repeated shulker interactions for its shulker-based mode."
      )),
      Map.entry(HitboxDesyncModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Directly repositions the local player to desynchronize hitbox/position behavior."
      )),
      Map.entry(HoleFillModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Placement-module automation that fills nearby eligible holes with configured blocks."
      )),
      Map.entry(HoleSnapModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Requests forward/sneak input and high-priority rotation to steer the local player toward holes."
      )),
      Map.entry(IllegalDisconnectModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Intentionally sends an invalid signed-command packet to force a disconnect; classification documents existing behavior only."
      )),
      Map.entry(InventoryTweaksModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Changes inventory click handling and can cancel/reissue container click actions."
      )),
      Map.entry(ItemSaverModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Cancels local item-use/attack actions when configured durability thresholds are reached."
      )),
      Map.entry(JesusModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes water-walking movement behavior and mutates outgoing movement packet position state."
      )),
      Map.entry(KeyPearlModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically selects and uses an ender pearl through the normal use-item path."
      )),
      Map.entry(LongJumpModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes jump/movement behavior and can coordinate additional movement-sensitive actions."
      )),
      Map.entry(MainhandModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically manages main-hand/offhand inventory state and use-item interactions."
      )),
      Map.entry(MultiTaskModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Allows normally exclusive attack/use actions to be invoked together through client interaction hooks."
      )),
      Map.entry(NoPacketKickModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes the connection exception boundary so selected packet-processing failures are intercepted instead of disconnecting the client."
      )),
      Map.entry(NukerModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Automates block breaking and can cancel normal handling while emitting explicit start/stop destroy action packets."
      )),
      Map.entry(OffhandModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs inventory swaps, offhand use and discard actions based on combat state."
      )),
      Map.entry(PistonCrystalModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates piston/crystal placement and use-item interactions with coordinated rotation."
      )),
      Map.entry(PusherModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically places pistons and power sources around selected targets using ordinary placement/swing helpers."
      )),
      Map.entry(ReplenishModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically performs container quick-move clicks to replenish configured hotbar stacks."
      )),
      Map.entry(RocketExtenderModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Holds/cancels selected ping and interaction packets and releases responses later while extending rocket-related state."
      )),
      Map.entry(RotationLockModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Forces or requests locked yaw/pitch values that can become the player's reported server-side rotation."
      )),
      Map.entry(SafeWalkModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Requests sneak input or cancels local edge movement to prevent walking off blocks."
      )),
      Map.entry(ScaffoldModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Automates block placement and can alter jump strength, velocity, tick rate and rotation while bridging/towering."
      )),
      Map.entry(SelfFillModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Automates self-placement while emitting explicit position/rotation and destroy-action packets around the fill sequence."
      )),
      Map.entry(SelfTrapModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Placement automation that covers the local player with configured blocks."
      )),
      Map.entry(SelfWebModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically places a cobweb at the local player's position with optional rotation."
      )),
      Map.entry(SourceFillModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically replaces nearby liquid source blocks with solid-block placements."
      )),
      Map.entry(SpeedModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes movement physics, movement events and optional tick-rate behavior across multiple speed modes."
      )),
      Map.entry(SprintModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Automates sprint state and can emit explicit position/rotation plus start/stop sprint command packets."
      )),
      Map.entry(StepModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes step height and emits intermediate position packets for configured step behavior."
      )),
      Map.entry(SwingModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local swing arm, duration and legacy animation/shake presentation; its packet observer does not rewrite the swing packet."
      )),
      Map.entry(TimerModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes client/player tick-rate multipliers, altering the cadence of movement and gameplay simulation."
      )),
      Map.entry(TridentModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes riptide eligibility, minimum charge time and riptide push strength through TridentItem mixins."
      )),
      Map.entry(VelocityModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Intercepts/cancels incoming push/explosion/entity-motion handling and has packet-aware velocity modes."
      )),
      Map.entry(WebAuraModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Placement automation that traps selected nearby targets with cobwebs."
      )),
      Map.entry(XCarryModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Cancels selected container-close packets so crafting slots can remain usable as extra carried inventory."
      )),
      Map.entry(AmbienceModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local gamma, time, fog, sky and lighting presentation; its time adjustments are applied to the client world and no outgoing gameplay packet path was found."
      )),
      Map.entry(AnimationsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local camera interpolation and entity/crystal model presentation; no outgoing packet or server-state mutation was found."
      )),
      Map.entry(ChamsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders entity/crystal chams and local ghost/highlight models; the reviewed hooks only affect render state."
      )),
      Map.entry(ESPModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Builds local overlays from entity state, clientbound block/sound observations and local item-use events; no outgoing packet mutation was found."
      )),
      Map.entry(HitmarkerModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Observes outgoing attack packets to trigger local HUD and sound feedback; it does not cancel or rewrite those packets."
      )),
      Map.entry(NameTagsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local name-tag, equipment and player-information overlays; the reviewed connection access is read-only."
      )),
      Map.entry(ParticlesModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Adds or suppresses client-side particle effects only."
      )),
      Map.entry(ShaderModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Applies local post-processing shaders and render-target effects only."
      )),
      Map.entry(TracersModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Draws world and HUD tracer lines from locally observed entity positions; no outgoing packet path was found."
      )),
      Map.entry(NoBobModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Adjusts the local player avatar walk animation state for first-person bobbing presentation."
      )),
      Map.entry(NoRenderModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Cancels selected clientbound chunk-cache-radius and block-update packets, changing which server updates reach the client; other settings affect local rendering."
      )),

      Map.entry(ViewModelModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Transforms first-person item and arm model position, scale, rotation and animation only."
      )),
      Map.entry(XrayModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local block render filtering and reloads the client renderer; no block interaction or packet mutation path was found."
      )),
      Map.entry(LogoutSpotsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Observes clientbound player-info changes and renders locally stored logout markers; it does not modify outgoing packets."
      )),
      Map.entry(SearchModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Scans already-loaded client chunk/entity data and renders local search overlays/sounds; no world interaction or outgoing packet path was found."
      )),
      Map.entry(WaypointsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders user-saved local coordinate markers and tracer/beam overlays without modifying player position or packets."
      )),
      Map.entry(BlurModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Blurs local GUI backgrounds and adjusts only client render targets."
      )),
      Map.entry(BordersModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Draws local chunk/map border overlays from already-loaded client world coordinates."
      )),
      Map.entry(BreakHighlightModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Draws local boxes from the client block-break tracker; it does not issue or rewrite mining interactions."
      )),
      Map.entry(CrosshairModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Draws and animates a local HUD crosshair without changing player input or outgoing packets."
      )),
      Map.entry(GlintModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local item glint texture, scale and render color only."
      )),
      Map.entry(HighlightModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes the local block outline and renders selected block boxes; its event cancellation affects only outline rendering."
      )),
      Map.entry(HoleESPModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local overlays from the client hole cache and block states; no outgoing interaction path was found."
      )),
      Map.entry(PhaseESPModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local phase/position indicators from observed world state; no movement or packet mutation path was found."
      )),
      Map.entry(SkeletonModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Draws local player/entity skeleton overlays from interpolated render poses."
      )),
      Map.entry(SkyboxModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local sky rendering and only observes login/respawn packets to reset local rendering state."
      )),
      Map.entry(TooltipsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes client tooltip content, formatting and inventory presentation; no inventory interaction is sent."
      )),
      Map.entry(TrailsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Records locally observed entity/projectile positions and renders trails without changing movement or packets."
      )),
      Map.entry(TrajectoriesModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders predicted projectile/item paths from current client entity and player state; it does not launch or alter projectiles."
      )),
      Map.entry(VoidESPModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Builds and renders local void-column overlays; it only observes login/respawn packets to reset its cache."
      )),
      Map.entry(NoFallModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Its Packet mode modifies the on-ground flag in outgoing movement packets."
      )),
      Map.entry(FreeLookModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Separates camera rotation from player rotation and changes the view vector used for entity/block picking, which can alter interaction targeting."
      )),
      Map.entry(ReachModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes interaction reach distance and can produce interactions outside vanilla reach."
      )),
      Map.entry(PhaseModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes collision and player-position behavior beyond the vanilla movement model."
      )),
      Map.entry(AntiPhaseModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automates block placement in response to pearl-phasing behavior."
      )),
      Map.entry(ElytraFlyModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Implements non-vanilla flight modes and movement behavior."
      )),
      Map.entry(CriticalsModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Offers modes that alter attack timing/outcomes through packet or rotation behavior."
      )),
      Map.entry(NoSlowModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes movement slowdown behavior; mode-specific packet and input effects require review."
      )),
      Map.entry(FastClimbModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Changes climbing movement behavior beyond ordinary player input."
      )),
      Map.entry(ZoomModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Consumes mouse-wheel input to adjust local field of view and camera smoothing; it does not alter gameplay packets."
      )),
      Map.entry(ViewClipModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Changes third-person camera clipping and consumes configured mouse-wheel input; it does not change the local player position or movement packets."
      )),
      Map.entry(FastPlaceModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Changes the cadence of block/item placement interactions."
      )),
      Map.entry(SpeedMineModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Changes mining interaction cadence and progression."
      )),
      Map.entry(AntiHungerModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes movement/sprint packet behavior to influence hunger updates."
      )),
      Map.entry(PingSpoofModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Delays or alters keepalive timing."
      )),
      Map.entry(FakeLagModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Buffers or delays movement and other outbound packets."
      )),
      Map.entry(PortalGodModeModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Changes handling or timing of portal-related server packets."
      )),
      Map.entry(WarpModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Accumulates and replays movement packets, changing their timing/order."
      ))
   );

   private static final Map<SettingKey, SettingReview> REVIEWED_SETTINGS = Map.ofEntries(
      Map.entry(setting(TridentModule.class, "Always"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls whether the TridentItem mixin permits riptide use outside vanilla water/rain conditions."
      )),
      Map.entry(setting(TridentModule.class, "Boost"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Scales the riptide push vector applied by the TridentItem mixin."
      )),
      Map.entry(setting(TimerModule.class, "Mode"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Selects the tick-rate scheduling mode used by Timer."
      )),
      Map.entry(setting(TimerModule.class, "Physics"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Chooses whether Timer applies its multiplier to player-physics ticks instead of the broader client tick controller."
      )),
      Map.entry(setting(TimerModule.class, "Shift"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the requested tick-rate multiplier in non-pulse Timer modes."
      )),
      Map.entry(setting(TimerModule.class, "PulseMin"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls one endpoint of Timer's alternating tick-rate multiplier."
      )),
      Map.entry(setting(TimerModule.class, "PulseMax"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls one endpoint of Timer's alternating tick-rate multiplier."
      )),
      Map.entry(setting(TimerModule.class, "MinDelay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the duration of one Timer pulse phase."
      )),
      Map.entry(setting(TimerModule.class, "MaxDelay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the duration of the alternate Timer pulse phase."
      )),
      Map.entry(setting(TimerModule.class, "DisableDelay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls how long Timer keeps its configured multiplier before disabling in Disable mode."
      )),
      Map.entry(setting(SwingModule.class, "Hand"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Selects the locally rendered swinging arm; the reviewed SwingModule path does not rewrite the outgoing swing packet."
      )),
      Map.entry(setting(SwingModule.class, "Type"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Selects the local legacy swing-animation style."
      )),
      Map.entry(setting(SwingModule.class, "Shake"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Enables local legacy swing shake animation state."
      )),
      Map.entry(setting(SwingModule.class, "Speed"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes the local rendered swing duration."
      )),
      Map.entry(setting(NoPacketKickModule.class, "Message"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Only controls whether intercepted connection exceptions are reported locally; it does not control interception itself."
      )),
      Map.entry(setting(dev.schizo.client.modules.movement.NoFallModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects NoFall's behavior mode; reviewed modes can alter outgoing movement state or movement/action sequencing."
      )),
      Map.entry(setting(dev.schizo.client.modules.movement.NoFallModule.class, "GroundDistance"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the local ground-distance threshold used by NoFall's MLG-style action logic."
      )),
      Map.entry(setting(PingSpoofModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects which latency-related packet path PingSpoof delays or mutates."
      )),
      Map.entry(setting(PingSpoofModule.class, "Delay"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls how long selected latency-related packets are held before release."
      )),
      Map.entry(setting(PingSpoofModule.class, "Jitter"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Adds variation to the configured packet-delay interval."
      )),
      Map.entry(setting(FakeLagModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects whether buffered outbound packets are held until disable or released on a timed pulse."
      )),
      Map.entry(setting(FakeLagModule.class, "Cancel"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Discards the current buffered packet set and rolls the local position back to the buffered position."
      )),
      Map.entry(setting(FakeLagModule.class, "Timeout"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the timed release interval for buffered packets in pulse mode."
      )),
      Map.entry(setting(FakeLagModule.class, "AutoDisable"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls automatic termination of packet buffering in response to configured conditions."
      )),
      Map.entry(setting(FakeLagModule.class, "OnDamage"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls whether observed damage terminates the active packet-buffering session."
      )),
      Map.entry(setting(FakeLagModule.class, "OnRubberBand"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls whether a received server position correction terminates the active packet-buffering session."
      )),
      Map.entry(setting(FakeLagModule.class, "OnTime"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the duration before an auto-disable condition ends packet buffering."
      )),
      Map.entry(setting(FakeLagModule.class, "Render"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only the local buffered-position rendering feature."
      )),
      Map.entry(setting(FakeLagModule.class, "Chams"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only local rendering of the buffered player representation."
      )),
      Map.entry(setting(FakeLagModule.class, "Fill"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local buffered-position fill color."
      )),
      Map.entry(setting(FakeLagModule.class, "Outline"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local buffered-position outline color."
      )),
      Map.entry(setting(FakeLagModule.class, "LineWidth"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local buffered-position outline width."
      )),
      Map.entry(setting(FakeLagModule.class, "Packets"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts as the local settings group for packet-selection controls and is not itself read by the buffering logic."
      )),
      Map.entry(setting(FakeLagModule.class, "Movement"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of outgoing movement packets."
      )),
      Map.entry(setting(FakeLagModule.class, "Interact"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of outgoing block-use interaction packets."
      )),
      Map.entry(setting(FakeLagModule.class, "Swing"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of outgoing swing packets."
      )),
      Map.entry(setting(FakeLagModule.class, "Swap"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of outgoing carried-slot change packets."
      )),
      Map.entry(setting(FakeLagModule.class, "InteractEntity"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of outgoing entity-interaction packets."
      )),
      Map.entry(setting(FakeLagModule.class, "Digging"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of outgoing player-action packets."
      )),
      Map.entry(setting(FakeLagModule.class, "Ping"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls buffering of selected latency, keepalive and resource-pack response packets."
      )),
      Map.entry(setting(FakeLagModule.class, "All"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Causes every outgoing packet seen by FakeLag's send hook to be eligible for buffering."
      )),
      Map.entry(setting(ReachModule.class, "Modifier"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the interaction/reach distance used by the Reach module."
      )),
      Map.entry(setting(ReachModule.class, "GhostHand"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes block-use targeting and can replace the normal interaction with an explicit use-item-on packet."
      )),
      Map.entry(setting(ReachModule.class, "Boats"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes handling of outgoing boat placement interactions."
      )),
      Map.entry(setting(ReachModule.class, "LiquidPlace"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes liquid-target placement handling and can emit replacement use-item-on interactions."
      )),
      Map.entry(setting(ReachModule.class, "AirPlace"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables placement handling without the normal block-target requirement."
      )),
      Map.entry(setting(ReachModule.class, "Grim"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects an alternate packet/hand sequencing path for reviewed Reach placement interactions."
      )),
      Map.entry(setting(ReachModule.class, "NoEntityTrace"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes whether entity hits are ignored when selecting an interaction target."
      )),
      Map.entry(setting(ReachModule.class, "Pickaxe"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls when NoEntityTrace applies while holding a pickaxe."
      )),
      Map.entry(setting(ReachModule.class, "Food"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls when NoEntityTrace applies while holding food."
      )),
      Map.entry(setting(ReachModule.class, "Crystals"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls when NoEntityTrace applies while holding end crystals."
      )),
      Map.entry(setting(ReachModule.class, "Totems"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls when NoEntityTrace applies while holding a totem."
      )),
      Map.entry(setting(ReachModule.class, "All"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Applies NoEntityTrace targeting behavior regardless of the held-item filter."
      )),
      Map.entry(setting(FreeLookModule.class, "Scroll"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Consumes mouse-wheel input to change local third-person camera distance while FreeLook is active."
      )),
      Map.entry(setting(NoRenderModule.class, "Chunks"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Cancels selected clientbound chunk-cache-radius packet handling."
      )),
      Map.entry(setting(NoRenderModule.class, "Fire"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Can cancel a matching clientbound fire block-update packet in the reviewed End-crystal fire path."
      )),
      Map.entry(setting(NoRenderModule.class, "FireBlock"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Suppresses local fire/smoke particle presentation and is distinct from the packet-facing entity Fire setting."
      )),
      Map.entry(setting(AntiCheatModule.class, "2b2t"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables the module's server-profile-specific packet, inventory and movement coordination paths."
      )),
      Map.entry(setting(AntiCheatModule.class, "Limiter"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables the module's outbound packet-rate limiting path when the associated server profile and movement sync are active."
      )),
      Map.entry(setting(AntiCheatModule.class, "NoServerSlot"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes handling of server-selected hotbar slot updates and can resend the local held-slot state."
      )),
      Map.entry(setting(AntiCheatModule.class, "StrictDirection"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes which support directions are accepted by shared block-placement helpers."
      )),
      Map.entry(setting(AntiCheatModule.class, "Mining"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the mining action sequencing policy used for outgoing destroy-block actions."
      )),
      Map.entry(setting(AntiCheatModule.class, "Placements"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects the shared placement-policy variant used by block interaction helpers."
      )),
      Map.entry(setting(AntiCheatModule.class, "MovementSync"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables shared movement/rotation synchronization used by multiple gameplay actions."
      )),
      Map.entry(setting(AntiCheatModule.class, "Rotations"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Selects how synchronized rotations are represented during movement and interaction actions."
      )),
      Map.entry(setting(AntiCheatModule.class, "Swing"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls whether shared interaction actions use transmitted swing behavior rather than client-only swing presentation."
      )),
      Map.entry(setting(AntiCheatModule.class, "Place"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls transmitted swing behavior for placement/interact actions."
      )),
      Map.entry(setting(AntiCheatModule.class, "Hit"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls transmitted swing behavior for hit actions."
      )),
      Map.entry(setting(AntiCheatModule.class, "MiningSwing"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls transmitted swing behavior for mining actions."
      )),
      Map.entry(setting(AntiCheatModule.class, "AssumeBlockBreaking"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes shared placement occupancy decisions for blocks currently tracked as being broken."
      )),
      Map.entry(setting(SprintModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the sprint behavior path, including variants that explicitly coordinate sprint command and position/rotation traffic."
      )),
      Map.entry(setting(SprintModule.class, "StopInLiquids"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes whether automated sprinting is suppressed while the player is in liquid."
      )),
      Map.entry(setting(SprintModule.class, "InBlocks"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes whether automated sprinting is permitted while collision state indicates the player is inside blocks."
      )),
      Map.entry(setting(SprintModule.class, "DirectionSpoof"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes the rotation/position state reported during reviewed sprint movement coordination."
      )),
      Map.entry(setting(SprintModule.class, "Keep"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls explicit stop/start sprint command handling around attacks."
      )),
      Map.entry(setting(SprintModule.class, "VanillaSlowdown"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes sprint speed calculation to include vanilla block slowdown factors."
      )),
      Map.entry(setting(SprintModule.class, "Adjust"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables a configurable multiplier in sprint movement-speed calculation."
      )),
      Map.entry(setting(SprintModule.class, "Factor"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the sprint movement-speed multiplier when Adjust is enabled."
      )),
      Map.entry(setting(SprintModule.class, "Boost"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed explosion-impulse-based sprint movement boost handling."
      )),
      Map.entry(setting(SprintModule.class, "BoostSpeed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the multiplier applied by the reviewed sprint boost path."
      )),
      Map.entry(setting(StepModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the step implementation, including the reviewed path that emits intermediate position packets."
      )),
      Map.entry(setting(StepModule.class, "Height"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the requested non-vanilla step height."
      )),
      Map.entry(setting(StepModule.class, "SafeDisable"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes when Step automatically disables based on local collision/ground safety state."
      )),
      Map.entry(setting(StepModule.class, "UseTimer"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables temporary client tick-rate adjustment during the reviewed step path."
      )),
      Map.entry(setting(StepModule.class, "TimerSpeed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the temporary tick-rate multiplier used by Step."
      )),
      Map.entry(setting(VelocityModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the velocity handler used for incoming motion/push packet and event processing."
      )),
      Map.entry(setting(VelocityModule.class, "Horizontal"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the horizontal component retained from reviewed incoming velocity handling."
      )),
      Map.entry(setting(VelocityModule.class, "Vertical"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the vertical component retained from reviewed incoming velocity handling."
      )),
      Map.entry(setting(VelocityModule.class, "Inverse"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes the direction applied to retained velocity in the plain velocity mode."
      )),
      Map.entry(setting(VelocityModule.class, "Reduce"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables additional reduction behavior in reviewed velocity handling."
      )),
      Map.entry(setting(VelocityModule.class, "Static"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes how the reviewed velocity reduction path derives its resulting motion."
      )),
      Map.entry(setting(VelocityModule.class, "Sources"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts as the local settings group for velocity-source selectors and is not itself a packet/movement control."
      )),
      Map.entry(setting(VelocityModule.class, "Explosions"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls interception of explosion-derived velocity handling."
      )),
      Map.entry(setting(VelocityModule.class, "Push"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls interception of entity/player push handling."
      )),
      Map.entry(setting(VelocityModule.class, "Liquids"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed liquid-current motion suppression."
      )),
      Map.entry(setting(VelocityModule.class, "FishingBobber"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls interception of fishing-bobber pull motion handling."
      )),
      Map.entry(setting(NoGlitchBlocksModule.class, "Place"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Can replace normal placement handling with an explicit use-item-on interaction."
      )),
      Map.entry(setting(NoGlitchBlocksModule.class, "Break"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls cancellation of selected local block-break interaction handling."
      )),
      Map.entry(setting(SkinFlickerModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the sequence of player model-part option changes that are broadcast through client options."
      )),
      Map.entry(setting(SkinFlickerModule.class, "Cape"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls whether cape visibility participates in the transmitted model-part option changes."
      )),
      Map.entry(setting(SkinFlickerModule.class, "Delay"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the cadence of transmitted model-part option changes."
      )),
      Map.entry(setting(IllegalDisconnectModule.class, "IgnoreProxy"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes whether the module permits its existing invalid-packet disconnect action for recognized proxy brands."
      )),
      Map.entry(setting(XCarryModule.class, "AutoPickup"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables automatic inventory slot pickup/movement into crafting slots; the module's close-packet interception is independent of this setting."
      )),
      Map.entry(setting(XCarryModule.class, "OnlyNearItems"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Restricts XCarry auto-pickup inventory actions to times when nearby pickup items are detected."
      )),
      Map.entry(setting(FastLatencyModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the latency probe and response-handling packet path."
      )),
      Map.entry(setting(FastLatencyModule.class, "Timeout"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls when an outstanding latency probe is considered expired."
      )),
      Map.entry(setting(FastLatencyModule.class, "Response"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls how selected server ping/keepalive/resource-pack response packets are answered by the reviewed fast-response path."
      )),
      Map.entry(setting(AntiCyrillicModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects whether Cyrillic normalization applies to outgoing chat, commands or both."
      )),
      Map.entry(setting(RocketExtenderModule.class, "Timeout"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls how long the reviewed rocket-extension state remains active before it is released/reset."
      )),
      Map.entry(setting(RocketExtenderModule.class, "Hold"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls whether selected incoming ping packets are held while the reviewed rocket-extension state is active."
      )),
      Map.entry(setting(NoRotateModule.class, "InBlocks"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects how reviewed rotation-correction handling behaves while the player is inside blocks, including cancellation or reported-rotation handling."
      )),
      Map.entry(setting(WarpModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed Warp movement/packet timing implementation."
      )),
      Map.entry(setting(WarpModule.class, "Boost"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the movement/displacement multiplier used by Warp."
      )),
      Map.entry(setting(WarpModule.class, "Charge"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the amount of packet-timing charge accumulated or consumed by Warp."
      )),
      Map.entry(setting(WarpModule.class, "ChargeSpeed"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the rate at which the reviewed Warp charge state changes."
      )),
      Map.entry(setting(WarpModule.class, "Recharge"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects how reviewed queued timing-response packets are released to replenish Warp charge."
      )),
      Map.entry(setting(WarpModule.class, "FakeLag"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables Warp's reviewed packet-holding behavior while displacement is active."
      )),
      Map.entry(setting(WarpModule.class, "DisableGround"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls whether the movement effect is disabled when grounded."
      )),
      Map.entry(setting(WarpModule.class, "AutoDisable"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls automatic termination of Warp's reviewed movement/packet state."
      )),
      Map.entry(setting(FastClimbModule.class, "Speed"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Controls the locally requested climb-speed adjustment."
      )),
      Map.entry(setting(FastClimbModule.class, "Airborne"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Controls whether fast-climb input/movement handling remains active while not grounded."
      )),
      Map.entry(setting(ViewClipModule.class, "Skip"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Selects which side of the local third-person camera clip animation is skipped."
      )),
      Map.entry(setting(ViewClipModule.class, "Range"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls local third-person camera clip distance."
      )),
      Map.entry(setting(ViewClipModule.class, "Smooth"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only local camera interpolation."
      )),
      Map.entry(setting(ViewClipModule.class, "Scroll"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Consumes mouse-wheel input to adjust local third-person camera distance."
      )),
      Map.entry(setting(ZoomModule.class, "Smooth"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only the local zoom transition animation."
      )),
      Map.entry(setting(ZoomModule.class, "SmoothCamera"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls the local Minecraft smooth-camera option while Zoom is active."
      )),
      Map.entry(setting(ZoomModule.class, "Scroll"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Consumes mouse-wheel input to change local zoom distance."
      )),
      Map.entry(setting(ZoomModule.class, "Amount"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls the local field-of-view zoom divisor."
      )),
      Map.entry(setting(PhaseModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects Phase's reviewed movement/interaction path, including paths that emit explicit position or use-item traffic."
      )),
      Map.entry(setting(PhaseModule.class, "Auto"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls automatic repetition of the reviewed movement action."
      )),
      Map.entry(setting(PhaseModule.class, "Delay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the cadence of the reviewed automatic movement action."
      )),
      Map.entry(setting(PhaseModule.class, "Attack"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether nearby blocking entities or blocks are cleared through ordinary combat/mining interaction helpers."
      )),
      Map.entry(setting(PhaseModule.class, "Dynamic"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes target/rotation selection for the reviewed movement interaction."
      )),
      Map.entry(setting(PhaseModule.class, "NCP"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects an alternate reviewed interaction/slot sequencing path."
      )),
      Map.entry(setting(PhaseModule.class, "Always"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls whether the reviewed movement interaction remains active for repeated attempts instead of disabling after one result."
      )),
      Map.entry(setting(PhaseModule.class, "InBlocks"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes whether the reviewed movement interaction may proceed while collision state indicates the player is inside blocks."
      )),
      Map.entry(setting(PhaseModule.class, "InCrawl"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes whether the reviewed movement interaction may proceed while the player is in crawl-sized collision state."
      )),
      Map.entry(setting(PhaseModule.class, "Avoid"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes the reviewed movement target/angle selection near unsafe vertical boundaries."
      )),
      Map.entry(setting(PhaseModule.class, "1.21"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed version-specific rotation/interaction sequencing path."
      )),
      Map.entry(setting(PhaseModule.class, "Place"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables support-block placement as part of the reviewed interaction."
      )),
      Map.entry(setting(PhaseModule.class, "Await"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether the reviewed interaction waits for a valid support-placement state before continuing."
      )),
      Map.entry(setting(PhaseModule.class, "Flatten"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes the support-block positions considered by the reviewed placement path."
      )),
      Map.entry(setting(SpeedMineModule.class, "Blocks"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects which block types are eligible for automatic mining behavior."
      )),
      Map.entry(setting(SpeedMineModule.class, "Mode"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects how the configured block list is applied when choosing mining targets."
      )),
      Map.entry(setting(SpeedMineModule.class, "Damage"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls the mining-progress threshold used before the reviewed mining action advances."
      )),
      Map.entry(setting(SpeedMineModule.class, "Range"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls the maximum distance at which reviewed mining targets remain eligible."
      )),
      Map.entry(setting(SpeedMineModule.class, "Grim"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects an alternate reviewed mining action-sequencing path."
      )),
      Map.entry(setting(SpeedMineModule.class, "TPSSync"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes reviewed mining timing using observed server tick-rate information."
      )),
      Map.entry(setting(SpeedMineModule.class, "NoBreakDelay"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether the module suppresses the local delay before continuing mining actions."
      )),
      Map.entry(setting(SpeedMineModule.class, "ExtraBreak"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables additional reviewed mining-action sequencing for a secondary break target."
      )),
      Map.entry(setting(SpeedMineModule.class, "SneakExtend"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes how secondary mining targets are extended while the player is sneaking."
      )),
      Map.entry(setting(SpeedMineModule.class, "RebreakPage"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts only as the local settings-group state for rebreak controls."
      )),
      Map.entry(setting(SpeedMineModule.class, "Rebreak"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed repeated-mining action strategy."
      )),
      Map.entry(setting(SpeedMineModule.class, "Async"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes scheduling of the reviewed repeated-mining action sequence."
      )),
      Map.entry(setting(SpeedMineModule.class, "InstantDelay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the delay between reviewed repeated-mining attempts."
      )),
      Map.entry(setting(SpeedMineModule.class, "HoldingBest"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Requires the locally selected best mining tool before a reviewed repeated-mining action proceeds."
      )),
      Map.entry(setting(SpeedMineModule.class, "SwapPage"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts only as the local settings-group state for mining tool-swap controls."
      )),
      Map.entry(setting(SpeedMineModule.class, "AutoSwap"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects how the module automatically changes inventory/hotbar tool state for mining."
      )),
      Map.entry(setting(SpeedMineModule.class, "SwapBack"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether the previous inventory/hotbar selection is restored after reviewed mining actions."
      )),
      Map.entry(setting(SpeedMineModule.class, "1.21"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed version-specific mining/tool-swap sequencing path."
      )),
      Map.entry(setting(SpeedMineModule.class, "Alternative"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects an alternate reviewed silent tool-swap/mining sequence."
      )),
      Map.entry(setting(SpeedMineModule.class, "SwapReset"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls reset behavior around the reviewed silent tool-swap sequence."
      )),
      Map.entry(setting(SpeedMineModule.class, "Rotate"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables rotation requests toward the reviewed mining target."
      )),
      Map.entry(setting(SpeedMineModule.class, "Limit"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the rotation limit applied by the reviewed mining rotation path."
      )),
      Map.entry(setting(SpeedMineModule.class, "Render"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only local mining-progress rendering."
      )),
      Map.entry(setting(SpeedMineModule.class, "LineWidth"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only the local mining overlay line width."
      )),
      Map.entry(setting(SpeedMineModule.class, "RenderMode"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Selects only the local mining overlay presentation mode."
      )),
      Map.entry(setting(SpeedMineModule.class, "Air"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only whether reviewed mining overlay state is shown for air/finished targets."
      )),
      Map.entry(setting(SpeedMineModule.class, "Fade"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only local mining overlay fade animation."
      )),
      Map.entry(setting(SpeedMineModule.class, "ColorMode"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Selects only the local mining overlay color strategy."
      )),
      Map.entry(setting(SpeedMineModule.class, "Fill"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local mining overlay fill color."
      )),
      Map.entry(setting(SpeedMineModule.class, "Outline"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local mining overlay outline color."
      )),
      Map.entry(setting(SpeedMineModule.class, "Abort"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts as a hidden local reset action that clears the module's current mining session state."
      )),
      Map.entry(setting(CriticalsModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed critical-hit packet/movement sequencing path."
      )),
      Map.entry(setting(CriticalsModule.class, "PauseOnMove"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes when the reviewed critical-hit packet sequence is eligible to run while the player is moving."
      )),
      Map.entry(setting(CriticalsModule.class, "OnlyFull"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Restricts when the reviewed critical-hit packet sequence is eligible based on local collision state."
      )),
      Map.entry(setting(CriticalsModule.class, "OnlyWeapon"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Restricts the reviewed critical-hit packet sequence to selected weapon/tool categories."
      )),
      Map.entry(setting(FastPlaceModule.class, "Delay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the local right-click/use-item cadence used by FastPlace."
      )),
      Map.entry(setting(FastPlaceModule.class, "AutoPlace"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables automatic placement/use-item interaction while an eligible target is present."
      )),
      Map.entry(setting(FastPlaceModule.class, "RandomBlock"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes which eligible block is selected for automatic placement."
      )),
      Map.entry(setting(FastPlaceModule.class, "Items"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts only as the local settings group for FastPlace item filters."
      )),
      Map.entry(setting(FastPlaceModule.class, "All"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Applies the reviewed fast-use interaction behavior to all supported item categories."
      )),
      Map.entry(setting(FastPlaceModule.class, "Exp"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls fast-use interaction behavior for experience bottles."
      )),
      Map.entry(setting(FastPlaceModule.class, "Rockets"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls fast-use interaction behavior for firework rockets."
      )),
      Map.entry(setting(FastPlaceModule.class, "Blocks"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls fast placement behavior for ordinary block items."
      )),
      Map.entry(setting(FastPlaceModule.class, "Food"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls fast-use interaction behavior for food items."
      )),
      Map.entry(setting(FastPlaceModule.class, "Tools"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls fast-use interaction behavior for supported right-click tools."
      )),
      Map.entry(setting(FastPlaceModule.class, "EnderChests"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls reviewed ender-chest placement/use behavior."
      )),
      Map.entry(setting(FastPlaceModule.class, "AutoReplace"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables automatic replacement placement around the reviewed mined-block state."
      )),
      Map.entry(setting(AutoToolModule.class, "BlockDamage"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the block-progress threshold at which AutoTool's reviewed explicit mining-action sequence can run."
      )),
      Map.entry(setting(AutoToolModule.class, "Inventory"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Allows automatic tool selection to use inventory slots outside the hotbar."
      )),
      Map.entry(setting(AutoToolModule.class, "SwapBack"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether the previous tool/inventory selection is restored after automatic tool use."
      )),
      Map.entry(setting(AutoToolModule.class, "NoBreakDelay"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes local mining delay/cadence while AutoTool is active."
      )),
      Map.entry(setting(ArrowsModule.class, "AutoShoot"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables automatic target selection, rotation and bow/crossbow release actions."
      )),
      Map.entry(setting(ArrowsModule.class, "Speed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the scheduled cadence of reviewed automatic release actions."
      )),
      Map.entry(setting(ArrowsModule.class, "BeforePull"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes when automatic inventory arrow preparation occurs relative to the release action."
      )),
      Map.entry(setting(ArrowsModule.class, "Fov"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls the target-selection angle used by reviewed automatic arrow actions."
      )),
      Map.entry(setting(ArrowsModule.class, "AssumeDuration"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls local effect-duration assumptions used when choosing which effect arrow to prepare."
      )),
      Map.entry(setting(NoSlowModule.class, "GuiMove"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Enables movement-key handling while supported GUI screens are open."
      )),
      Map.entry(setting(NoSlowModule.class, "Containers"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Controls whether GUI movement input remains active while container screens are open."
      )),
      Map.entry(setting(NoSlowModule.class, "Rotate"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Enables keyboard-driven local player rotation while GUI movement is active."
      )),
      Map.entry(setting(NoSlowModule.class, "Strict"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts only as the local settings group for strict NoSlow controls."
      )),
      Map.entry(setting(NoSlowModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed item-use slowdown handling path, including paths that alter interaction or carried-slot packet sequencing."
      )),
      Map.entry(setting(NoSlowModule.class, "StrictInventory"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed inventory interaction mode used while strict NoSlow handling is active."
      )),
      Map.entry(setting(NoSlowModule.class, "Player"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts only as the local settings group for player slowdown controls."
      )),
      Map.entry(setting(NoSlowModule.class, "Items"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables reviewed item-use slowdown cancellation and mode-specific interaction handling."
      )),
      Map.entry(setting(NoSlowModule.class, "Sneak"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses reviewed local sneaking slowdown behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "Crawl"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses reviewed local crawling slowdown behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "Slowness"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses selected local Slowness effect queries that influence movement behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "Blocks"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Acts only as the local settings group for block-slowdown controls."
      )),
      Map.entry(setting(NoSlowModule.class, "Ice"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes reviewed local movement behavior on ice blocks."
      )),
      Map.entry(setting(NoSlowModule.class, "SoulSand"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses reviewed local soul-sand slowdown behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "Honey"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses reviewed local honey-block slowdown behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "BerryBush"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Suppresses reviewed local sweet-berry-bush slowdown behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "Slime"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes reviewed local slime-block movement behavior."
      )),
      Map.entry(setting(NoSlowModule.class, "NoSlimeBounce"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls suppression of local slime-block bounce behavior."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed elytra movement mode, including modes with explicit flight-state or movement packet handling."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Takeoff"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed takeoff path, which can transmit start-fall-flying state."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Responsive"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes how quickly reviewed elytra movement responds to player input."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Factorize"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes reviewed elytra movement scaling behavior."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Always"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes when reviewed boost/movement handling remains active."
      )),
      Map.entry(setting(ElytraFlyModule.class, "NCPBoost"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Selects an alternate reviewed elytra boost calculation."
      )),
      Map.entry(setting(ElytraFlyModule.class, "MinBoost"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the minimum reviewed elytra movement boost."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Limit"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Caps reviewed elytra movement speed."
      )),
      Map.entry(setting(ElytraFlyModule.class, "AutoJump"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Automates jump/takeoff input used by reviewed elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "InLava"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls whether reviewed elytra movement handling remains active in lava."
      )),
      Map.entry(setting(ElytraFlyModule.class, "LavaSpeed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed movement speed while the lava-specific path is active."
      )),
      Map.entry(setting(ElytraFlyModule.class, "MinY"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the minimum altitude used by reviewed vanilla-style elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Height"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the target altitude window used by reviewed vanilla-style elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "UseFirework"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables ordinary firework-rocket use as part of reviewed elytra flight automation."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Speed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed horizontal elytra flight speed."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Vertical"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Selects reviewed vertical-movement handling in control mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "VSpeed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed vertical elytra movement speed."
      )),
      Map.entry(setting(ElytraFlyModule.class, "VPitch"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the pitch threshold used by reviewed vertical elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Glide"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed passive vertical glide adjustment."
      )),
      Map.entry(setting(ElytraFlyModule.class, "SpoofPitch"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables reviewed reported-pitch substitution while elytra movement is active."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Pitch"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the reported pitch used by the reviewed pitch-substitution path."
      )),
      Map.entry(setting(ElytraFlyModule.class, "AntiAfk"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables reviewed periodic movement/flight-state activity in packet-oriented flight mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "VerticalPacket"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables reviewed vertical movement handling in the packet-oriented elytra mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Accelerate"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables gradual reviewed elytra movement acceleration."
      )),
      Map.entry(setting(ElytraFlyModule.class, "VerAccelMin"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the starting vertical acceleration value for reviewed elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "AccelMin"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the starting horizontal acceleration value for reviewed elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "AccelTime"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the time window over which reviewed elytra acceleration changes."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Start"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the initial movement value used by the reviewed strict elytra mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "StrictSpeed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls movement speed in the reviewed strict elytra mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "DeployTime"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the timing of reviewed fall-flying state deployment."
      )),
      Map.entry(setting(ElytraFlyModule.class, "PitchLock"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed pitch locking in bounce-style elytra movement."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Silent"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls whether reviewed pitch/rotation handling is represented without matching local camera presentation."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Auto"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables automatic reviewed pitch selection for bounce-style flight."
      )),
      Map.entry(setting(ElytraFlyModule.class, "BouncePitch"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls the reviewed bounce-flight pitch when automatic pitch selection is disabled."
      )),
      Map.entry(setting(ElytraFlyModule.class, "BounceBoost"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables the reviewed packet-oriented boost path for bounce flight."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Diagonal"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes reviewed bounce-flight movement for diagonal input."
      )),
      Map.entry(setting(ElytraFlyModule.class, "BounceMaxBoost"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Caps reviewed bounce-flight boost speed."
      )),
      Map.entry(setting(ElytraFlyModule.class, "Compensate"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed movement compensation in bounce flight."
      )),
      Map.entry(setting(ElytraFlyModule.class, "StopMotion"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Selects how reviewed elytra movement is reduced or stopped in the associated mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "FallChestplate"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Enables automatic chest-slot inventory management during reviewed elytra state changes."
      )),
      Map.entry(setting(ElytraFlyModule.class, "ResponsiveDescend"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Changes reviewed descend responsiveness in the associated elytra mode."
      )),
      Map.entry(setting(ElytraFlyModule.class, "MuteElytra"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Suppresses only local elytra sound presentation."
      )),
      Map.entry(setting(ElytraFlyModule.class, "InfDurability"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed durability-management path, which coordinates inventory and fall-flying state actions."
      )),
      Map.entry(setting(ChorusControlModule.class, "Yaw"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the reported yaw used when the reviewed delayed teleport state is released."
      )),
      Map.entry(setting(ChorusControlModule.class, "Smart"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables reviewed distance-aware selection/release of the held teleport/acknowledgement state."
      )),
      Map.entry(setting(ChorusControlModule.class, "Range"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the distance threshold used by the reviewed smart teleport-state selection."
      )),
      Map.entry(setting(ChorusControlModule.class, "Render"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only local rendering of the selected teleport position."
      )),
      Map.entry(setting(ChorusControlModule.class, "Fill"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local target-position fill color."
      )),
      Map.entry(setting(ChorusControlModule.class, "Outline"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local target-position outline color."
      )),
      Map.entry(setting(ChorusControlModule.class, "LineWidth"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local target-position outline width."
      )),
      Map.entry(setting(FastWebModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed cobweb escape path, including movement and explicit block-action behavior."
      )),
      Map.entry(setting(FastWebModule.class, "Horizontal"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed horizontal movement scaling while inside cobwebs."
      )),
      Map.entry(setting(FastWebModule.class, "Speed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed horizontal cobweb movement speed."
      )),
      Map.entry(setting(FastWebModule.class, "Vertical"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed vertical movement scaling while inside cobwebs."
      )),
      Map.entry(setting(FastWebModule.class, "VSpeed"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed vertical cobweb movement speed."
      )),
      Map.entry(setting(FastWebModule.class, "Upwards"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls whether reviewed vertical cobweb movement may accelerate upward."
      )),
      Map.entry(setting(FreecamModule.class, "Speed"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls only local free-camera horizontal movement speed."
      )),
      Map.entry(setting(FreecamModule.class, "VerticalSpeed"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls only local free-camera vertical movement speed."
      )),
      Map.entry(setting(FreecamModule.class, "Scroll"), settingAssessed(
         Classification.INPUT_MODIFYING,
         "Consumes local scroll input to adjust free-camera movement speed."
      )),
      Map.entry(setting(FreecamModule.class, "NoHands"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Controls only local first-person hand rendering while Freecam is active."
      )),
      Map.entry(setting(FreecamModule.class, "Rotate"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed rotation of the real player toward the free-camera interaction target."
      )),
      Map.entry(setting(FreecamModule.class, "Always"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls whether reviewed real-player rotation follows the free-camera target continuously."
      )),
      Map.entry(setting(FreecamModule.class, "ClampY"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Enables local free-camera vertical clamping."
      )),
      Map.entry(setting(FreecamModule.class, "Min"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls only the local free-camera minimum vertical bound."
      )),
      Map.entry(setting(FreecamModule.class, "Value"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls the local replacement height used by free-camera vertical clamping."
      )),
      Map.entry(setting(FreecamModule.class, "Max"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls only the local free-camera maximum vertical bound."
      )),
      Map.entry(setting(JesusModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed water-walking movement/packet handling mode."
      )),
      Map.entry(setting(JesusModule.class, "Strict"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes reviewed outgoing movement-position handling while water-walking behavior is active."
      )),
      Map.entry(setting(JesusModule.class, "Ascending"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Controls reviewed upward movement strength while leaving liquid."
      )),
      Map.entry(setting(NukerModule.class, "Blocks"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects which block types are eligible for automated breaking."
      )),
      Map.entry(setting(NukerModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed automated mining/action sequencing mode."
      )),
      Map.entry(setting(NukerModule.class, "Shape"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects the spatial region used to choose automated mining targets."
      )),
      Map.entry(setting(NukerModule.class, "Sort"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls local ordering of eligible automated mining targets."
      )),
      Map.entry(setting(NukerModule.class, "BPT"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls how many reviewed block-breaking actions may be advanced per tick."
      )),
      Map.entry(setting(NukerModule.class, "Delay"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the cadence of reviewed automated block-breaking actions."
      )),
      Map.entry(setting(NukerModule.class, "Range"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls the maximum range used when selecting automated mining targets."
      )),
      Map.entry(setting(NukerModule.class, "WallRange"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls the maximum range used for targets without direct line-of-sight."
      )),
      Map.entry(setting(NukerModule.class, "Height"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls the vertical extent of the reviewed tunnel-shaped mining selection."
      )),
      Map.entry(setting(NukerModule.class, "StrictDirection"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Restricts reviewed block-breaking target faces/directions."
      )),
      Map.entry(setting(NukerModule.class, "Rotate"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables rotation requests toward automated mining targets."
      )),
      Map.entry(setting(NukerModule.class, "Creative"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects reviewed creative-style block-breaking action handling."
      )),
      Map.entry(setting(NukerModule.class, "Flatten"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes the spatial target-selection filter for automated mining."
      )),
      Map.entry(setting(NukerModule.class, "BaritoneArea"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Restricts automated mining targets to the locally supplied Baritone area when available."
      )),
      Map.entry(setting(NukerModule.class, "PlayerProximity"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Changes reviewed target eligibility based on nearby player positions."
      )),
      Map.entry(setting(NukerModule.class, "Color"), settingAssessed(
         Classification.VISUAL_ONLY,
         "Changes only the local queued-target render color."
      )),
      Map.entry(setting(SelfFillModule.class, "Blocks"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects which block types are eligible for reviewed self-placement."
      )),
      Map.entry(setting(SelfFillModule.class, "Selection"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Selects how the configured block list is applied during reviewed self-placement."
      )),
      Map.entry(setting(SelfFillModule.class, "Mode"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects the reviewed self-placement movement/packet sequencing path."
      )),
      Map.entry(setting(SelfFillModule.class, "Rotate"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed rotation toward the self-placement interaction."
      )),
      Map.entry(setting(SelfFillModule.class, "Attack"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether blocking entities are attacked before reviewed self-placement."
      )),
      Map.entry(setting(SelfFillModule.class, "Center"), settingAssessed(
         Classification.SERVER_SENSITIVE,
         "Enables reviewed local-player centering/repositioning before self-placement."
      )),
      Map.entry(setting(SelfFillModule.class, "Alternative"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Selects an alternate reviewed self-placement packet/movement sequence."
      )),
      Map.entry(setting(SelfFillModule.class, "Head"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether reviewed self-placement also considers the head-level position."
      )),
      Map.entry(setting(SelfFillModule.class, "MultiPlace"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Controls whether multiple reviewed self-placement positions may be acted on in one placement cycle."
      )),
      Map.entry(setting(SelfFillModule.class, "Persistent"), settingAssessed(
         Classification.WORLD_INTERACTION,
         "Keeps reviewed self-placement behavior active across repeated placement opportunities."
      )),
      Map.entry(setting(SelfFillModule.class, "JumpDisable"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Only controls whether the module disables itself when jump input is detected."
      )),
      Map.entry(setting(AutoLogModule.class, "Health"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the health threshold that can trigger the reviewed automatic disconnect path."
      )),
      Map.entry(setting(AutoLogModule.class, "InvincTimeout"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the local invincibility-time assumption used by reviewed automatic disconnect decisions."
      )),
      Map.entry(setting(AutoLogModule.class, "Reconnect"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls only the local post-disconnect automatic reconnect flow."
      )),
      Map.entry(setting(AutoLogModule.class, "AutoDisable"), settingAssessed(
         Classification.CLIENT_ONLY,
         "Controls only whether AutoLog disables its own module state after triggering."
      )),
      Map.entry(setting(AutoLogModule.class, "GearedOnly"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Restricts when reviewed automatic disconnect behavior is eligible based on local equipment state."
      )),
      Map.entry(setting(AutoLogModule.class, "UnfocusedOnly"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Restricts when reviewed automatic disconnect behavior is eligible based on local window focus state."
      )),
      Map.entry(setting(AutoLogModule.class, "OnRender"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes which local health snapshot is used when deciding whether to trigger reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Crystal"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables crystal-danger detection as a trigger for reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "TNTMinecart"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables TNT-minecart danger detection as a trigger for reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Totems"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables totem-count state as a trigger for reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "TotemCount"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the totem-count threshold used by reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Elytra"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables elytra durability/equipment state as a trigger for reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Durability"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the elytra durability threshold used by reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "AssumeInventory"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Changes whether inventory-held equipment is included in reviewed disconnect-risk evaluation."
      )),
      Map.entry(setting(AutoLogModule.class, "YLevel"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables vertical-position state as a trigger for reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Y"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the vertical threshold used by reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Waypoints"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Enables local waypoint proximity as a condition in reviewed automatic disconnect behavior."
      )),
      Map.entry(setting(AutoLogModule.class, "Distance"), settingAssessed(
         Classification.PACKET_AFFECTING,
         "Controls the waypoint-distance threshold used by reviewed automatic disconnect behavior."
      ))
   );

   private ModuleCompatibility() {}

   public static Assessment assess(Class<?> moduleType) {
      if (moduleType == null) {
         return UNVERIFIED;
      }
      return REVIEWED.getOrDefault(moduleType, UNVERIFIED);
   }

   public static int reviewedSettingCount() {
      return REVIEWED_SETTINGS.size();
   }

   public static SettingAssessment assessSetting(Class<?> moduleType, String settingName) {
      Assessment module = assess(moduleType);
      String name = settingName == null || settingName.isBlank() ? "unnamed setting" : settingName;
      SettingReview review = moduleType == null ? null : REVIEWED_SETTINGS.get(setting(moduleType, name));
      if (review != null) {
         return new SettingAssessment(
            review.classification(),
            module.classification(),
            true,
            review.reason()
         );
      }
      return new SettingAssessment(
         Classification.UNVERIFIED,
         module.classification(),
         false,
         "Setting '" + name + "' has not been individually reviewed; parent module classification is "
            + module.classification() + "."
      );
   }

   private static Assessment assessed(Classification classification, String reason) {
      return new Assessment(classification, true, reason);
   }

   private static SettingKey setting(Class<?> moduleType, String settingName) {
      return new SettingKey(moduleType, settingName);
   }

   private static SettingReview settingAssessed(Classification classification, String reason) {
      return new SettingReview(classification, reason);
   }

   public enum Classification {
      CLIENT_ONLY,
      VISUAL_ONLY,
      INPUT_MODIFYING,
      WORLD_INTERACTION,
      PACKET_AFFECTING,
      SERVER_SENSITIVE,
      UNVERIFIED;

      public boolean requiresMultiplayerWarning() {
         return this != CLIENT_ONLY && this != VISUAL_ONLY;
      }
   }

   public record Assessment(Classification classification, boolean sourceReviewed, String reason) {
      public boolean requiresMultiplayerWarning() {
         return this.classification().requiresMultiplayerWarning();
      }
   }

   private record SettingKey(Class<?> moduleType, String settingName) {
   }

   private record SettingReview(Classification classification, String reason) {
   }

   public record SettingAssessment(
      Classification classification,
      Classification inheritedModuleClassification,
      boolean individuallyReviewed,
      String reason
   ) {
      public boolean requiresWarning() {
         return this.individuallyReviewed()
            ? this.classification().requiresMultiplayerWarning()
            : this.inheritedModuleClassification().requiresMultiplayerWarning();
      }
   }
}
