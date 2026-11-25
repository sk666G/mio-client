# Licensing and attribution

Schizo contains code from more than one source lineage. This file is an index; it does not replace or broaden the license grant carried by an individual source file or third-party component.

## Schizo and Open Zio source

Java and project files derived from the Open Zio source tree retain their source-level license headers. The imported upstream snapshot did not contain a separate root license file, so this repository does not invent a copyright owner or retroactively replace those retained notices.

Original Schizo files added in this repository use the MIT license header shown in those files.

See [UPSTREAM.md](UPSTREAM.md) for the pinned Open Zio source snapshot and migration notes.

## Vendored third-party code

The DiscordIPC source under `src/main/java/com/jagrosh/discordipc/` is covered by Apache License 2.0. The full license text is stored at `LICENSES/DiscordIPC-APACHE-2.0.txt` and is packaged with Schizo artifacts.

Other dependency roles and distribution expectations are recorded in [docs/DEPENDENCY_REVIEW.md](docs/DEPENDENCY_REVIEW.md).

## Redistribution review

Before publishing a release, review the retained per-file headers, [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), this index, and the dependency/distribution review together. Do not infer a single repository-wide license for a file whose retained notice says otherwise.
