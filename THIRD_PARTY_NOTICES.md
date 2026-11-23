# Third-party notices

## DiscordIPC

The vendored Java package under src/main/java/com/jagrosh/discordipc/ is attributed to John A. Grosh and the jagrosh/DiscordIPC project:

- Upstream repository: https://github.com/jagrosh/DiscordIPC
- Upstream license: Apache License 2.0
- Full license text: THIRD_PARTY_LICENSES/DiscordIPC-Apache-2.0.txt
- Current upstream reference inspected: commit `a8d6631cc90b25f1ede2178b99ad19d016f002a0` (2023-05-19).

The vendored tree is not byte-identical to that upstream snapshot: all 12 shared source paths differ by Git blob hash, with 8 additional files in Schizo. The exact imported revision and origin of the local modifications could not be established. The package is treated as a modified derivative of DiscordIPC; this notice includes the upstream Apache-2.0 terms and does not claim that the Schizo sources match the referenced commit. Establish the import provenance and review changes before release.

No new third-party dependencies were introduced in this hardening branch.

## Gradle Wrapper

The wrapper scripts and JAR retain their original Gradle Apache License 2.0 notices.
