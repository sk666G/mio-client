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

## Source dependency audit (2026-11-24)

The current implementation was traced from both scheduling sites through their direct calculation helpers. This inventory is intentionally explicit so the migration does not stop after copying coordinates.

### AutoCrystal path

- `AutoCrystalModule.onMinecraftRunTickEvent` schedules `calculatePlacement` on the calculation executor. The submitted closure retains the module instance and a `List<LivingEntity>`, and the calculation reads settings and mutable module fields while it runs.
- `AutoCrystalTargeting.snapshot` copies only the list container. Its elements remain live `LivingEntity` objects. `refresh` applies friend/death/game-mode policy using live entities.
- `CrystalPlacementValidator.canPlace` reads the active level's block/fluid states, world-state manager's unconfirmed-break position, rendered entity collection, live entity flags and bounds, player eye position, player wall state, and movement prediction manager.
- `CrystalTargetDamage.calculateForBase` reads live target position/bounds/equipment, module settings and flags, social registry, player identity, and calls back into module damage helpers. Its result record stores a live `LivingEntity`.
- `AutoCrystalModule.calculatePlacement` also reads `WorldUtils` for block identity and visibility, `BlockInteractionUtils` for strict directions/support/place checks, `DamageUtils` for self and target damage, and `RotationManager` / SpeedMine settings while the worker is active.
- `DamageUtils` is not a pure calculator: explosion exposure builds a `BlockRaycastContext` with a live entity, `RaycastUtils.raycast` fetches each block from `Minecraft.level` and asks the live level to compute interaction clipping, and damage reduction reads live world difficulty/damage sources plus target effects, attributes, armor stacks and blocking item.
- `WorldUtils.canSee` delegates to that same live-world raycast path. `BlockInteractionUtils` placement and support helpers likewise consult the active world and entity state.

### AnchorAura path

- `AnchorAuraModule.updateCalculation` submits `calculatePlacement` to `ClientAccess.executorService`. The closure retains the module instance; the worker reads live player coordinates, settings, mining state and world state.
- Each candidate invokes `BlockInteractionUtils` for bounds, support and placement checks; `WorldUtils` for occupied positions and visibility; and `AnchorTargetDamage.calculateForBase` for live world players, target policy, entity bounds and damage.
- `AnchorTargetDamage` enumerates `ClientLevel.players()`, calls live `Player` methods and stores the selected `Player` in its result record.
- `AnchorAuraModule.shouldRejectDamage` recomputes self/target damage through `DamageUtils` and reads live player health, bounds, anchor state and mining state. It is currently called from the worker path.

### Migration implications

1. A shallow copy of `AutoCrystalTargeting.targets`, or a record that stores `BlockState` alongside a live `Entity`, is not a sufficient snapshot.
2. The current result types (`CrystalPlacementResult`, `CrystalTargetDamage`, `AnchorPlacementResult`, and `AnchorTargetDamage`) must become data-only worker results. Resolve entity IDs/UUIDs on the client thread after completion.
3. `DamageUtils` needs a separate pure damage calculator whose inputs include captured difficulty, damage-reduction attributes/effects, equipment/enchantment contributions, blocking state, and precomputed exposure. The existing method should remain a reference oracle for fixture comparisons until parity is demonstrated.
4. `RaycastUtils` needs a snapshot-backed ray traversal implementation. Reusing `BlockGetter.traverseBlocks` is possible only if its block/fluid/shape lookups are served entirely by immutable captured data; passing the live `ClientLevel` defeats the boundary.
5. Snapshot capture must be performed before executor submission on the client thread. If capture cannot fit the configured budget, skip that scan and retain the last validated result rather than reading live world state from the worker.
6. Before applying any result, the client thread must resolve target identity and rerun action-critical checks. The worker result is advisory, not permission to act.

This audit changes no gameplay behavior. It records why the existing worker code is still unsafe and the concrete class boundary that the implementation must replace.
## First pure-calculation extraction

`ExplosionDamageMath` now owns the primitive-only explosion impact and difficulty calculation used by `DamageUtils`. Its inputs are normalized distance, captured exposure, explosion power, and an explicit difficulty enum; it does not read Minecraft state, modules, entities, or world objects. The existing `DamageUtils` path captures difficulty and exposure, delegates the mathematical portion, then continues through the existing armor/effect/enchantment and blocking reductions.

Focused unit coverage checks the explosion boundary, full exposure at the center, zero exposure, and all difficulty branches. This is deliberately only the first extraction: exposure raycasting and post-impact damage reduction still depend on live Minecraft objects and must be converted separately before either combat scanner can safely use the complete damage path off-thread.

Parity requirement: preserve the exact floating-point operation order and current difficulty behavior when further stages are extracted. Do not replace the current raycast or armor pipeline with approximate formulas without fixtures against the existing implementation.
### Collision-context boundary

