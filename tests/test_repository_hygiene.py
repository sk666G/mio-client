import json
import pathlib
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


if __name__ == "__main__":
    unittest.main()
