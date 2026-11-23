#!/usr/bin/env python3
"""Replace the old scaffold with the pinned Open Zio source tree, rebranded as Schizo."""
from __future__ import annotations

import argparse
import json
import shutil
import zipfile
from pathlib import Path

EXPECTED_COMMIT = "2fec97aa0c52860f9252b61543dce8fc9825e081"
REFERENCE_ONLY_PATHS = {
    "src/main/java/dev/zio/client/modules/exploit/GrimDisablerModule.java":
        "reference-source/anti-cheat-modules/GrimDisablerModule.java",
    "src/main/java/dev/zio/client/modules/exploit/RaytraceBypassModule.java":
        "reference-source/anti-cheat-modules/RaytraceBypassModule.java",
}
PRESERVE_TOP_LEVEL = {".git", ".github", "tools"}

README = """# Schizo Client

Schizo is a client-only Fabric client for Minecraft 1.21.11, rebuilt on the Open Zio source architecture. It uses Zio's module registry, event bus, configuration and preset managers, ClickGUI/HUD editor, rendering pipeline, mixins, access widener, integrations and module implementations instead of the previous minimal Schizo scaffold.

## Upstream and attribution

This project is a rebrand and derivative of **Open Zio** by the upstream contributors. The imported source snapshot is pinned in UPSTREAM.md. The upstream Java files carry MIT-license notices; those headers are preserved. The vendored DiscordIPC sources are separately covered by Apache License 2.0; see LICENSES/ and THIRD_PARTY_NOTICES.md.

Schizo is not affiliated with or endorsed by Mio. Mio is a separate project and trademark.

## Build

Use JDK 25 to run Gradle and have JDK 21 installed for the Java toolchain, as required by the upstream project.

~~~bash
./gradlew --no-daemon clean build
./gradlew runClient
~~~

The remapped client JAR is written to build/libs/.
"""

UPSTREAM = f"""# Upstream source and migration notes

- Project: https://git.spectrum.best/SpectrumDevs/open-zio
- Source snapshot commit: {EXPECTED_COMMIT}
- Upstream description: an optimized recreation of Mio 2.4.4 for Minecraft 1.21.11.
- Imported Java source files: 1,199 in the supplied archive; 1,179 carry the upstream MIT-license header.
- Rebrand: dev.zio -> dev.schizo; mod id/resource namespace/artifact/root project renamed from zio to schizo; entrypoint renamed to SchizoClient.
- The previous handcrafted Schizo scaffold is replaced by the upstream module, event, manager, UI and rendering architecture.
- The original GrimDisablerModule and RaytraceBypassModule source files are preserved byte-for-byte under reference-source/anti-cheat-modules/. They remain reference-only and are not copied into the compiled source tree or registered in the client.
- Vendored com.jagrosh.discordipc sources are from https://github.com/jagrosh/DiscordIPC and are Apache-2.0 licensed. Their license and attribution are included separately.

Upstream source headers and attribution are retained. This file records provenance; it does not replace notices in individual source files.
"""

THIRD_PARTY = """# Third-party notices

## DiscordIPC

The source tree under src/main/java/com/jagrosh/discordipc/ is from DiscordIPC by John Grosh (jagrosh). It is licensed under the Apache License, Version 2.0. A copy is included at LICENSES/Apache-2.0.txt.

Upstream: https://github.com/jagrosh/DiscordIPC
"""


def rebrand_text(text: str) -> str:
    text = text.replace("ZioClient", "SchizoClient")
    text = text.replace("dev.zio", "dev.schizo")
    text = text.replace("Zio", "Schizo")
    text = text.replace("ZIO", "SCHIZO")
    text = text.replace("zio", "schizo")
    text = text.replace(
        "https://github.com/Cypphi/open-schizo",
        "https://github.com/sk666G/mio-client",
    )
    return text


def mapped_path(relative: str) -> str:
    mapped = relative.replace("dev/zio/", "dev/schizo/")
    mapped = mapped.replace("/zio/", "/schizo/")
    mapped = mapped.replace("zio.", "schizo.")
    mapped = mapped.replace("zio_", "schizo_")
    mapped = mapped.replace("ZioClient.java", "SchizoClient.java")
    if mapped.startswith("assets/zio/"):
        mapped = "assets/schizo/" + mapped[len("assets/zio/"):]
    if mapped == "zio.png":
        mapped = "schizo.png"
    return mapped


