# Schizo Studio 2.0 — integrated candidate

## Workspace

The centered, viewport-aware ClickGUI shows one real category panel at a time. Search matches module names, aliases, descriptions and setting names. Ctrl+F focuses search; Enter reveals a matched module; Escape clears the query before closing the workspace. Left click toggles a module, right click opens existing settings, and middle click opens the inspector. The bottom dock opens HUD, Performance, Profiles, Health and Themes.

## Implemented surfaces

- Crimson/graphite default, compact switches, slider rails, dropdown highlights and reduced-motion support. The complete legacy named Schizo palette migrates to crimson; customized colors are preserved and mismatched named palettes are labeled Custom.
- Masonic loading emblem with bounded real resource-loading progress and eased scale. No artificial delay.
- HUD editor with existing layout history and read-only snap-guide rendering.
- Performance Benchmark Lab: opt-in 30-second / 120,000-frame capture; two saved comparison slots; frame-time summary and graph; JVM heap and cumulative GC counters.
- Context profile editor: preview representative context, rule order, potential overlap and differences from live settings before saving; stale-rule checks and deletion confirmation.
- Health Center: local counters, integration status and sanitized aggregate report; shared input groups are review signals, not confirmed behavioral conflicts.
- Inspector: module metadata, compatibility assessment and session-local 64-entry undo/redo for eligible boolean/numeric controls. Other settings remain in the original controls.
- Theme Studio: detached interactive palette sample, explicit apply, stale-preview rejection and cancel without mutating live colors.

## Verification and limitations

A combined candidate requires fresh exact-head builds, client launch smoke, gametest screenshots and interaction checks, restart smoke, Windows storage tests, and rebuild checks where applicable. Automated screenshot existence and dimensions do not prove visual quality. Compare performance using matching workloads and runtime settings; no measured FPS improvement is claimed.

Manual acceptance: open at multiple window sizes and GUI scales, check keyboard/mouse/scroll navigation and text clipping, inspect every secondary screen, verify palette migration and custom preservation, exercise HUD drag/resize/undo, capture and compare benchmarks, review and restore context profiles, verify restart persistence, and inspect the actual loading emblem and reduced-motion behavior. Global undo/redo, full-client live theme restyling, behavioral conflict analysis and consistent secondary-screen transitions remain follow-up work.

Do not merge to main or publish a replacement preview solely on automated test success.
