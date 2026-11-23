#!/usr/bin/env python3
"""Import a pinned Open Zio tree into an existing Schizo checkout."""
from __future__ import annotations
import argparse, json, re, shutil, stat, subprocess, zipfile
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

The original upstream module sources are included in the active Schizo source tree. Their implementations and original source notices are retained during the rebrand.
"""
UPSTREAM = f"""# Upstream source and migration notes

- Project: https://git.spectrum.best/SpectrumDevs/open-zio
- Pinned source snapshot: {EXPECTED_COMMIT}, matching the commit comment on the supplied archive.
- Project description: an optimized recreation of Mio 2.4.4 for Minecraft 1.21.11.
- The supplied archive contains 1,199 Java files; 1,179 carry the upstream MIT-license header. The supplied archive has no separate root LICENSE file. Original headers are retained.
- Rebrand: dev.zio becomes dev.schizo; mod ID, resource namespace, artifact and Gradle root name become schizo; ZioClient becomes SchizoClient.
- The previous hand-built Schizo scaffold is replaced by the upstream application architecture.
- GrimDisablerModule.java and RaytraceBypassModule.java are retained in the active source tree with their original source notices.
- The upstream project URL remains intact in retained source headers and attribution.

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

def validate_relative_path(relative_path: str) -> str:
    """Validate an archive/member path before it is mapped or joined to a destination."""
    if not relative_path or "\x00" in relative_path or "\\" in relative_path:
        raise ValueError(f"Unsafe archive path: {relative_path!r}")

    posix_path = PurePosixPath(relative_path)
    windows_path = PureWindowsPath(relative_path)
    parts = relative_path.split("/")
    if (posix_path.is_absolute() or windows_path.is_absolute() or windows_path.drive
        or any(part in ("", ".", "..") for part in parts)):
        raise ValueError(f"Unsafe archive path: {relative_path!r}")
    return posix_path.as_posix()


def validate_zip_entry(entry: zipfile.ZipInfo) -> str:
    """Reject path traversal and Unix symbolic links in ZIP metadata."""
    relative_path = entry.filename.rstrip("/") if entry.is_dir() else entry.filename
    safe_path = validate_relative_path(relative_path)
    mode = (entry.external_attr >> 16) & 0xFFFF
    if stat.S_ISLNK(mode):
        raise ValueError(f"ZIP symbolic links are not allowed: {entry.filename!r}")
    return safe_path


def prepare_members(members):
    """Apply path mapping once and reject collisions before the destination is modified."""
    prepared = []
    seen_paths = set()
    for relative_path, raw in members:
        safe_source_path = validate_relative_path(relative_path)
        if safe_source_path.split("/", 1)[0] in PRESERVE_TOP_LEVEL:
            continue
        mapped = validate_relative_path(mapped_path(safe_source_path))
        collision_key = mapped.casefold()
        if collision_key in seen_paths:
            raise ValueError(f"Multiple upstream files map to the same destination: {mapped}")
        seen_paths.add(collision_key)
        prepared.append((safe_source_path, mapped, raw))
    return prepared


def validate_destination(destination: Path) -> Path:
    """Require an existing Git checkout and refuse symlinked checkout roots."""
    if destination.is_symlink():
        raise SystemExit("Destination must not be a symbolic link.")
    if not destination.exists() or not destination.is_dir():
        raise SystemExit("Destination must be an existing Git checkout directory.")
    dest = destination.resolve()
    git_metadata = dest / ".git"
    if git_metadata.is_symlink() or not git_metadata.exists():
        raise SystemExit("Destination must be a Git checkout with non-symlink .git metadata.")
    try:
        repository_root = Path(subprocess.check_output(
            ["git", "-C", str(dest), "rev-parse", "--show-toplevel"], text=True
        ).strip()).resolve()
    except subprocess.CalledProcessError as failure:
        raise SystemExit("Destination is not a valid Git checkout.") from failure
    if repository_root != dest:
        raise SystemExit("Destination must be the root of its Git checkout.")
    return dest