`BlockRaycastContext` currently creates `CollisionContext.of(entity)` in its constructor and passes that context into block and fluid shape selection. A snapshot raycast must not carry that context or the source `Entity` to a worker. Before implementing the replacement, enumerate the exact collision-context queries used by the target Minecraft version and capture their required values into a small immutable profile (for example, movement/descending state, minimum Y, relevant held-item/fluid interaction state, and any entity dimensions consulted by contextual shapes). The snapshot shape resolver must consume only that profile and immutable block/fluid shape data.

`CollisionContext.of(entity)` is not just an AABB: for a normal entity it captures descending state, entity-bottom Y, main-hand item, and fluid-collision mode, but its `canStandOnFluid` query still calls back into the live `LivingEntity`. An experimental-movement minecart can receive a distinct `MinecartCollisionContext`. These cases must be represented explicitly or their relevant shape results precomputed; carrying the original context into a worker is not acceptable. Schizo's combat/visibility path does **not** call `ClientLevel.clip` or `BlockGetter.clip`: `RaycastUtils.raycast(BlockRaycastContext)` runs `BlockGetter.traverseBlocks`, reads each block state from the live `ClientLevel`, calculates the context-dependent collision shape, and calls `ClientLevel.clipWithInteractionOverride`, which may also read the block's interaction shape. This custom path currently does not query fluid shapes (although `BlockRaycastContext` has a fluid-shape method for other callers). `WorldUtils.canSee` relies on the builder's default local-player collision context, while `DamageUtils.getExposure` explicitly replaces it with the target entity context. For **exposure's hit/miss result**, the collision shape is sufficient: `clipWithInteractionOverride` only consults the interaction shape after the collision shape has already hit, and it changes the returned face, not whether the result is a miss. Any future caller that depends on hit face or hit location must capture/reproduce interaction-shape behavior too; `ExplosionRaycastSnapshot` deliberately exposes only HIT/MISS/INCOMPLETE and must not be used for that richer contract. The snapshot ray path needs separate captured collision profiles for the local player and each damage target, and it must not implement an unnecessary fluid traversal as a substitute for the exact current behavior.\n\nDo not assume that caching `BlockState` alone is enough: state shape queries can depend on the `BlockGetter`, block position, and collision context. The builder must either precompute the relevant immutable collision shapes on the client thread for each captured position/profile, or provide a snapshot-backed read-only `BlockGetter` whose every lookup is bounded to captured data. No fallback to the live `ClientLevel` is allowed.

### Damage-reduction boundary

`DamageUtils.applyDamageReduction` reads armor value, armor toughness, resistance effect/amplifier, per-piece enchantment levels, and the AutoCrystal assume-best-armor setting. It then invokes vanilla damage-source and combat-rule APIs and applies protection reduction. `resolveBlockingDamage` additionally reads the active use item component, head yaw, target position/view vector, and explosion direction. Capture all inputs on the client thread and move only the mathematically necessary values into a pure calculator. If a vanilla helper requires a live `LivingEntity` or `DamageSource`, it cannot be called from the worker; reproduce its behavior only with a verified equivalent and fixed fixtures across armor/toughness/effect/enchantment/blocking cases.
## Second pure-calculation extraction

`ExplosionDamageReductionMath` now owns the post-impact armor, resistance, and protection stages as a primitive-only calculation. `DamageUtils.applyDamageReduction` remains the live-state adapter: it reads armor/toughness, resistance effect state, module configuration, armor stacks, and enchantment levels, then passes the captured numbers to the pure calculator.

The armor step preserves the 1.21.11 client-side `CombatRules` operation order: toughness scaling, effective armor calculation, 0.2 minimum armor ratio, 20-point cap, division by 25, then multiplication by the remaining damage factor. The source's weapon-effectiveness branch is server-level-only and does not run for the client-level damage calculation used here. The extracted method also preserves the existing resistance fallback used by Assume Best Armor (`1.0F` when no effect instance is available), the resistance/protection order, protection upper cap, and final non-negative clamp.

Focused tests cover armor/toughness reduction, resistance ordering, protection ordering and cap, the Assume Best Armor fallback, negative final damage, and the crossed armor-bound clamp edge case. These tests exercise the pure calculator; they do not yet prove the live-state adapter is safe on a worker thread.

**Important remaining limitation:** `DamageUtils.applyDamageReduction` still reads live entity, inventory, effect, module, and registry state while obtaining those primitive inputs. This extraction makes the math reusable with an immutable snapshot but does not itself migrate AutoCrystal or AnchorAura off live world/entity references.
## Third pure-calculation extraction

`ExplosionBlockingDamageMath` now handles the `BlocksAttacks` damage-reduction rules using a primitive-only `Reduction` record: horizontal blocking angle, already-resolved damage-type eligibility, base reduction, and damage factor. It preserves the 1.21.11 component behavior: each matching rule contributes a clamped base-plus-factor value, contributions are summed, and the total is clamped to the incoming damage.

`DamageUtils.resolveBlockingDamage` remains responsible for reading the active item component, comparing its optional damage-type holder set against the current explosion damage type, and calculating the entity-relative horizontal angle. Those are live inputs and still belong in the future client-thread snapshot builder. The new helper contains no Minecraft or item-component references.

