#!/usr/bin/env python3
"""Check the remapped Schizo JAR's metadata, mixin bridge methods, and exclusions."""
from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

BINDINGS = (
    ("BlockModuleBindings", "BlockMixin", "schizo$bindBlockModules"),
    ("BundleContentsModuleBindings", "BundleContentsMixin", "schizo$bindBundleContentsModules"),
    ("EntityModuleBindings", "EntityMixin", "schizo$bindEntityModules"),
    ("HoneyBlockModuleBindings", "HoneyBlockMixin", "schizo$bindHoneyBlockModules"),
    ("LevelModuleBindings", "LevelMixin", "schizo$bindLevelModules"),
    ("LiquidBlockModuleBindings", "LiquidBlockMixin", "schizo$bindLiquidBlockModules"),
    ("LivingEntityModuleBindings", "LivingEntityMixin", "schizo$bindLivingEntityModules"),
    ("MaterialModuleBindings", "MaterialMixin", "schizo$bindMaterialModules"),
    ("MinecraftModuleBindings", "MinecraftMixin", "schizo$bindMinecraftModules"),
    ("MobModuleBindings", "MobMixin", "schizo$bindMobModules"),
    ("PlayerModuleBindings", "PlayerMixin", "schizo$bindPlayerModules"),
    ("RenderTypeColorModuleBindings", "RenderTypeColorMixin", "schizo$bindRenderTypeColorModules"),
    ("SlimeBlockModuleBindings", "SlimeBlockMixin", "schizo$bindSlimeBlockModules"),
    ("SweetBerryBushBlockModuleBindings", "SweetBerryBushBlockMixin", "schizo$bindSweetBerryBushBlockModules"),
    ("TextureTransformModuleBindings", "TextureTransformMixin", "schizo$bindTextureTransformModules"),
    ("TridentItemModuleBindings", "TridentItemMixin", "schizo$bindTridentItemModules"),
    ("WebBlockModuleBindings", "WebBlockMixin", "schizo$bindWebBlockModules"),
)


def verify_jar(path: Path) -> list[str]:
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        for resource in ("fabric.mod.json", "schizo.mixins.json", "schizo.accesswidener"):
            if resource not in names:
                raise ValueError(f"Missing required resource: {resource}")

        metadata = json.loads(archive.read("fabric.mod.json"))
        if metadata.get("id") != "schizo" or metadata.get("accessWidener") != "schizo.accesswidener":
            raise ValueError("Fabric metadata does not identify Schizo and its access widener")
        mixin_configs = metadata.get("mixins", [])
        if "schizo.mixins.json" not in mixin_configs:
            raise ValueError("Fabric metadata does not register schizo.mixins.json")
        mixin_config = json.loads(archive.read("schizo.mixins.json"))
        configured = set(mixin_config.get("mixins", []))
        for binding, target, method in BINDINGS:
            if binding not in configured or target not in configured:
                raise ValueError(f"Mixin config omits binding {binding} or target {target}")
            binding_path = f"dev/schizo/mixin/{binding}.class"
            target_path = f"dev/schizo/mixin/{target}.class"
            if binding_path not in names or target_path not in names:
                raise ValueError(f"Remapped JAR omits {binding_path} or {target_path}")
            marker = method.encode("ascii")
            if marker not in archive.read(binding_path) or marker not in archive.read(target_path):
                raise ValueError(f"Mixin bridge method {method} is missing from its compiled classes")

        forbidden = ("GrimDisablerModule", "RaytraceBypassModule")
        for entry in names:
            if any(module in entry for module in forbidden):
                raise ValueError(f"Removed module reappeared in artifact: {entry}")

    return [
        "Fabric metadata, mixin config, and access widener present",
        f"All {len(BINDINGS)} @Unique binding helpers and invokers are packaged",
        "GrimDisabler and RaytraceBypass classes are absent",
    ]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    args = parser.parse_args()
    for result in verify_jar(args.jar):
        print(f"verified: {result}")


if __name__ == "__main__":
    main()