def iter_source(source: Path):
    if source.is_symlink():
        raise SystemExit("Source must not be a symbolic link.")
    if source.is_dir():
        if not (source / ".git").exists():
            raise SystemExit("Source directory must be a Git checkout.")
        commit = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
        if commit != EXPECTED_COMMIT:
            raise SystemExit(f"Unexpected source commit {commit}; expected {EXPECTED_COMMIT}")
        for path in source.rglob("*"):
            rel = path.relative_to(source).as_posix()
            if rel == ".git" or rel.startswith(".git/") or rel.startswith(".gitlab/") or rel == ".gitlab-ci.yml":
                continue
            if path.is_symlink():
                raise SystemExit(f"Source checkout contains a symbolic link: {rel}")
            if path.is_file():
                validate_relative_path(rel)
                yield rel, path.read_bytes()
    else:
        with zipfile.ZipFile(source) as archive:
            commit = archive.comment.decode("ascii", errors="strict").strip()
            if commit != EXPECTED_COMMIT:
                raise SystemExit(f"Unexpected archive commit {commit!r}; expected {EXPECTED_COMMIT}")
            entries = archive.infolist()
            safe_entries = [(entry, validate_zip_entry(entry)) for entry in entries]
            files = [(entry, relative_path) for entry, relative_path in safe_entries if not entry.is_dir()]
            if len(files) < 1200:
                raise SystemExit(f"Archive looks incomplete: {len(files)} files")
            roots = {relative_path.split("/", 1)[0] for _, relative_path in files}
            if len(roots) != 1:
                raise SystemExit(f"Unexpected archive roots: {sorted(roots)}")
            root = next(iter(roots))
            for entry, relative_path in files:
                parts = relative_path.split("/")
                if len(parts) < 2 or parts[0] != root:
                    raise SystemExit(f"Unexpected ZIP layout: {entry.filename!r}")
                rel = "/".join(parts[1:])
                if rel and not rel.startswith(".gitlab/") and rel != ".gitlab-ci.yml":
                    validate_relative_path(rel)
                    yield rel, archive.read(entry)

def mapped_path(rel: str) -> str:
    rel = rel.replace("dev/zio/", "dev/schizo/").replace("/zio/", "/schizo/")
    rel = rel.replace("ZioClient.java", "SchizoClient.java")
    rel = rel.replace("zio.accesswidener", "schizo.accesswidener")
    rel = rel.replace("zio.mixins.json", "schizo.mixins.json")
    rel = rel.replace("assets/zio/", "assets/schizo/").replace("/zio.png", "/schizo.png")
    return rel

def mapped_text(text: str) -> str:
    header = ""
    m = re.match(r"\A/\*.*?Licensed under the MIT License\..*?\*/\s*", text, re.S)
    if m:
        header, text = m.group(0), text[m.end():]
    urls = []
    def stash(m):
        urls.append(m.group(0))
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
    for i, url in enumerate(urls):
        text = text.replace(f"__SCHIZO_URL_{i}__", url)
    return header + text

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source", type=Path, help="verified Open Zio checkout or supplied ZIP")
    ap.add_argument("destination", type=Path, help="existing Schizo Git checkout root")
    ap.add_argument(
        "--replace-existing", action="store_true",
        help="confirm that existing destination files (except .git, .github and tools) may be removed",
    )
    args = ap.parse_args()
    if not args.replace_existing:
        ap.error("refusing destructive import without --replace-existing")
    dest = validate_destination(args.destination)
    source = args.source
    if source.is_symlink():
        raise SystemExit("Source must not be a symbolic link.")
    source = source.resolve()
    if source == dest or source in dest.parents or dest in source.parents:
        raise SystemExit("Source and destination must be separate directory trees.")

    # Read and validate every input and mapped path before cleaning a single destination file.
    try:
        members = prepare_members(iter_source(source))
    except (ValueError, OSError, zipfile.BadZipFile) as failure:
        raise SystemExit(f"Refusing unsafe or invalid upstream source: {failure}") from failure

    for child in list(dest.iterdir()):
        if child.name in PRESERVE_TOP_LEVEL:
            continue
        shutil.rmtree(child) if child.is_dir() and not child.is_symlink() else child.unlink()

    copied = 0
    for rel, mapped, raw in members:
        target = dest.joinpath(*PurePosixPath(mapped).parts)
        current = dest
        for part in PurePosixPath(mapped).parts[:-1]:
            current = current / part
            if current.is_symlink():
                raise SystemExit(f"Refusing to write through a symbolic-link directory: {current}")
            if current.exists() and not current.is_dir():
                raise SystemExit(f"Refusing to write beneath a non-directory path: {current}")
        if target.is_symlink():
            raise SystemExit(f"Refusing to replace a symbolic link: {target}")
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

    meta_path = dest / "src/main/resources/fabric.mod.json"
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

    prop = dest / "gradle.properties"
    text = prop.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^version=.*$", "version=0.1.0", text)
    text = re.sub(r"(?m)^group=.*$", "group=dev.schizo", text)
    prop.write_text(text, encoding="utf-8")
    build = dest / "build.gradle"
    build.write_text(build.read_text(encoding="utf-8").replace("archivesName = 'zio'", "archivesName = 'schizo'"), encoding="utf-8")

    (dest / "README.md").write_text(README, encoding="utf-8")
    (dest / "UPSTREAM.md").write_text(UPSTREAM, encoding="utf-8")
    (dest / "THIRD_PARTY_NOTICES.md").write_text(THIRD_PARTY, encoding="utf-8")
    gitignore = dest / ".gitignore"
    current = gitignore.read_text(encoding="utf-8") if gitignore.exists() else ""
    for pattern in (".gradle/", "build/", "run/", "run-*/", ".idea/", "*.iml"):
        if pattern not in current:
            current += "\n" + pattern
    gitignore.write_text(current.strip() + "\n", encoding="utf-8")
    print(f"Imported {copied} files from {EXPECTED_COMMIT}; original module source is retained.")

if __name__ == "__main__":
    main()
