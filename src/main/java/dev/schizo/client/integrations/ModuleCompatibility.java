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

   private ModuleCompatibility() {}

   public static Assessment assess(Class<?> moduleType) {
      if (moduleType == null) {
         return UNVERIFIED;
      }
      return REVIEWED.getOrDefault(moduleType, UNVERIFIED);
   }

   /**
    * No setting has been individually certified yet. The parent module classification is
    * included only as inherited context and is not a setting-level compatibility claim.
    */
   public static SettingAssessment assessSetting(Class<?> moduleType, String settingName) {
      Assessment module = assess(moduleType);
      String name = settingName == null || settingName.isBlank() ? "unnamed setting" : settingName;
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

   public record SettingAssessment(
      Classification classification,
      Classification inheritedModuleClassification,
      boolean individuallyReviewed,
      String reason
   ) {
      public boolean requiresWarning() {
         return this.inheritedModuleClassification().requiresMultiplayerWarning();
      }
   }
}
