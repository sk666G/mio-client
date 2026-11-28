# Project Singularity — infrastructure checkpoint I

## Evidence and scope

- Verified baseline: `5a380ba17945c2876b36aef2417594880e4dfd30`.
- [Build #3571](https://github.com/sk666G/mio-client/actions/runs/46155886818) passed all listed steps, including rebuild security tests, Gradle build and remapped artifact verification/provenance/upload.
- [Baseline artifact](https://github.com/sk666G/mio-client/actions/runs/46155886818/artifacts/15267841614): `schizo-client`, ID `15267841614`, archive SHA-256 `7c0740d968dfe1cecbc42dbd156a2ecd286269b85a5de368bd5c3ca7883c58ed`. This is not the current source checkpoint or individual JAR digest.
- Source checkpoint: `a21cc91c8073d15bebb062da7d1ae87fe40c1625`, 21 commits, 17 changed files, +375/-58 relative to the baseline. [Build #3604](https://github.com/sk666G/mio-client/actions/runs/46160304970) was pending before documentation updates. Verify the eventual PR head separately.
- Java 21 available for standalone tests. No local checkout or Gradle executable; Git DNS lookup failed. GitHub file/PR/workflow tools were available. No Minecraft runtime or exhaustive directory tree established.
- Scope: configuration integrity, startup ownership, GUI clipping, font draw cleanup, frame timing and benchmark accuracy. No gameplay or network behavior changes.

## Implemented

| Area | Defect / change | Verification |
|---|---|---|
| Preset undo | Post-operation guards compare reviewed disk + registry data for save/duplicate/rename, refuse occupied delete targets, compare live state for load. Failed undo stays retryable. Delete restoration uses create-new. Undo load applies through existing JSON rollback. | New JUnit tests; pending CI |
| Reload | Clear undo history before replacing preset registries; reject recursive undo and remove the exact completed history entry instead of a newly pushed entry. | New regression coverage; pending CI |
| Startup | Explicit manager/client phases reject reentry, concurrent initialization and retry after partial failure; preserve the original failure; no lock held during initialization callbacks. Successful repeated initialization remains a no-op. | Standalone 1,000 scenario checks passed; new JUnit tests pending |
| Scissor | Convert top-left GUI edges to bottom-left framebuffer coordinates, round outward, clamp to framebuffer, preserve empty clips, remove unconditional one-pixel height subtraction. | Standalone 200,000 geometry cases passed; new JUnit tests pending |
| Fonts | Queued text pose restoration in finally; clear queued draw references after draw failure; atomically consume reload requests before construction so requests made during reload are retained. | Targeted source review; CI/live testing pending |
| Benchmark | Reject duplicate, reordered and overlapping frame intervals; accepted intervals cannot overcount the observed capture interval. | 100 randomized sessions, 100,000 frame-event groups passed; new JUnit test pending |
| Frame timing | Evict expired durations before summing new ones, reset accumulator/cache when window becomes empty, refresh backward cache timestamps, reject nonfinite frame-factor scale. | New JUnit tests; pending CI |

## Independent verification

Standalone Java 21 harnesses compiled exact source copies (Git blob hashes matched repository file metadata):
- ScissorBounds: `55721529602afde7f0ff4c4c62d718ea5f43f3b5`.
- InitializationPhase: `e46fc690d5d12a01b2f22e4435d1f65d3b4b0cb9`.
- BenchmarkCapture: `1382042dfc3b5c8582f593eb9b1f22b87cc67ea1`.
- BenchmarkSession: `5b30941d8403e1b73e1e700dba5f5948c988b627`.

14 new JUnit methods added in this pass. Full integrated tests have not yet been confirmed on this source checkpoint. The independent checks do not establish actual visual behavior, GPU state correctness, startup integration or measured runtime performance.

## Manual acceptance additions

1. Create, duplicate, rename, overwrite and delete presets; undo each. Change the resulting file externally before undo and confirm rejection. Restore matching content and retry. Recreate a deleted name with different case and verify undo refuses replacement. Reload configuration and confirm obsolete undo entries cannot restore old registry data.
2. Load a preset, change a setting afterward, and confirm undo does not overwrite newer live configuration. Use backed-up test configuration.
3. Launch and close the exact verified candidate; verify normal two-phase initialization, settings and resource load, and shutdown. A failed startup should retain its original error, not retry partial construction in the same process.
4. Test nested scroll lists and string editor clipping at integer and fractional GUI scales, tiny/empty bounds and viewport edges. Inspect one-pixel rows and clipping in actual screenshots.
5. Exercise custom fonts, font-setting changes, chat rendering and reload. Check no duplicated queued draws or invalid pose state after errors.
6. Run Benchmark Lab and compare recorded interval counts and summaries against a known workload. Use the same world, settings and GUI scale for any before/after performance comparison.
7. Continue the existing manual checklist in [GUI terminal acceptance](gui-terminal-acceptance.md).

## Limitations and next priorities

- Preset guards are stale-state checks, not atomic filesystem compare-and-swap against external processes. They protect normal recorded undo, not arbitrary direct low-level registry calls; no full registry concurrency contract is established. Load guards retain an extra bounded snapshot. Stale history may block older undo until matching state is restored or history is cleared on reload.
- Startup phase failure is intentionally sticky and requires a restart; it does not attempt rollback of arbitrary partially constructed services.
- Scissor conversion deliberately rounds outward to cover fractional pixels; live visual correctness remains unverified.
- Font atlas allocation/disposal ownership, worker lifecycle, full configuration save behavior, complete rendering-state audit and inherited integration require further review. No comprehensive source inventory, module/event lifecycle audit or measured FPS claim.
- GUI locking and independent layer ordering remain excluded as recorded previously.
- Main untouched; PR #301 remains draft. Do not certify release readiness until exact final-head CI and appropriate live acceptance/integration review are complete.
