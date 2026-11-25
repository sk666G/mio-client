# Dependency and distribution review

This is an engineering inventory, not legal advice.

Its purpose is to separate dependencies that Schizo only compiles/tests/runs against from code or nested JARs that are actually redistributed in Schizo artifacts. The authoritative final distribution evidence is the `NESTED_JARS.txt` manifest generated from the runnable JAR by CI, together with direct artifact inspection.

## Review rules

- **Build/test only** dependencies are not part of the released Schizo JAR merely because Gradle resolves them.
- **Compile-only / local-runtime** dependencies are expected to remain external unless another build rule explicitly copies their files or nested libraries.
- `implementation` of files extracted from another mod can cause those extracted libraries to be redistributed and therefore requires artifact-level verification.
- A dependency's upstream license is recorded here for engineering review. This file does not interpret whether any particular distribution is legally sufficient.
- Restricted or all-rights-reserved dependencies must not be assumed redistributable.

## Direct build and runtime dependencies

| Dependency | Schizo configuration / role | Upstream license evidence | Distribution expectation |
| --- | --- | --- | --- |
| Minecraft 1.21.11 | `minecraft` development/runtime target | Mojang/Microsoft proprietary software | Not a Schizo artifact payload; supplied by the user's Minecraft installation. |
| Fabric Loader | `modImplementation` | Apache-2.0 — https://github.com/FabricMC/fabric-loader | External loader; final artifact inventory must confirm no unexpected embedded copy. |
| Fabric API | `modImplementation` | Apache-2.0 — https://github.com/FabricMC/fabric-api | Required external mod according to `fabric.mod.json`; final artifact inventory must confirm no unexpected embedded copy. |
| Fabric Loom | Gradle plugin | MIT — https://github.com/FabricMC/fabric-loom | Build-only. |
| ViaFabricPlus | `modCompileOnly`, `modLocalRuntime`; its own parent JAR is excluded from the extracted-library `implementation` set | GPL-3.0 — https://github.com/ViaVersion/ViaFabricPlus | Parent mod expected external. Some libraries nested inside the downloaded ViaFabricPlus distribution are deliberately extracted into Schizo's build; exact redistributed nested JARs must be reviewed from `NESTED_JARS.txt`. |
| Lithium | `modCompileOnly` | LGPL-3.0 — https://github.com/CaffeineMC/lithium | Optional compatibility only; expected external. |
| ImmediatelyFast | `modCompileOnly` | LGPL-3.0 — https://github.com/RaphiMC/ImmediatelyFast | Optional compatibility only; expected external. |
| Sodium | `modCompileOnly` | PolyForm Shield 1.0.0 except separately noticed third-party material — https://github.com/CaffeineMC/sodium | Optional compatibility only; expected external. Must not be treated as redistributable merely because it is a compile dependency. |
| Xaero's Minimap | `modCompileOnly` | All Rights Reserved — https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap/license | Optional compatibility only; expected external. Do not redistribute its JAR without separate permission/terms supporting that distribution. |
| EntityCulling | `modCompileOnly`, `modLocalRuntime`; nested libraries are local-runtime only | tr7zw Protective License — https://github.com/tr7zw/EntityCulling/blob/main/LICENSE-EntityCulling | Optional compatibility/local development. Upstream terms restrict commercial use and general JAR redistribution; Schizo's final artifact must not unexpectedly contain the EntityCulling parent JAR. |
| DevAuth Fabric | `modLocalRuntime` | MIT — https://github.com/DJtheRedstoner/DevAuth | Development runtime only; expected absent from release artifact. |
| Baritone | pinned distribution; `modCompileOnly` / `modLocalRuntime`; libraries from its `META-INF/jars` are added through `implementation` | LGPL-3.0 — https://github.com/cabaletta/baritone | Baritone parent mod expected external, but its extracted nested libraries may be redistributed. Exact names require `NESTED_JARS.txt` review. The downloaded Baritone JAR is SHA-256 pinned by the build. |
| webrtc-java 1.0.3 | common API `compileOnly`; native classifiers `runtimeOnly` | Apache-2.0 POM — https://central.sonatype.com/artifact/dev.kastle.webrtc/webrtc-java/1.0.3 | External runtime/native dependency; upstream documents that natives are separate artifacts. Final JAR inventory must confirm they are not unexpectedly nested. |
| JetBrains annotations 26.0.2 | `compileOnly` | Apache-2.0 — https://github.com/JetBrains/java-annotations/blob/master/LICENSE.txt | Compile-only; expected absent. |
| JUnit Jupiter 5.11.4 / Platform launcher | test-only | EPL-2.0 — https://github.com/junit-team/junit-framework | Test-only; expected absent. |

## Vendored source

### DiscordIPC

Schizo contains modified vendored source under `src/main/java/com/jagrosh/discordipc/`.

The upstream project evidence and downstream attribution are recorded in `THIRD_PARTY_NOTICES.md`. The full Apache-2.0 text is stored at `LICENSES/DiscordIPC-APACHE-2.0.txt`.

CI verifies that the project MIT license, third-party notice and DiscordIPC Apache-2.0 text are present in the runnable and source JARs.

### Schizo Discord IPC transport

The Unix-domain socket frame codec under `dev.schizo.client.integrations.discord` is Schizo-owned MIT-header code. See `THIRD_PARTY_NOTICES.md`.

## Artifact-level verification

The normal Build workflow and the fresh-upstream rebuild workflow now:

1. require exactly one runnable Schizo JAR;
2. require exactly one Schizo source JAR;
3. verify `fabric.mod.json`, the project `LICENSE`, `THIRD_PARTY_NOTICES.md`, and the DiscordIPC Apache-2.0 license in the runnable JAR;
4. verify the project license, third-party notice and DiscordIPC license in the source JAR;
5. produce `NESTED_JARS.txt` by listing `META-INF/jars/*.jar` from the runnable JAR;
6. record the runnable JAR SHA-256;
7. record the exact tested source commit.

## Final candidate evidence

Build #739 on v7 head `bdaa3bd4b07217edfaae9c94e63e81a8c72f916e` passed the artifact verification step and printed:

`Nested libraries in runtime JAR: (none)`

That means the tested runnable JAR contains no nested JAR payloads under `META-INF/jars/`. Optional compatibility mods such as EntityCulling, Xaero's Minimap and Sodium are therefore not being redistributed as nested JARs in that candidate. Vendored DiscordIPC source remains separately covered by `THIRD_PARTY_NOTICES.md` and `LICENSES/DiscordIPC-APACHE-2.0.txt`, which CI verifies in both runtime and source JARs.

## Final dependency-release gate

Before public release, review the final candidate's `NESTED_JARS.txt` and classify every listed nested JAR.

For each nested library:

- identify the source project and exact version if possible;
- record the applicable license;
- verify required notices/license texts are included when redistribution requires them;
- verify no restricted optional parent mod (especially EntityCulling, Xaero's Minimap, or Sodium) was accidentally copied into the artifact;
- keep unresolved entries as release blockers rather than assuming they are safe.

The build configuration is not sufficient evidence by itself. The generated artifact inventory is the release source of truth.
