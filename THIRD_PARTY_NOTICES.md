# Third-party notices

## DiscordIPC core

Eighteen source files under `src/main/java/com/jagrosh/discordipc/` correspond by path and source history to the public [CDAGaming/DiscordIPC](https://github.com/CDAGaming/DiscordIPC) project, a continuation of work originally authored by John Grosh. The upstream repository declares Apache License 2.0. Its full license text is included at [LICENSES/DiscordIPC-APACHE-2.0.txt](LICENSES/DiscordIPC-APACHE-2.0.txt).

The corresponding files in this snapshot contain downstream changes carried by Open Zio and Schizo. Copyright and Apache-2.0 notices have been restored in those 18 files. The rebuild tool preserves both MIT and Apache-2.0 source headers. The checked upstream repository has no separate `NOTICE` file.

## Schizo Discord IPC transport

The Unix-domain socket frame codec under `src/main/java/dev/schizo/client/integrations/discord/` is an original Schizo implementation under the project's MIT license header. The two unverified Open Zio helper files previously present in this area have been removed; Unix socket connections now use the standard Java NIO `SocketChannel` API.

## Gradle Wrapper

The wrapper scripts and JAR retain their original Gradle Apache License 2.0 notices.
