# Third-party notices

## DiscordIPC core

Eighteen source files under `src/main/java/com/jagrosh/discordipc/` correspond by path and source history to the public [CDAGaming/DiscordIPC](https://github.com/CDAGaming/DiscordIPC) project, a continuation of work originally authored by John Grosh. The upstream repository declares Apache License 2.0. Its full license text is included at [LICENSES/DiscordIPC-APACHE-2.0.txt](LICENSES/DiscordIPC-APACHE-2.0.txt).

The corresponding files in this snapshot contain downstream changes carried by Open Zio and Schizo. Copyright and Apache-2.0 notices have been restored in those 18 files. The rebuild tool preserves both MIT and Apache-2.0 source headers. The checked upstream repository has no separate `NOTICE` file.

## Open Zio-only DiscordIPC transport helpers

`src/main/java/com/jagrosh/discordipc/entities/pipe/IpcFrameCodec.java` and `JavaUnixSocketTransport.java` are additional files in the pinned Open Zio source snapshot and are not present in the public CDAGaming/DiscordIPC source tree checked for this review. Their authorship and licence terms have not been established from available source metadata. The Apache-2.0 license copy above is **not** asserted to cover these two files. Resolve their provenance/licensing or replace them with clearly licensed alternatives before redistribution.

## Gradle Wrapper

The wrapper scripts and JAR retain their original Gradle Apache License 2.0 notices.
