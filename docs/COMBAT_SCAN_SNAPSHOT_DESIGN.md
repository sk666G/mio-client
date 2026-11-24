# Combat scan worker snapshot design

**Status:** Design only. AutoCrystal and AnchorAura still read live Minecraft world and entity state from worker threads. This document defines the safe replacement; it does not claim that the concurrency issue is fixed.

## Problem statement

The current asynchronous scans are cancellable, but cancellation is not data isolation. A worker can enter a helper that reads the active client world while the world is ticking, entities are moving, a packet is being applied, or the client is disconnecting.

The current dependency paths are broader than the scan-loop coordinates:

- **Crystal placement validation** reads the base, crystal-space and optional upper block states; fluid state; all rendered entities; entity alive/removed state and bounding boxes; interpolated and predicted player bounds; player eye position; and prediction-manager data.
- **Crystal visibility and placement helpers** call WorldUtils and BlockInteractionUtils, which read the active ClientLevel, collision state, placement occupancy, entity collections and player state. Visibility and explosion exposure use block raycasts.
- **Crystal damage** evaluates target bounds, health/absorption, armor, toughness, resistance effects, armor enchantments, active blocking state, social policy, damage sources and world difficulty. Exposure samples a target AABB and raycasts each sample through the world.
- **Anchor placement validation** reads fluid and block states, support faces, occupied placement state, blocking entities and world collision data.
- **Anchor damage** enumerates live players, reads their current bounds/health state, uses explosion exposure raycasts, and applies live world difficulty and damage reduction.

Capturing only the player's coordinates or replacing a shared MutableBlockPos does not address these dependencies.

## Required ownership model

1. The client thread owns all reads from Minecraft client state. It captures one immutable scan snapshot for a specific world/session generation.
2. A calculation worker accepts only immutable snapshot data and immutable settings. Its call graph must not reference ClientAccess, ClientContext, Minecraft, ClientLevel, LocalPlayer, Entity or mutable manager instances.
3. A worker returns an immutable result. It returns stable entity identifiers, not live entity references.
4. The client thread resolves those identifiers and revalidates the candidate against the current world before any rotation, inventory change, packet, placement or attack.
5. A result is discarded when its captured session/world generation is no longer current.

## Snapshot contents

The builder should capture only the region and inputs required by the configured scan, with explicit upper bounds on memory and work.

| Snapshot group | Required data |
|---|---|
| Session | Monotonic world/session generation, dimension identifier, client tick sequence, and a validity token checked again when applying results |
| Player | Position, eye position, bounding box, rotation, game mode flags, relevant health/absorption, equipment and active-use/blocking state |
| Candidate targets | Entity id plus UUID, type, alive/dead/spectator/creative flags, friend/server-death policy result, health/absorption, armor/toughness, resistance, armor enchantment contribution, relevant held item and blocking state |
| Collision entities | Stable type/identity, alive/removed state, bounding box, item age where relevant, and player interpolation/prediction bounds for the configured prediction window |
| Blocks | Immutable block and fluid states for the scan volume plus required support/upper/raycast margin; placement occupancy, mining and ignored-block decisions captured as data |
| Raycast | Immutable collision/raycast inputs for the snapshot. Worker visibility and explosion-exposure queries must use the snapshot, never the active ClientLevel |
| Rules/settings | A single captured set of range, damage, direction, placement, prediction and policy settings used by the scan |

Avoid copying unrelated chunks or all world entities. Measure snapshot size and capture time, and reject a snapshot that exceeds its fixed budget rather than allowing unbounded memory growth.

## Worker result contract

Crystal and anchor calculations should return data-only records such as:

- candidate block position and interaction direction;
- target entity id and UUID;
- predicted/observed damage values used to rank the candidate;
- placement kind and the relevant scan generation.

Do not store LivingEntity, Player, ClientLevel, mutable BlockPos, ItemStack, or manager references in the result. Use immutable positions and primitive/value fields.

## Client-thread revalidation

Before applying a worker result, the client thread must verify:

- the same world/session generation and dimension are still active;
- the target entity id still resolves to the same UUID and is still eligible;
- the target remains in range and the relevant friend/death/game-mode policy still permits it;
- the candidate block and required surrounding blocks still match the placement assumptions;
- collision, support direction, visibility, self-damage and target-damage constraints still pass;
- the player and inventory are still in a state where the requested action is valid;
- the module remains enabled and its settings still correspond to the captured scan configuration.

A failed check discards the result and lets the next client-thread snapshot schedule a fresh scan. Do not try to repair stale results by using old entity objects.

## Bounded scheduling and cancellation

- Keep at most one active or queued scan per module unless profiling proves that a different bound is necessary.
- Superseding a snapshot cancels the prior task and advances a generation. Cancellation is a CPU-saving hint; generation validation remains the correctness barrier.
- Do not submit cancelled work to an unbounded executor queue. Bound outstanding snapshots and define an explicit rejection path.
- Check interruption inside candidate loops and raycast sampling loops.
- On disable, disconnect, dimension/world replacement or module setting changes that alter scan semantics, invalidate the generation and discard pending results.
- Return to the client thread for all result application.

## Migration sequence

1. Extract the damage inputs and math into data-only value types. Preserve the existing calculation as the reference implementation for fixtures.
2. Implement a bounded client-thread snapshot builder for block, entity, player, prediction, policy and settings data.
3. Implement snapshot-only visibility, collision and explosion-exposure calculations. Do not route these through WorldUtils, BlockInteractionUtils or RaycastUtils while they still dereference the live client world.
4. Convert AutoCrystal first. Add deterministic parity fixtures for base eligibility, predicted player collision, visibility, ignored blocks, damage and lethality before enabling the snapshot worker path.
5. Convert AnchorAura using the same shared snapshot/raycast layer and its own deterministic fixtures for support direction, fluids, anchor blocks and damage.
6. Add controlled local-world integration tests for world replacement, entity removal/id reuse, target movement, block changes, module disable and cancellation.
7. Remove the old live-world worker entry points only after parity and integration tests pass. Keep any temporary fallback strictly on the client thread and bounded per tick.

## Acceptance tests

- Pure calculator package has no imports or static references to Minecraft client state or client-global managers.
- A worker can execute using only a serialized/frozen fixture snapshot with no Minecraft client initialized.
- Mutating the source world after snapshot capture does not change the snapshot's result.
- A world/session generation change invalidates a completed or late worker result.
- Entity id reuse with a different UUID invalidates the result.
- Worker interruption and queue saturation remain bounded and cannot leave placement gates, swaps or pending state stuck.
- Fixture tests compare the snapshot path against known current behavior for block eligibility, collision, exposure, damage and target ordering.
- Controlled local integration tests verify the final client-thread revalidation before action.

No anti-cheat bypass, server-validation evasion, packet spoofing or new exploit behavior is part of this work. The goal is to make the existing calculations deterministic, session-safe and thread-correct.