Together, the impact, armor/resistance/protection, and blocking arithmetic are now split into pure calculation classes. The full damage path is still not worker-safe: exposure requires block raycasts, and `DamageUtils` adapters still collect target, inventory, effect, item, module, and world data directly.
## Fourth pure-calculation extraction

`ExplosionExposureMath` now owns the sample-grid calculation from target bounds to explosion rays. It accepts primitive bounds and explosion coordinates and calls a supplied visibility tester for each sample. `DamageUtils.getExposure` supplies the existing `RaycastUtils.raycast` callback, so the ray behavior is unchanged while the sampling geometry can now be reused by a snapshot-only tester.

Tests cover the eight-sample degenerate-bound case, endpoint forwarding, negative-step early exit and null tester rejection. The callback boundary is important: the helper itself is pure, but the current callback still reaches the active `ClientLevel` through `RaycastUtils`. This is a sampling extraction, not a completed exposure snapshot.
## Snapshot-only ray infrastructure

The first reusable ray snapshot layer is now present, but AutoCrystal and AnchorAura have not been switched to it.

- `ExplosionRaycastSnapshot` stores packed block-position keys and immutable copies of block-local collision-box lists plus explosion-resistance values. It is intentionally a hit/miss-only contract for explosion exposure: it does not return a hit face or location. It retains world-generation, capture-tick, dimension and collision-entity id/UUID metadata alongside those value objects; it does not retain `BlockState`, `Level`, `Entity`, `CollisionContext`, or managers.
- Its ray method uses the static `BlockGetter.traverseBlocks` DDA with the snapshot as callback data, not as a live `BlockGetter`. It resolves only captured box geometry using the static `AABB.clip` helper and the same initial-point-in-shape check as `VoxelShape.clip`.
- The result is tri-state: `HIT`, `MISS`, or `INCOMPLETE`. A block position missing from the capture returns `INCOMPLETE`; it is never silently treated as air. Query overrides preserve the current classifier order: ignored positions, explicit solid position, then low-resistance filtering.
- `ExplosionRaycastSnapshotBuilder` is a separate client-thread adapter. It checks the active `ClientLevel`, collision entity world identity and client-thread ownership; bounds the capture to 32,768 blocks by default; rejects unloaded chunks or regions outside build height; computes context-dependent collision shapes on the client thread; and flattens them to AABBs before the snapshot is handed off. The returned snapshot carries the collision entity's stable id/UUID so a damage profile cannot be paired with another entity's collision geometry.
- `ExplosionRaycastSnapshotBuilder.Region.enclosingRays` creates a padded integer region enclosing the target AABB and explosion endpoint. The padding accounts for the existing exposure sample offsets and ray traversal boundary handling.
- `ExplosionExposureMath.calculateFromSnapshot` can now use this value-only ray layer. It propagates an unknown ray as an incomplete exposure result so the caller can discard the scan rather than ranking a candidate using guessed exposure.

Current limit: this is only the geometry/exposure foundation. No combat module calls the builder or snapshot yet. The next work is to define one bounded scan-wide capture that covers every candidate ray and relevant target collision profile, add world-generation/settings/entity identity checks, and then migrate AutoCrystal with client-thread revalidation before considering AnchorAura.
## Immutable target damage snapshot

`ExplosionDamageSnapshot` is now the canonical data-only input and calculation object for one target/explosion pair. It replaces the earlier idea of separate profile/builder/calculator types and keeps the capture contract in one place.

- `capture(...)` is a client-thread-only adapter. It verifies the active client level and target world, then captures target entity id/UUID, world generation, game tick, dimension, the exact target AABB and explosion position/power, normalized distance, world difficulty, creative self-damage immunity, health plus absorption, totem-in-hand state, armor/toughness, resistance state/amplifier, Assume Best Armor, armor-enchantment protection score, and blocking-component rules/angle.
- Ignored block positions are packed into immutable query data during capture. The returned record holds primitives, UUID/string metadata and immutable reduction records—not a live entity, inventory stack, effect instance, registry holder or world object.
- `calculate(raySnapshot)` checks generation, tick, dimension and collision-entity id/UUID before doing work. It uses the tri-state ray snapshot for exposure and returns an incomplete result for stale identity or missing ray coverage. A worker must discard incomplete results.
- Completed results include damage, exposure, sample count, lethal classification against captured health-plus-absorption, and whether the target held a totem. This provides the data needed for the next target-policy stage without resolving live entities on the worker.
- Tests cover deterministic damage, armor/resistance/protection ordering, blocking reductions, totem/lethal output, collision entity UUID reuse, world/tick/dimension mismatch, and incomplete ray coverage.

**Not yet integrated into AutoCrystal or AnchorAura.** `CrystalTargetDamage` and `CrystalDamagePolicy` still read live target and module state. In particular, face-place health/armor thresholds, lowest armor durability, active shield, target portal block state, minimum place/break damage settings, mining policy, friend policy, target range, naked/armor checks and crystal-holding checks must be captured separately. Placement validation also depends on collision entities, support directions, block states and movement prediction. Switching only the damage call would leave the rest of each worker unsafe.