# Multiplayer compatibility review

**Client target:** Minecraft Java Edition 1.21.11 / Fabric  
**Audit basis:** source inspection of packet/event hooks and selected movement, combat, interaction and exploit modules; no server session was run.

## Protocol facts and boundaries

In 1.21.11, movement packets carry position/rotation change flags and an on-ground flag; the protocol also defines distinct serverbound player action, command, entity-interaction, block-use and item-use packets. The mapped server listener has separate handlers for those packet types. See [1.21.11 movement packet mappings](https://mappings.dev/1.21.11/net/minecraft/network/protocol/game/ServerboundMovePlayerPacket.html), [1.21.11 server packet listener mappings](https://mappings.dev/1.21.11/net/minecraft/network/protocol/game/ServerGamePacketListener.html) and the [Java Edition packet reference](https://minecraft.wiki/w/Java_Edition_protocol/Packets).

Fabric's networking documentation describes packets as the client/server state bridge and warns that unsynchronized client/server state produces desynchronization. It does not define how every server or plugin validates movement, reach, timing or interactions. A packet being structurally valid does not establish that its state transition is vanilla-consistent or accepted by a server.

## Source-reviewed disposition

| Module or group | Observed behavior from source | Compatibility status |
| --- | --- | --- |
| `NoFall` | Its Packet mode changes the outgoing movement packet's on-ground flag after tracked fall distance exceeds 2.1 blocks. | `PACKET_AFFECTING`; not vanilla-consistent and not supported on multiplayer. |
| `Reach` | Changes reach distance up to 6 blocks and advertises illegal interactions. | `SERVER_SENSITIVE`; not vanilla-consistent and not supported on multiplayer. |
| `Phase` / `AntiPhase` | Phase changes collision/position behavior; AntiPhase automates block placement against pearl phasing. | `SERVER_SENSITIVE` / `WORLD_INTERACTION`; unsupported pending controlled testing. |
| `ElytraFly` | Implements non-vanilla flight modes and mode-specific movement behavior. | `SERVER_SENSITIVE`; server/plugin dependent and unsupported pending testing. |
| `Criticals` | Offers Packet, NCP and Grim-named modes to alter attack outcomes. | `SERVER_SENSITIVE`; server-validation-sensitive. |
| `NoSlow`, `FastClimb` | Change movement slowdown or climbing behavior; some modes need packet/input review. | `SERVER_SENSITIVE` / `INPUT_MODIFYING`; per-setting review pending. |
| `AntiHunger`, `PingSpoof`, `FakeLag`, `PortalGodMode`, `Warp` | Alter movement-related state or delay, cancel, retain or replay packets. | `PACKET_AFFECTING`; packet timing/order can diverge from vanilla behavior. |
| `FastPlace`, `SpeedMine` | Change the cadence of block placement or mining interactions. | `WORLD_INTERACTION`; server/plugin dependent. |
| `Settings`, `Colors`, `Fonts`, `HUD` | Local client configuration and presentation only in the reviewed code paths. | `CLIENT_ONLY`; technical code-path label, not an endorsement under any server's rules. |

“Unsupported” means Schizo has not demonstrated vanilla-consistent behavior for that feature and does not claim compatibility. It does not predict a specific server's response.

## In-client compatibility inventory

Use:

- `;compat` or `;compat status` for counts by classification.
- `;compat list` to list modules that are not marked client-only or visual-only.
- `;compat list <CLASSIFICATION>` to filter the inventory.
- `;compat module <ModuleClass>` for the module rationale and each setting's current review state.

The first-pass registry labels only the modules listed above. **Every other module defaults to `UNVERIFIED`**. Runtime warnings are shown when a module in one of the warning-required classes is enabled in multiplayer; warnings are deduplicated per module per connection and capped in chat. The inventory identifies every individual setting as `UNVERIFIED` until that setting has received its own source review. A module-level label is not a per-setting certification.

The report is informational only. It does not modify packets, attempt to evade server validation, or silently change module state. A strict activation profile is deliberately not enabled while the full module/setting matrix is incomplete; adding a deny policy before classifying the remaining modules would make the result misleading and could unexpectedly break user configurations.

## Tests and evidence

- **Source inspected:** the removed validation-circumvention modules and their registration/rebuild paths, plus representative movement, combat, packet-delay and interaction modules listed above.
- **Build-tested:** the project compiles and tests pass in GitHub Actions. Compilation does not validate server behavior.
- **Controlled multiplayer tested:** none.
- **Public-server tested:** none.
- **Server/plugin-specific compatibility:** none established.
- **Protocol behavior tested:** no packet-capture comparison against an unmodified vanilla client was performed.

## Remaining compatibility work

A complete matrix still needs to inspect every relevant module and each setting, then run reproducible scenarios on a locally controlled 1.21.11 server with the exact server build and plugin configuration recorded. Movement corrections, interaction outcomes, teleport confirmation, inventory synchronization and packet order need separate assertions. Schizo makes no claim of universal server compatibility.
