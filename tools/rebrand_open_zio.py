#!/usr/bin/env python3
"""Import a pinned Open Zio tree into an existing Schizo checkout."""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import stat
import subprocess
import tempfile
import zipfile
from pathlib import Path, PurePosixPath, PureWindowsPath

EXPECTED_COMMIT = "2fec97aa0c52860f9252b61543dce8fc9825e081"
VERSION_PLACEHOLDER = "$" + "{version}"
PRESERVE_TOP_LEVEL = {".git", ".github", "tools"}
README = """# Schizo Client

Schizo is a client-only Fabric client for Minecraft 1.21.11, rebuilt on the Open Zio source architecture. This replaces the former minimal Schizo scaffold with the upstream module registry, event bus, managers, configuration and preset systems, ClickGUI/HUD editor, rendering pipeline, mixins, access widener, integrations and module implementations.

## Upstream and attribution

This is an independent rebrand and derivative of Open Zio. See UPSTREAM.md for the pinned snapshot. Original upstream source headers are retained. Third-party notices and known license gaps are recorded in THIRD_PARTY_NOTICES.md.

Schizo is not affiliated with or endorsed by Mio.

## Build

Use JDK 25 to run Gradle and have JDK 21 installed for the Java toolchain.

    ./gradlew --no-daemon clean build
    ./gradlew runClient

The remapped client JAR is written to build/libs/.

## Original modules

The original upstream module sources are included in the active Schizo source tree.
"""
UPSTREAM = f"""# Upstream source and migration notes

- Project: https://git.spectrum.best/SpectrumDevs/open-zio
- Pinned source snapshot: {EXPECTED_COMMIT}, matching the commit comment on the supplied archive.
- Project description: an optimized recreation of Mio 2.4.4 for Minecraft 1.21.11.
- The supplied archive contains 1,199 Java files; 1,179 carry the upstream MIT-license header. The supplied archive has no separate root LICENSE file. Original headers are retained.
- Rebrand: dev.zio becomes dev.schizo; mod ID, resource namespace, artifact and Gradle root name become schizo; ZioClient becomes SchizoClient.
- The previous hand-built Schizo scaffold is replaced by the upstream application architecture.
- GrimDisablerModule.java and RaytraceBypassModule.java are retained in the active source tree with their original source notices.
- The upstream project URL remains intact in retained source headers.

This file supplements rather than replaces upstream notices.
"""
THIRD_PARTY = """# Third-party notices

## DiscordIPC

The source files under src/main/java/com/jagrosh/discordipc/ are vendored from DiscordIPC by John Grosh (jagrosh).
Upstream: https://github.com/jagrosh/DiscordIPC

The supplied Open Zio archive contains no separate DiscordIPC license file. Its exact upstream license text must be verified and included before redistribution; no license terms are invented here.

## Gradle Wrapper

The wrapper scripts and JAR retain their original Gradle Apache License 2.0 notices.
"""


def safe_relative_member(name: str) -> str:
    if not name or "\\" in name or "\x00" in name or name.startswith("/"):
        raise ValueError(f"Unsafe archive path: {name!r}")
    if PureWindowsPath(name).is_absolute() or PureWindowsPath(name).drive:
        raise ValueError(f"Absolute or drive-qualified archive path: {name!r}")
    parts = name.split("/")
    if any(part in ("", ".", "..") for part in parts):
        raise ValueError(f"Traversal or empty component in archive path: {name!r}")
    if ":" in parts[0] or any(ord(character) < 32 for character in name):
        raise ValueError(f"Invalid archive path: {name!r}")
    return PurePosixPath(*parts).as_posix()


def validate_destination(destination: Path) -> Path:
    dest = destination.resolve()
    if destination.is_symlink() or not dest.is_dir():
        raise ValueError(f"Destination must be a real Schizo checkout: {destination}")
    required = (".git", ".github", "tools", "build.gradle", "src")
    missing = [name for name in required if not (dest / name).exists()]
    if missing:
        raise ValueError(f"Refusing unsafe destination; missing checkout markers: {', '.join(missing)}")
    if not (dest / ".github").is_dir() or not (dest / "tools").is_dir() or not (dest / "src").is_dir():
        raise ValueError(f"Destination markers are not directories: {dest}")
    return dest


