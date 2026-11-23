#!/usr/bin/env python3
"""Import a pinned Open Zio tree into an existing Schizo checkout."""
from __future__ import annotations
import argparse, json, re, shutil, subprocess, zipfile
from pathlib import Path

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

def iter_source(source: Path):
    if source.is_dir():
        if not (source / ".git").exists():
            raise SystemExit("Source directory must be a Git checkout.")
        commit = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
        if commit != EXPECTED_COMMIT:
            raise SystemExit(f"Unexpected source commit {commit}; expected {EXPECTED_COMMIT}")
        for path in source.rglob("*"):
            if path.is_file():
                rel = path.relative_to(source).as_posix()
                if rel.startswith(".git/") or rel.startswith(".gitlab/") or rel == ".gitlab-ci.yml":
                    continue
                yield rel, path.read_bytes()
    else:
        with zipfile.ZipFile(source) as archive:
            commit = archive.comment.decode("ascii", errors="strict").strip()
            if commit != EXPECTED_COMMIT:
                raise SystemExit(f"Unexpected archive commit {commit!r}; expected {EXPECTED_COMMIT}")
            files = [item for item in archive.infolist() if not item.is_dir()]
            if len(files) < 1200:
                raise SystemExit(f"Archive looks incomplete: {len(files)} files")
            roots = {item.filename.split("/", 1)[0] for item in files}
            if len(roots) != 1:
                raise SystemExit(f"Unexpected archive roots: {sorted(roots)}")
            root = next(iter(roots)) + "/"
            for item in files:
                if item.filename.startswith(root):
                    rel = item.filename[len(root):]
                    if rel and not rel.startswith(".gitlab/") and rel != ".gitlab-ci.yml":
                        yield rel, archive.read(item)

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
    ap.add_argument("destination", type=Path)
    args = ap.parse_args()
    dest = args.destination.resolve()
    dest.mkdir(parents=True, exist_ok=True)
    members = list(iter_source(args.source.resolve()))
    for child in list(dest.iterdir()):
        if child.name in PRESERVE_TOP_LEVEL:
            continue
        shutil.rmtree(child) if child.is_dir() and not child.is_symlink() else child.unlink()

    copied = 0
    for rel, raw in members:
        target = dest / mapped_path(rel)
        target.parent.mkdir(parents=True, exist_ok=True)
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            target.write_bytes(raw)
        else:
            text = mapped_text(text)
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
