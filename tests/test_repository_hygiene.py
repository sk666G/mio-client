import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class RepositoryHygieneTest(unittest.TestCase):
    def test_mixin_inventory_matches_sources_and_java_runtime(self):
        config_path = ROOT / "src/main/resources/schizo.mixins.json"
        config = json.loads(config_path.read_text(encoding="utf-8"))
        self.assertEqual("JAVA_21", config.get("compatibilityLevel"))

        entries = config.get("mixins", [])
        self.assertEqual(len(entries), len(set(entries)), "Mixin config contains duplicate entries")
        mixin_root = ROOT / "src/main/java/dev/schizo/mixin"
        missing = [name for name in entries if not (mixin_root / f"{name}.java").is_file()]
        self.assertEqual([], missing, f"Mixin sources missing for configured entries: {missing}")

    def test_no_stale_root_zio_branding_asset(self):
        self.assertFalse((ROOT / "zio.png").exists())

    def test_mod_metadata_matches_supported_runtime(self):
        metadata = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
        self.assertEqual("schizo", metadata.get("id"))
        self.assertEqual("Schizo", metadata.get("name"))
        self.assertEqual("schizo.mixins.json", metadata.get("mixins", [None])[0])
        self.assertEqual("schizo.accesswidener", metadata.get("accessWidener"))
        self.assertEqual(">=21", metadata.get("depends", {}).get("java"))
        for entrypoint in ("main", "client"):
            self.assertEqual(
                ["dev.schizo.client.SchizoClient"],
                metadata.get("entrypoints", {}).get(entrypoint),
            )

    def test_explicit_module_registrations_resolve_without_duplicates(self):
        registry_path = ROOT / "src/main/java/dev/schizo/client/managers/ModuleRegistry.java"
        registry = registry_path.read_text(encoding="utf-8")
        registrations = re.findall(
            r"registerModule\(new\s+(?:dev\.schizo\.client\.modules\.)?([A-Za-z0-9_\.]+)\(",
            registry,
        )
        self.assertTrue(registrations, "No module registrations were discovered")
        self.assertEqual(len(registrations), len(set(registrations)), "Duplicate explicit module registration")

        module_root = ROOT / "src/main/java/dev/schizo/client/modules"
        missing = []
        for registered in registrations:
            if "." in registered:
                relative = pathlib.Path(*registered.split("."))
                candidate = module_root / f"{relative}.java"
            else:
                matches = list(module_root.rglob(f"{registered}.java"))
                candidate = matches[0] if len(matches) == 1 else None
            if candidate is None or not candidate.is_file():
                missing.append(registered)
        self.assertEqual([], missing, f"Registered module source missing or ambiguous: {missing}")

    def test_explicit_command_registrations_resolve_without_duplicates(self):
        registry = (ROOT / "src/main/java/dev/schizo/client/managers/CommandRegistry.java").read_text(encoding="utf-8")
        registrations = re.findall(
            r"registerCommand\(new\s+(?:dev\.schizo\.client\.commands\.)?([A-Za-z0-9_]+)\(",
            registry,
        )
        self.assertTrue(registrations, "No command registrations were discovered")
        self.assertEqual(len(registrations), len(set(registrations)), "Duplicate explicit command registration")

        command_root = ROOT / "src/main/java/dev/schizo/client/commands"
        missing = [name for name in registrations if not (command_root / f"{name}.java").is_file()]
        self.assertEqual([], missing, f"Registered command source missing: {missing}")

    def test_validation_circumvention_modules_cannot_reappear(self):
        forbidden = (
            ROOT / "src/main/java/dev/schizo/client/modules/exploit/GrimDisablerModule.java",
            ROOT / "src/main/java/dev/schizo/client/modules/exploit/RaytraceBypassModule.java",
        )
        for path in forbidden:
            self.assertFalse(path.exists(), f"Forbidden module returned: {path.name}")

        registry = (ROOT / "src/main/java/dev/schizo/client/managers/ModuleRegistry.java").read_text(encoding="utf-8")
        rebuild = (ROOT / "tools/rebrand_open_zio.py").read_text(encoding="utf-8")
        for name in ("GrimDisablerModule", "RaytraceBypassModule"):
            self.assertNotIn(f"new dev.schizo.client.modules.exploit.{name}", registry)
            self.assertIn(name, rebuild)


if __name__ == "__main__":
    unittest.main()