def clean_destination(destination: Path) -> None:
    for child in destination.iterdir():
        if child.name in PRESERVE_TOP_LEVEL:
            continue
        if child.is_dir() and not child.is_symlink():
            shutil.rmtree(child)
        else:
            child.unlink()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()

    destination = args.destination.resolve()
    archive_path = args.archive.resolve()
    destination.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(archive_path) as archive:
        commit = archive.comment.decode("ascii", errors="strict").strip()
        if commit != EXPECTED_COMMIT:
            raise SystemExit(f"Unexpected upstream archive commit: {commit!r}")

        file_members = [member for member in archive.infolist() if not member.is_dir()]
        if len(file_members) < 1200:
            raise SystemExit(f"Archive looks incomplete: only {len(file_members)} files")
        roots = {member.filename.split("/", 1)[0] for member in file_members}
        if len(roots) != 1:
            raise SystemExit(f"Expected one source root, found: {sorted(roots)}")
        source_root = next(iter(roots)) + "/"
        members = [m for m in file_members if m.filename.startswith(source_root)]

        clean_destination(destination)
        copied = 0
        archived = []
        for member in members:
            relative = member.filename[len(source_root):]
            if not relative or relative.startswith(".gitlab") or relative == ".gitlab-ci.yml":
                continue
            raw = archive.read(member)
            if relative in REFERENCE_ONLY_PATHS:
                output_path = destination / REFERENCE_ONLY_PATHS[relative]
                output_path.parent.mkdir(parents=True, exist_ok=True)
                output_path.write_bytes(raw)
                copied += 1
                archived.append(relative)
                continue

            output_path = destination / mapped_path(relative)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            try:
                decoded = raw.decode("utf-8")
            except UnicodeDecodeError:
                output_path.write_bytes(raw)
                copied += 1
                continue

            transformed = rebrand_text(decoded)
            if relative == "src/main/java/dev/zio/client/managers/ModuleRegistry.java":
                transformed = "\n".join(
                    line for line in transformed.splitlines()
                    if "RaytraceBypassModule" not in line and "GrimDisablerModule" not in line
                ) + "\n"
            output_path.write_text(transformed, encoding="utf-8", newline="")
            copied += 1

    metadata_path = destination / "src/main/resources/fabric.mod.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    metadata["id"] = "schizo"
    metadata["name"] = "Schizo"
    metadata["version"] = "\${version}"
    metadata["authors"] = ["Cypphi (Open Zio upstream)", "Schizo contributors"]
    metadata["description"] = "Schizo: an independent client-side Fabric fork built on the Open Zio source architecture."
    metadata["icon"] = "assets/schizo/textures/schizo.png"
    metadata["entrypoints"] = {
        key: ["dev.schizo.client.SchizoClient"] if key in ("main", "client") else values
        for key, values in metadata.get("entrypoints", {}).items()
    }
    metadata["contact"] = dict(metadata.get("contact", {}))
    metadata["contact"]["sources"] = "https://github.com/sk666G/mio-client"
    metadata["accessWidener"] = "schizo.accesswidener"
    metadata["mixins"] = ["schizo.mixins.json"]
    metadata_path.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")

    properties = destination / "gradle.properties"
    prop_text = properties.read_text(encoding="utf-8")
    prop_text = prop_text.replace("version=2.4.4", "version=0.1.0")
    properties.write_text(prop_text, encoding="utf-8")

    settings = destination / "settings.gradle"
    setting_text = settings.read_text(encoding="utf-8").replace(
        "rootProject.name = 'Schizo'", "rootProject.name = 'schizo'"
    )
    settings.write_text(setting_text, encoding="utf-8")

    (destination / "README.md").write_text(README, encoding="utf-8")
    (destination / "UPSTREAM.md").write_text(UPSTREAM, encoding="utf-8")
    (destination / "THIRD_PARTY_NOTICES.md").write_text(THIRD_PARTY, encoding="utf-8")

    gitignore = destination / ".gitignore"
    existing = gitignore.read_text(encoding="utf-8") if gitignore.exists() else ""
    for pattern in (".gradle/", "build/", "run/", "run-*", ".idea/", "*.iml"):
        if pattern not in existing:
            existing += f"\n{pattern}"
    gitignore.write_text(existing.strip() + "\n", encoding="utf-8")

    print(f"Imported {copied} files from Open Zio commit {commit}.")
    print("Preserved reference-only modules byte-for-byte: " + ", ".join(archived))
    print("Reference-only modules are not compiled or registered.")
    print("Project identity: Schizo / dev.schizo / schizo")


if __name__ == "__main__":
    main()
