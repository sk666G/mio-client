# Module Certification

Schizo treats module completion as a set of independent evidence gates. This prevents a source review or unit test from being presented as proof that the module is safe in a live client or compatible with a multiplayer server.

## Gates

Every registered module is evaluated against five gates:

1. **Source review** — the module has an explicit compatibility/source assessment rather than inheriting the default `UNVERIFIED` label.
2. **Setting-level review** — every current setting has its own review state. A reviewed parent module does not automatically certify its settings.
3. **Targeted automated evidence** — at least one focused automated test covers module-specific policy, lifecycle, timing, scanning or rendering behavior. Shared framework tests alone do not satisfy this gate.
4. **Live runtime validation** — the module has been exercised in the target Minecraft/Fabric runtime through its relevant enable/disable, world, screen and configuration lifecycle.
5. **Controlled multiplayer validation** — required for modules whose behavior is not purely client-only or visual-only. Client-only and visual-only modules use `NOT_REQUIRED` for this gate.

A module is fully certified only when every applicable gate is satisfied.

## Commands

- `;cert` or `;cert status` prints aggregate gate counts.
- `;cert pending` lists the least-certified registered modules and their next missing gate.
- `;cert module <ModuleClass>` prints the exact evidence state for one module.
- Hovering a module row in ClickGUI appends its current certification score, next missing gate and compatibility classification to the normal description tooltip.

Automated evidence is intentionally conservative. Existing focused tests are recorded as **TARGETED** evidence; they do not imply full behavioral coverage.

## Current boundary

The first certification inventory reuses Schizo's existing multiplayer source-review registry and records targeted automated evidence only where a focused module-specific test already exists. Live runtime and controlled multiplayer results are not fabricated from CI and therefore begin as `NOT_RECORDED`.

Future certification work should expand source and setting reviews, add reusable lifecycle-contract tests, and only promote runtime/multiplayer gates from actual recorded test sessions.
