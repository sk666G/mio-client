# Multiplayer compatibility review

**Client target:** Minecraft Java Edition 1.21.11 / Fabric  
**Audit basis:** source inspection of packet/event hooks and selected movement, combat, interaction and exploit modules; no server session was run.

## Protocol facts and boundaries

In 1.21.11, movement packets carry position/rotation change flags and an on-ground flag; the protocol also defines distinct serverbound player action, command, entity-interaction, block-use and item-use packets. The mapped server listener has separate handlers for those packet types. See [1.21.11 movement packet mappings](https://mappings.dev/1.21.11/net/minecraft/network/protocol/game/ServerboundMovePlayerPacket.html), [1.21.11 server packet listener mappings](https://mappings.dev/1.21.11/net/minecraft/network/protocol/game/ServerGamePacketListener.html) and the [Java Edition packet reference](https://minecraft.wiki/w/Java_Edition_protocol/Packets).

Fabric's networking documentation describes packets as the client/server state bridge and warns that unsynchronized client/server state produces desynchronization. It does not define how every server or plugin validates movement, reach, timing or interactions. A packet being structurally valid does not establish that its state transition is vanilla-consistent or accepted by a server.

## Disposition

| Module or group | Observed behavior from source | Compatibility status |
| --- | --- | --- |
| `GrimDisabler` | Delayed ping handling and player flight-state manipulation during elytra/firework sequences. | Removed from source and registration. |
| `RaytraceBypass` | Explicitly described as bypassing NCP raytracing; searches/sends alternative rotations. | Removed from source and registration. |
| `NoFall` | Its Packet mode changes the outgoing movement packet's on-ground flag after tracked fall distance exceeds 2.1 blocks. | Not vanilla-consistent; unsupported on multiplayer. |
| `Reach` | Changes reach distance up to 6 blocks and advertises illegal interactions. | Not vanilla-consistent; unsupported on multiplayer. |
| `Phase` / `AntiPhase` | Phase changes collision/position behavior; AntiPhase automates block placement against pearl phasing. | Server-state-sensitive; unsupported pending controlled testing. |
| `ElytraFly` | Implements non-vanilla flight modes and mode-specific movement behavior. | Server/plugin dependent; unsupported pending controlled testing. |
| `Criticals` | Offers Packet, NCP and Grim-named modes to alter attack outcomes. | Server-validation-sensitive; unsupported pending controlled testing. |
| `NoSlow`, `AntiHunger`, `FastClimb`, `FastPlace`, `SpeedMine` | Change movement, sprint, climbing, placement cadence or mining progression; several modes send or alter packets. | Vanilla behavior is not preserved by every setting; unsupported pending per-setting tests. |
| `PingSpoof`, `FastLatency`, `FakeLag`, `PortalGodMode`, `Warp` | Delay, cancel, retain or replay keepalive, movement, teleport-confirmation or other packets; Warp also accumulates/replays movement. | Packet timing/order or server state can diverge; unsupported on multiplayer. |

“Unsupported” means Schizo has not demonstrated vanilla-consistent behavior for that feature and does not claim compatibility. It does not predict a specific server's response.

## Tests and evidence

- **Source inspected:** the two removed modules, their registry and rebuild-script paths, plus representative movement, combat, packet-delay and interaction modules listed above.
- **Build-tested:** the project compiles and tests pass in GitHub Actions. Compilation does not validate server behavior.
- **Controlled multiplayer tested:** none.
- **Public-server tested:** none.
- **Server/plugin-specific compatibility:** none established.
- **Protocol behavior tested:** no packet-capture comparison against an unmodified vanilla client was performed.

## Remaining compatibility work

The source tree still contains modules whose settings intentionally alter player state, packet timing, reach or interaction cadence. This branch does not yet provide a conservative multiplayer activation profile or a per-setting warning/deny mechanism. Those require a reviewed compatibility classification and explicit tests before being represented as safe. For now, users should treat the listed behaviors as unsupported, and Schizo makes no claim of universal server compatibility.

A complete matrix still needs to inspect every relevant module and setting, then run reproducible scenarios on a locally controlled 1.21.11 server with the exact server build and plugin configuration recorded. Movement corrections, interaction outcomes, teleport confirmation, inventory synchronization and packet order need separate assertions.


## Compatibility classifications and conservative profile

Modules and settings carry a `CompatibilityAssessment`: one of `CLIENT_ONLY`, `VISUAL_ONLY`, `INPUT_MODIFYING`, `WORLD_INTERACTION`, `PACKET_AFFECTING`, `SERVER_SENSITIVE`, or `UNVERIFIED`, a required explanation, and evidence labeled unreviewed, source reviewed (not server tested), or controlled server tested. The default is `UNVERIFIED`.

The ClickGUI hover text displays the assessment for each module and shows a separate warning for risky settings. Source-reviewed assessments currently cover Criticals, AntiPhase, FastLatency, Phase, PingSpoof, Reach, ElytraFly, FakeLag and NoFall based on their actual packet, movement, reach or interaction code, plus Metrics and Frame Time Graph (local rendering), Settings (local preferences), and UI (ClickGUI presentation). These assessments are source-only; none represents a server test.

The Settings module contains a persisted `Conservative Multiplayer` toggle, off by default. When enabled in a multiplayer world it blocks enabling `PACKET_AFFECTING`, `SERVER_SENSITIVE` and `UNVERIFIED` modules before changing their state and reports the reason in chat and the log. It does not rewrite keybinds or stored preferences and does not reactivate anything after disconnect. Context Profiles own selected settings while active; user edits to those selected setting values are restored to the pre-profile baseline at the next profile transition. Turning the toggle off is the explicit global override. Classification is a risk explanation, not a claim that the remaining classes are universally accepted.

No controlled server test was performed. The ClickGUI search matches classification names, evidence labels, explanations, setting names and setting explanations. The full module/setting classification sweep and explicit screen-reader support remain incomplete; all unreviewed behavior remains explicitly unverified.