def iter_source(source: Path):
    if source.is_dir():
        if not (source / ".git").exists():
            raise SystemExit("Source directory must be a Git checkout.")
        commit = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
        if commit != EXPECTED_COMMIT:
            raise SystemExit(f"Unexpected source commit {commit}; expected {EXPECTED_COMMIT}")
        status = subprocess.check_output(
            ["git", "-C", str(source), "status", "--porcelain", "--untracked-files=all"], text=True
        )
        if status.strip():
            raise SystemExit("Source checkout must be clean before importing.")
        for path in source.rglob("*"):
            if path.is_symlink():
                raise SystemExit(f"Source checkout contains a symbolic link: {path}")
            if path.is_file():
                rel = safe_relative_member(path.relative_to(source).as_posix())
                if rel.startswith(".git/") or rel.startswith(".gitlab/") or rel == ".gitlab-ci.yml":
                    continue
                if rel.split("/", 1)[0] in PRESERVE_TOP_LEVEL:
                    continue
                yield rel, path.read_bytes()
    else:
        with zipfile.ZipFile(source) as archive:
            commit = archive.comment.decode("ascii", errors="strict").strip()
            if commit != EXPECTED_COMMIT:
                raise SystemExit(f"Unexpected archive commit {commit!r}; expected {EXPECTED_COMMIT}")
            entries = archive.infolist()
            if len(entries) < 1200:
                raise SystemExit(f"Archive looks incomplete: {len(entries)} entries")
            roots = set()
            for item in entries:
                normalized = item.filename[:-1] if item.is_dir() and item.filename.endswith("/") else item.filename
                safe_name = safe_relative_member(normalized)
                roots.add(safe_name.split("/", 1)[0])
            if len(roots) != 1:
                raise SystemExit(f"Unexpected archive roots: {sorted(roots)}")
            root = next(iter(roots))
            safe_entries = []
            seen = set()
            file_count = 0
            for item in entries:
                normalized = item.filename[:-1] if item.is_dir() and item.filename.endswith("/") else item.filename
                safe_name = safe_relative_member(normalized)
                if safe_name != root and not safe_name.startswith(root + "/"):
                    raise SystemExit(f"Unexpected archive root entry: {item.filename!r}")
                if item.is_dir():
                    continue
                file_count += 1
                rel = safe_name[len(root):].lstrip("/")
                rel = safe_relative_member(rel)
                mode = (item.external_attr >> 16) & 0xFFFF
                if stat.S_ISLNK(mode) or (mode and not stat.S_ISREG(mode)):
                    raise SystemExit(f"Unsupported archive entry type: {item.filename!r}")
                if rel.split("/", 1)[0] in PRESERVE_TOP_LEVEL:
                    continue
                if rel.casefold() in seen:
                    raise SystemExit(f"Duplicate archive path: {rel!r}")
                seen.add(rel.casefold())
                safe_entries.append((item, rel))
            if file_count < 1200:
                raise SystemExit(f"Archive looks incomplete: {file_count} files")
            for item, rel in safe_entries:
                yield rel, archive.read(item)

def mapped_path(rel: str) -> str:
    rel = safe_relative_member(rel)
    rel = rel.replace("dev/zio/", "dev/schizo/").replace("/zio/", "/schizo/")
    rel = rel.replace("ZioClient.java", "SchizoClient.java")
    rel = rel.replace("zio.accesswidener", "schizo.accesswidener")
    rel = rel.replace("zio.mixins.json", "schizo.mixins.json")
    rel = rel.replace("assets/zio/", "assets/schizo/").replace("/zio.png", "/schizo.png")
    return safe_relative_member(rel)


def mapped_text(text: str) -> str:
    header = ""
    match = re.match(r"\A/\*.*?Licensed under the MIT License\..*?\*/\s*", text, re.S)
    if match:
        header, text = match.group(0), text[match.end():]
    urls = []

    def stash(match):
        urls.append(match.group(0))
        return f"__SCHIZO_URL_{len(urls)-1}__"

    text = re.sub(r"https?://[^\s\"'<>]+", stash, text)
    for old, new in (
        ("dev.zio", "dev.schizo"), ("dev/zio", "dev/schizo"),
        ("ZioClient", "SchizoClient"), ("zioMinecraft", "schizoMinecraft"),
        ("zio.accesswidener", "schizo.accesswidener"),
        ("zio.mixins.json", "schizo.mixins.json"),
        ("assets/zio/", "assets/schizo/"),
        ('"zio"', '"schizo"'), ("'zio'", "'schizo'"),
        ('"Zio"', '"Schizo"'), ("'Zio'", "'Schizo'"),
    ):
        text = text.replace(old, new)
    text = re.sub(r"\bZio\b", "Schizo", text)
    text = re.sub(r"\bzio\b", "schizo", text)
    for index, url in enumerate(urls):
        text = text.replace(f"__SCHIZO_URL_{index}__", url)
    return header + text


def remove_path(path: Path) -> None:
    if path.is_symlink() or not path.is_dir():
        path.unlink(missing_ok=True)
    elif path.exists():
        shutil.rmtree(path)


