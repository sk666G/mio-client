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
import dev.schizo.client.modules.client.CapesModule;
import dev.schizo.client.modules.client.DiscordModule;
import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.BaritoneModule;
import dev.schizo.client.modules.client.NotificationsModule;
import dev.schizo.client.modules.hud.ArmorModule;
import dev.schizo.client.modules.hud.ChatModule;
import dev.schizo.client.modules.hud.CryptoModule;
import dev.schizo.client.modules.hud.DirectionModule;
import dev.schizo.client.modules.hud.EffectsModule;
import dev.schizo.client.modules.hud.EntityListModule;
import dev.schizo.client.modules.hud.FrameTimeGraphModule;
import dev.schizo.client.modules.hud.GraphModule;
import dev.schizo.client.modules.hud.IndicatorModule;
import dev.schizo.client.modules.hud.InventoryModule;
import dev.schizo.client.modules.hud.LagOMeterModule;
import dev.schizo.client.modules.hud.MapModule;
import dev.schizo.client.modules.hud.MetricsModule;
import dev.schizo.client.modules.hud.ModuleListModule;
import dev.schizo.client.modules.hud.MusicModule;
import dev.schizo.client.modules.hud.PlayerModelModule;
import dev.schizo.client.modules.hud.PositionModule;
import dev.schizo.client.modules.hud.TextRadarModule;
import dev.schizo.client.modules.hud.TotemsModule;
import dev.schizo.client.modules.hud.WatermarkModule;
import dev.schizo.client.modules.hud.WelcomerModule;
import dev.schizo.client.modules.misc.AnnouncerModule;
import dev.schizo.client.modules.misc.AntiAFKModule;
import dev.schizo.client.modules.misc.AntiAimModule;
import dev.schizo.client.modules.misc.AntiCyrillicModule;
import dev.schizo.client.modules.misc.AntiQuitModule;
import dev.schizo.client.modules.misc.AntiSpamModule;
import dev.schizo.client.modules.misc.AutoReconnectModule;
import dev.schizo.client.modules.misc.AutoRespawnModule;
import dev.schizo.client.modules.misc.AutoSignModule;
import dev.schizo.client.modules.misc.BetterChatModule;
import dev.schizo.client.modules.misc.ChatFilterModule;
import dev.schizo.client.modules.misc.ChestSearchBarModule;
import dev.schizo.client.modules.misc.CoordLoggerModule;
import dev.schizo.client.modules.misc.CustomDeathTextModule;
import dev.schizo.client.modules.misc.ExtraScreenshotModule;
import dev.schizo.client.modules.misc.ExtraTabModule;
import dev.schizo.client.modules.misc.HeavenModule;
import dev.schizo.client.modules.misc.KillEffectsModule;
import dev.schizo.client.modules.misc.MiddleClickModule;
import dev.schizo.client.modules.misc.NoNarratorModule;
import dev.schizo.client.modules.misc.NoPacketKickModule;
import dev.schizo.client.modules.misc.NoRotateModule;
import dev.schizo.client.modules.misc.SkinFlickerModule;
import dev.schizo.client.modules.misc.SoundBlockerModule;
import dev.schizo.client.modules.misc.SpammerModule;
import dev.schizo.client.modules.misc.StashFinderModule;
import dev.schizo.client.modules.misc.VisualRangeModule;
import dev.schizo.client.modules.misc.WebhookNotifsModule;
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
import dev.schizo.client.modules.render.TunnelsModule;
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
      Map.entry(ArmorModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local armor/equipment HUD state only; no outgoing gameplay packet path was found."
      )),
      Map.entry(ChatModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local chat HUD information only; no outgoing chat or gameplay packet path was found."
      )),
      Map.entry(CryptoModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders market-price HUD data fetched from an external public HTTP API; it does not modify Minecraft gameplay traffic or server state."
      )),
      Map.entry(DirectionModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local facing-direction text only."
      )),
      Map.entry(EffectsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders locally observed status-effect information only."
      )),
      Map.entry(EntityListModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders locally observed entity counts/list information only."
      )),
      Map.entry(FrameTimeGraphModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders bounded local frame-time telemetry only."
      )),
      Map.entry(GraphModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local graph telemetry only."
      )),
      Map.entry(IndicatorModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local indicator state derived from client/module data only."
      )),
      Map.entry(InventoryModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders the locally observed inventory as a HUD widget without issuing inventory actions."
      )),
      Map.entry(LagOMeterModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local latency/tick timing observations only."
      )),
      Map.entry(MapModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders locally available map data in the HUD without issuing world interactions."
      )),
      Map.entry(MetricsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local performance metrics only."
      )),
      Map.entry(ModuleListModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local module status text only."
      )),
      Map.entry(MusicModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local media-session display text only."
      )),
      Map.entry(PlayerModelModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders a local player-model HUD preview only."
      )),
      Map.entry(PositionModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local/fake coordinate text and only observes clientbound position corrections; it does not rewrite or send movement packets."
      )),
      Map.entry(TextRadarModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders a local player radar from observed client entity state only."
      )),
      Map.entry(TotemsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local totem count/state only."
      )),
      Map.entry(WatermarkModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local Schizo watermark/version text only."
      )),
      Map.entry(WelcomerModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Renders local welcome/status text only."
      )),
      Map.entry(UIModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Opens and configures Schizo's local ClickGUI, cursor, language, sound and palette presentation; the reviewed packet hook only observes disconnect to close/disable the UI."
      )),
      Map.entry(CapesModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Selects a local cape texture resource for rendering; no gameplay packet, interaction or server-state mutation path was found."
      )),
      Map.entry(DiscordModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Publishes local Discord IPC rich-presence state. The optional Server setting can disclose the current server address to the user's local Discord client, but no Minecraft gameplay packet is modified."
      )),
      Map.entry(NotificationsModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Most notification settings are local chat presentation, but SayChat can automatically send configured chat/command text after observed totem events."
      )),
      Map.entry(BaritoneModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Permanent bridge that changes Baritone pathing/automation assumptions, optional block backfilling and disconnect-on-arrival behavior; these settings can change automated multiplayer actions."
      )),
      Map.entry(AnnouncerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Observes local/world/player activity and can automatically send configured chat announcements to the connected server."
      )),
      Map.entry(AntiAFKModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Automates rotation, jumping/swinging and optional whisper replies to prevent inactivity; behavior can affect server-observed player state."
      )),
      Map.entry(AntiAimModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Changes player rotation behavior while observing item-use packets; the resulting rotations are server-visible."
      )),
      Map.entry(AntiCyrillicModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Normalizes outgoing chat/command text, including mutation of the serverbound command payload."
      )),
      Map.entry(AntiQuitModule.class, assessed(
         Classification.INPUT_MODIFYING,
         "Adds local confirmation flow before server disconnect or game close; it does not synthesize gameplay packets."
      )),
      Map.entry(AntiSpamModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Deduplicates and formats local incoming chat presentation and resets local history on disconnect."
      )),
      Map.entry(AutoReconnectModule.class, assessed(
         Classification.SERVER_SENSITIVE,
         "Automatically initiates a new connection attempt after a disconnect when configured."
      )),
      Map.entry(AutoRespawnModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically requests respawn and can send configured chat/command text after respawn."
      )),
      Map.entry(AutoSignModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically submits sign text with a ServerboundSignUpdatePacket."
      )),
      Map.entry(BetterChatModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Mostly changes local chat presentation, but its message prefix/suffix feature modifies outgoing chat text before transmission."
      )),
      Map.entry(ChatFilterModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Filters local incoming chat; outgoing chat is observed only to avoid filtering the client's echoed message."
      )),
      Map.entry(ChestSearchBarModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Adds local inventory/container search UI only."
      )),
      Map.entry(CoordLoggerModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Records local coordinates on selected client events and clientbound death information without modifying gameplay traffic."
      )),
      Map.entry(CustomDeathTextModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Replaces only the local death-screen text."
      )),
      Map.entry(ExtraScreenshotModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Adds local screenshot/clipboard tooling and an optional explicit Imgur upload; it does not modify Minecraft gameplay traffic."
      )),
      Map.entry(ExtraTabModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Changes local player-list/tab presentation and input display only."
      )),
      Map.entry(HeavenModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Moves only the local death-screen camera after death."
      )),
      Map.entry(KillEffectsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Observes clientbound kill state and creates local visual/audio kill effects without sending gameplay actions."
      )),
      Map.entry(MiddleClickModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Middle-click can toggle local friend state or use firework rockets through normal client interaction paths."
      )),
      Map.entry(NoNarratorModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Suppresses local narrator behavior only."
      )),
      Map.entry(NoPacketKickModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Cancels the client's connection exception handler while enabled, changing how malformed/exceptional packet handling affects the session."
      )),
      Map.entry(NoRotateModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Mutates clientbound position-correction rotation and selected outgoing movement yaw/pitch fields."
      )),
      Map.entry(SkinFlickerModule.class, assessed(
         Classification.PACKET_AFFECTING,
         "Toggles player model-part options and broadcasts the resulting client-information state to the server."
      )),
      Map.entry(SoundBlockerModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Cancels selected sounds in the local sound engine only."
      )),
      Map.entry(SpammerModule.class, assessed(
         Classification.WORLD_INTERACTION,
         "Automatically sends configured chat/command messages to the connected server."
      )),
      Map.entry(StashFinderModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Observes loaded chunk/container state and records/notifies local stash information without issuing world interactions."
      )),
      Map.entry(VisualRangeModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Observes client entity/chunk packets and prints local range notifications without modifying outgoing gameplay traffic."
      )),
      Map.entry(WebhookNotifsModule.class, assessed(
         Classification.CLIENT_ONLY,
         "Forwards selected local chat/session notifications to an explicitly configured external webhook or Telegram destination without modifying Minecraft gameplay traffic."
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
      Map.entry(TunnelsModule.class, assessed(
         Classification.VISUAL_ONLY,
         "Scans already-loaded client chunk block states on bounded client-thread slices and renders local tunnel overlays; no outgoing gameplay packet or world interaction path was found."
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

   private static final Map<SettingKey, SettingAssessment> REVIEWED_SETTINGS = Map.ofEntries(
      setting(CapesModule.class, "Cape", Classification.VISUAL_ONLY,
         "Selects only the local cape texture resource."),
      setting(DiscordModule.class, "Server", Classification.CLIENT_ONLY,
         "Controls whether the local Discord rich presence includes the current server address; it does not alter Minecraft gameplay traffic."),
      setting(TunnelsModule.class, "Verticals", Classification.VISUAL_ONLY,
         "Controls which locally detected tunnel shapes are included in the overlay."),
      setting(TunnelsModule.class, "LineWidth", Classification.VISUAL_ONLY,
         "Controls only local tunnel-outline rendering width."),
      setting(TunnelsModule.class, "Height", Classification.VISUAL_ONLY,
         "Controls only local tunnel-overlay render height."),
      setting(TunnelsModule.class, "MinLength", Classification.VISUAL_ONLY,
         "Filters locally rendered tunnel segments by detected length."),
      setting(TunnelsModule.class, "Fill", Classification.VISUAL_ONLY,
         "Controls only local tunnel fill color."),
      setting(TunnelsModule.class, "Outline", Classification.VISUAL_ONLY,
         "Controls only local tunnel outline color."),

      setting(NotificationsModule.class, "Watermark", Classification.CLIENT_ONLY,
         "Controls only the local notification watermark color."),
      setting(NotificationsModule.class, "Brackets", Classification.CLIENT_ONLY,
         "Controls only local notification bracket color."),
      setting(NotificationsModule.class, "Commands", Classification.CLIENT_ONLY,
         "Controls local display of Schizo command feedback."),
      setting(NotificationsModule.class, "Modules", Classification.CLIENT_ONLY,
         "Controls local module-toggle notifications."),
      setting(NotificationsModule.class, "OnlyDrawn", Classification.CLIENT_ONLY,
         "Filters local module-toggle notifications by drawn state."),
      setting(NotificationsModule.class, "TotemPops", Classification.CLIENT_ONLY,
         "Enables local totem-pop notifications; outgoing chat remains separately controlled by SayChat."),
      setting(NotificationsModule.class, "Self", Classification.CLIENT_ONLY,
         "Controls whether local self totem events are displayed."),
      setting(NotificationsModule.class, "Primary", Classification.CLIENT_ONLY,
         "Controls local notification text color."),
      setting(NotificationsModule.class, "Secondary", Classification.CLIENT_ONLY,
         "Controls local notification text color."),
      setting(NotificationsModule.class, "SayChat", Classification.WORLD_INTERACTION,
         "Enables automatic outgoing chat/command messages after qualifying totem events."),
      setting(NotificationsModule.class, "Path", Classification.WORLD_INTERACTION,
         "Selects the local message-template file used by the automatic outgoing chat feature."),
      setting(NotificationsModule.class, "Delay", Classification.WORLD_INTERACTION,
         "Controls timing of automatic outgoing totem chat messages."),
      setting(NotificationsModule.class, "OnlyEnemies", Classification.WORLD_INTERACTION,
         "Filters which observed players can trigger automatic outgoing totem chat."),
      setting(NotificationsModule.class, "Random", Classification.WORLD_INTERACTION,
         "Controls template selection for automatic outgoing totem chat."),
      setting(NotificationsModule.class, "Refresh", Classification.WORLD_INTERACTION,
         "Reloads templates used by the automatic outgoing totem chat feature."),

      setting(BaritoneModule.class, "FreeLook", Classification.INPUT_MODIFYING,
         "Changes Baritone free-look behavior while its automation is active."),
      setting(BaritoneModule.class, "BackFill", Classification.WORLD_INTERACTION,
         "Allows Baritone to place blocks behind its path while automating movement."),
      setting(BaritoneModule.class, "LogOnArrival", Classification.SERVER_SENSITIVE,
         "Configures Baritone to disconnect the multiplayer session when a path goal is reached."),
      setting(BaritoneModule.class, "AssumeEnabled", Classification.SERVER_SENSITIVE,
         "Synchronizes Baritone path assumptions with Step, AutoTool and SafeWalk module state."),
      setting(BaritoneModule.class, "CensorCoords", Classification.CLIENT_ONLY,
         "Controls Baritone coordinate/command redaction in local output."),
      setting(BaritoneModule.class, "ReplaceColors", Classification.VISUAL_ONLY,
         "Controls local Baritone path/goal render color replacement."),
      setting(BaritoneModule.class, "Color", Classification.VISUAL_ONLY,
         "Controls local Baritone path rendering color."),
      setting(BaritoneModule.class, "Goal", Classification.VISUAL_ONLY,
         "Controls local Baritone goal rendering color.")
   );

   private ModuleCompatibility() {}

   public static Assessment assess(Class<?> moduleType) {
      if (moduleType == null) {
         return UNVERIFIED;
      }
      return REVIEWED.getOrDefault(moduleType, UNVERIFIED);
   }

   /**
    * Setting reviews are keyed by exact runtime class and exact config name. New settings
    * therefore remain UNVERIFIED until they receive their own explicit entry.
    */
   public static SettingAssessment assessSetting(Class<?> moduleType, String settingName) {
      Assessment module = assess(moduleType);
      String name = settingName == null || settingName.isBlank() ? "unnamed setting" : settingName;
      SettingAssessment reviewed = REVIEWED_SETTINGS.get(new SettingKey(moduleType, name));
      if (reviewed != null) {
         return reviewed;
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

   private static Map.Entry<SettingKey, SettingAssessment> setting(
      Class<?> moduleType,
      String settingName,
      Classification classification,
      String reason
   ) {
      Assessment module = assess(moduleType);
      return Map.entry(
         new SettingKey(moduleType, settingName),
         new SettingAssessment(classification, module.classification(), true, reason)
      );
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

   private record SettingKey(Class<?> moduleType, String settingName) {}

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
