import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIXIN_CONFIG = ROOT / "src" / "main" / "resources" / "schizo.mixins.json"
FABRIC_MOD = ROOT / "src" / "main" / "resources" / "fabric.mod.json"
MIXIN_SOURCE = ROOT / "src" / "main" / "java" / "dev" / "schizo" / "mixin"


class MixinInventoryTest(unittest.TestCase):
    def test_all_declared_mixins_have_source_files(self):
        config = json.loads(MIXIN_CONFIG.read_text(encoding="utf-8"))
        missing = []
        for entry in config.get("mixins", []):
            source = MIXIN_SOURCE.joinpath(*entry.split(".")).with_suffix(".java")
            if not source.is_file():
                missing.append(entry)

        self.assertEqual([], missing, f"Missing mixin source files: {missing}")

    def test_mixin_and_fabric_java_levels_are_aligned(self):
        mixins = json.loads(MIXIN_CONFIG.read_text(encoding="utf-8"))
        fabric = json.loads(FABRIC_MOD.read_text(encoding="utf-8"))

        self.assertEqual("JAVA_21", mixins.get("compatibilityLevel"))
        self.assertEqual(">=21", fabric.get("depends", {}).get("java"))

    def test_mixin_entries_are_unique(self):
        config = json.loads(MIXIN_CONFIG.read_text(encoding="utf-8"))
        entries = config.get("mixins", [])
        duplicates = sorted({entry for entry in entries if entries.count(entry) > 1})
        self.assertEqual([], duplicates, f"Duplicate mixin entries: {duplicates}")


if __name__ == "__main__":
    unittest.main()
