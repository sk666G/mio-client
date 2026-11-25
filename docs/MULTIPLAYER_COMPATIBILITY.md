# Multiplayer compatibility review

**Client target:** Minecraft Java Edition 1.21.11 / Fabric  
**Audit basis:** class-level source inspection of all 178 explicitly registered modules, their packet/event hooks and relevant mixin/manager consumers; no server session was run. Individual settings remain separately unverified unless documented otherwise.

## Protocol facts and boundaries

In 1.21.11, movement packets carry position/rotation change flags and an on-ground flag; the protocol also defines distinct serverbound player action, command, entity-interaction, block-use and item-use packets. The mapped server listener has separate handlers for those packet types. See [1.21.11 movement packet mappings](https://mappings.dev/1.21.11/net/minecraft/network/protocol/game/ServerboundMovePlayerPacket.html), [1.21.11 server packet listener mappings](https://mappings.dev/1.21.11/net/minecraft/network/protocol/game/ServerGamePacketListener.html) and the [Java Edition packet reference](https://minecraft.wiki/w/Java_Edition_protocol/Packets).

Fabric's networking documentation describes packets as the client/server state bridge and warns that unsynchronized client/server state produces desynchronization. It does not define how every server or plugin validates movement, reach, timing or interactions. A packet being structurally valid does not establish that its state transition is vanilla-consistent or accepted by a server.

## Source-reviewed disposition

| Module or group | Observed behavior from source | Compatibility status |
| --- | --- | --- |
| `NoFall` | Its Packet mode changes the outgoing movement packet's on-ground flag after tracked fall distance exceeds 2.1 blocks. | `PACKET_AFFECTING`; not vanilla-consistent and not supported on multiplayer. |
| `NoRender` | Cancels selected clientbound chunk-cache-radius and End fire block-update packet events; other settings hide local render effects. | `PACKET_AFFECTING`; affects client packet processing, with setting-level review pending. |
| `FreeLook` | Separates camera rotation from player rotation and changes the view vector used for entity/block picking. | `SERVER_SENSITIVE`; interaction targeting can diverge from the player's reported rotation. |
| `Reach` | Changes reach distance up to 6 blocks and advertises illegal interactions. | `SERVER_SENSITIVE`; not vanilla-consistent and not supported on multiplayer. |
| `Phase` / `AntiPhase` | Phase changes collision/position behavior; AntiPhase automates block placement against pearl phasing. | `SERVER_SENSITIVE` / `WORLD_INTERACTION`; unsupported pending controlled testing. |
| `ElytraFly` | Implements non-vanilla flight modes and mode-specific movement behavior. | `SERVER_SENSITIVE`; server/plugin dependent and unsupported pending testing. |
| `Criticals` | Offers Packet, NCP and Grim-named modes to alter attack outcomes. | `SERVER_SENSITIVE`; server-validation-sensitive. |
| `NoSlow`, `FastClimb`, `ViewClip`, `Zoom` | Change movement slowdown, climbing or mouse-wheel input behavior; Zoom changes local FOV and consumes scroll input. | `SERVER_SENSITIVE` / `INPUT_MODIFYING`; per-setting review pending. |
| `AntiHunger`, `PingSpoof`, `FakeLag`, `PortalGodMode`, `Warp` | Alter movement-related state or delay, cancel, retain or replay packets. | `PACKET_AFFECTING`; packet timing/order can diverge from vanilla behavior. |
| `FastPlace`, `SpeedMine` | Change the cadence of block placement or mining interactions. | `WORLD_INTERACTION`; server/plugin dependent. |
| `Settings`, `Colors`, `Fonts`, `HUD` | Local client configuration and presentation only in the reviewed code paths. | `CLIENT_ONLY`; technical code-path label, not an endorsement under any server's rules. |
| `Ambience`, `Animations`, `Blur`, `Borders`, `BreakHighlight`, `Chams`, `Crosshair`, `ESP`, `Glint`, `Highlight`, `Hitmarker`, `HoleESP`, `LogoutSpots`, `NameTags`, `NoBob`, `Particles`, `PhaseESP`, `Search`, `Shader`, `Skeleton`, `Skybox`, `Tooltips`, `Trails`, `Trajectories`, `Tracers`, `ViewModel`, `VoidESP`, `Waypoints`, `Xray` | Local render/camera/HUD/world overlays. ESP, Hitmarker, LogoutSpots, Skybox and VoidESP observe packet/player-info events but do not rewrite outbound gameplay packets in the reviewed paths. | `VISUAL_ONLY`; setting-level review remains incomplete. |

“Unsupported” means Schizo has not demonstrated vanilla-consistent behavior for that feature and does not claim compatibility. It does not predict a specific server's response.

## In-client compatibility inventory

Use:

- `;compat` or `;compat status` for counts by classification.
- `;compat list` to list modules that are not marked client-only or visual-only.
- `;compat list <CLASSIFICATION>` to filter the inventory.
- `;compat module <ModuleClass>` for the module rationale and each setting's current review state.

The registry now contains an explicit source-reviewed assessment for **all 178 registered modules**. Current class-level distribution is: **32 `PACKET_AFFECTING`**, **28 `SERVER_SENSITIVE`**, **51 `WORLD_INTERACTION`**, **8 `INPUT_MODIFYING`**, **21 `CLIENT_ONLY`** and **38 `VISUAL_ONLY`**. CI compares `ModuleRegistry` directly with `ModuleCompatibility` so a newly registered module cannot silently fall back to an unreviewed class-level state. Runtime warnings are shown when a module in one of the warning-required classes is enabled in multiplayer; warnings are deduplicated per module per connection and capped in chat. The setting-level pass has begun separately: **260 settings currently have explicit individual assessments** across Timer, Trident, Swing, NoPacketKick, NoFall, PingSpoof, FakeLag, Reach, FreeLook and NoRender. Every other setting remains `UNVERIFIED` until it receives its own source review. A module-level label is not a per-setting certification. In remote multiplayer, hover any ClickGUI setting to see its explicit `UNVERIFIED` review state and the parent module classification in the existing tooltip; local and single-player tooltips remain unchanged.

The report does not modify packets or attempt to evade server validation. The default policy is **WARN_ONLY** and leaves module activation unchanged.

An optional conservative activation policy is available for user-toggleable modules. Always-on `PermanentModule` services cannot be safely disabled through the ordinary module lifecycle, so they remain warning-only until their individual settings have dedicated enforcement and tests:

- `;compat policy status` shows the current policy and explicit exceptions.
- `;compat policy conservative` blocks enabling `SERVER_SENSITIVE`, `PACKET_AFFECTING` and `UNVERIFIED` modules while connected to a remote multiplayer server.
- `;compat policy warn` returns to warning-only mode.
- `;compat policy allow <ModuleClass>` adds an explicit class-level exception for user-toggleable modules. The command accepts the module name, but the saved exception is keyed by its fully qualified runtime class name, so another class with the same simple name is not affected. Permanent services cannot be excepted as a whole.
- `;compat policy deny <ModuleClass>` removes that exception; `;compat policy clear` removes all exceptions.
- `;compat policy reset <ModuleClass>` explicitly saves the module as disabled, clearing any preserved enable preference from a previous policy suspension.

The conservative policy is opt-in because most modules and all individual settings have not completed source review. `CLIENT_ONLY`, `VISUAL_ONLY`, `INPUT_MODIFYING` and `WORLD_INTERACTION` modules are not blocked by this first policy; the latter two continue to show warnings. An exception is a user permission, not a compatibility certification. Use `reset` when you want to clear a suspended module's saved enabled preference instead of retaining it for a future warning-only startup.

When the conservative policy is active, already-enabled blocked modules are suspended on the first player tick in a remote multiplayer session. Their saved enabled preference is preserved in the module config, but they are not automatically re-enabled on disconnect or when the policy changes to warning-only. Toggle them manually, or restart under warning-only mode to restore saved enable preferences. The policy and its explicit exceptions are stored separately in `multiplayer-compatibility.json` (schema version 2, with fully qualified class names). Older schema versions are ignored rather than reinterpreting simple-name exceptions.

## Tests and evidence

- **Source inspected:** every explicitly registered module class (178/178), the removed validation-circumvention modules and their registration/rebuild paths, plus packet/mixin/manager consumers needed to classify indirect behavior.
- **Build-tested:** the project compiles and tests pass in GitHub Actions. Compilation does not validate server behavior.
- **Controlled multiplayer tested:** none.
- **Public-server tested:** none.
- **Server/plugin-specific compatibility:** none established.
- **Protocol behavior tested:** no packet-capture comparison against an unmodified vanilla client was performed.

## Remaining compatibility work

The **class-level module matrix is complete**. Setting-level review is in progress (260 explicit setting assessments at this checkpoint). Remaining review work is: inspect settings that materially change networking, movement, interaction timing or compatibility policy, then run reproducible scenarios on a locally controlled 1.21.11 server with the exact server build and plugin configuration recorded. Movement corrections, interaction outcomes, teleport confirmation, inventory synchronization and packet order need separate assertions. Schizo makes no claim of universal server compatibility.