def build_staging_tree(stage: Path, members) -> int:
    copied = 0
    collision_paths = set()
    for rel, raw in members:
        rel = safe_relative_member(rel)
        mapped = mapped_path(rel)
        collision_key = mapped.casefold()
        if collision_key in collision_paths:
            raise ValueError(f"Multiple source entries map to {mapped!r}")
        collision_paths.add(collision_key)
        target = (stage / mapped).resolve()
        if not target.is_relative_to(stage.resolve()):
            raise ValueError(f"Mapped path escapes staging directory: {rel!r}")
        target.parent.mkdir(parents=True, exist_ok=True)
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            target.write_bytes(raw)
        else:
            text = mapped_text(text)
            if rel == "src/main/java/dev/zio/client/managers/ModuleRegistry.java":
                raytrace = '      if (!this.registerModule(new dev.schizo.client.modules.exploit.RaytraceBypassModule())) throw new IllegalStateException("Registration rejected RaytraceBypassModule");'
                grim = '      if (!this.registerModule(new dev.schizo.client.modules.exploit.GrimDisablerModule())) throw new IllegalStateException("Registration rejected GrimDisablerModule");'
                if raytrace in text and grim not in text:
                    text = text.replace(raytrace, raytrace + "\n" + grim)
            target.write_text(text, encoding="utf-8", newline="")
        copied += 1

    meta_path = stage / "src/main/resources/fabric.mod.json"
    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    meta.update({
        "id": "schizo", "name": "Schizo", "version": VERSION_PLACEHOLDER,
        "authors": ["Cypphi (Open Zio upstream)", "Schizo contributors"],
        "description": "Schizo: an independent client-side Fabric fork built on the Open Zio source architecture.",
        "icon": "assets/schizo/textures/schizo.png",
        "accessWidener": "schizo.accesswidener", "mixins": ["schizo.mixins.json"],
    })
    meta["entrypoints"] = {
        key: ["dev.schizo.client.SchizoClient"] if key in ("main", "client") else value
        for key, value in meta.get("entrypoints", {}).items()
    }
    meta.setdefault("contact", {})["sources"] = "https://github.com/sk666G/mio-client"
    meta_path.write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")

    prop = stage / "gradle.properties"
    text = prop.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^version=.*$", "version=0.1.0", text)
    text = re.sub(r"(?m)^group=.*$", "group=dev.schizo", text)
    prop.write_text(text, encoding="utf-8")
    build = stage / "build.gradle"
    build.write_text(build.read_text(encoding="utf-8").replace("archivesName = 'zio'", "archivesName = 'schizo'"), encoding="utf-8")

    (stage / "README.md").write_text(README, encoding="utf-8")
    (stage / "UPSTREAM.md").write_text(UPSTREAM, encoding="utf-8")
    (stage / "THIRD_PARTY_NOTICES.md").write_text(THIRD_PARTY, encoding="utf-8")
    gitignore = stage / ".gitignore"
    current = gitignore.read_text(encoding="utf-8") if gitignore.exists() else ""
    for pattern in (".gradle/", "build/", "run/", "run-*/", ".idea/", "*.iml"):
        if pattern not in current:
            current += "\n" + pattern
    gitignore.write_text(current.strip() + "\n", encoding="utf-8")
    return copied


def install_staging_tree(dest: Path, stage: Path) -> None:
    backup = Path(tempfile.mkdtemp(prefix=".schizo-rebuild-backup-", dir=dest.parent))
    moved_original = []
    installed = []
    try:
        for child in list(dest.iterdir()):
            if child.name in PRESERVE_TOP_LEVEL:
                continue
            child.rename(backup / child.name)
            moved_original.append(child.name)
        for child in list(stage.iterdir()):
            child.rename(dest / child.name)
            installed.append(child.name)
    except Exception as failure:
        for name in reversed(installed):
            try:
                remove_path(dest / name)
            except OSError:
                pass
        rollback_errors = []
        for name in reversed(moved_original):
            try:
                (backup / name).rename(dest / name)
            except OSError as rollback_failure:
                rollback_errors.append(rollback_failure)
        if rollback_errors:
            raise RuntimeError(f"Rebuild failed and rollback was incomplete; backup preserved at {backup}") from failure
        shutil.rmtree(backup, ignore_errors=True)
        raise
    else:
        shutil.rmtree(backup)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="verified Open Zio checkout or supplied ZIP")
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()

    dest = validate_destination(args.destination)
    members = list(iter_source(args.source.resolve()))
    if not members:
        raise SystemExit("Source contains no importable files.")

    # Validate all mapped paths before creating staging data or touching the checkout.
    seen = set()
    for rel, _ in members:
        mapped = mapped_path(rel)
        target = (dest / mapped).resolve()
        if not target.is_relative_to(dest):
            raise SystemExit(f"Mapped source path escapes the destination: {rel!r}")
        if mapped.casefold() in seen:
            raise SystemExit(f"Multiple source entries map to {mapped!r}")
        seen.add(mapped.casefold())

    stage = Path(tempfile.mkdtemp(prefix=".schizo-rebuild-stage-", dir=dest.parent))
    try:
        copied = build_staging_tree(stage, members)
        install_staging_tree(dest, stage)
    finally:
        shutil.rmtree(stage, ignore_errors=True)
    print(f"Imported {copied} files from {EXPECTED_COMMIT}; original module source is retained.")


if __name__ == "__main__":
    main()
