import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class RepositoryHygieneTest(unittest.TestCase):
    def test_mixin_java_level_matches_runtime_toolchain(self):
        config = json.loads((ROOT / "src/main/resources/schizo.mixins.json").read_text(encoding="utf-8"))
        self.assertEqual("JAVA_21", config["compatibilityLevel"])

    def test_hidden_unlock_marker_is_not_reintroduced(self):
        for path in (ROOT / "src/main/java").rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            self.assertNotIn("USER_HOME_MARKER_PATH", text, path)
            self.assertNotIn('Path.of(userHome, "_____a")', text, path)

    def test_no_direct_print_stack_trace_calls_in_client_source(self):
        offenders = []
        for path in (ROOT / "src/main/java/dev/schizo").rglob("*.java"):
            if "printStackTrace(" in path.read_text(encoding="utf-8"):
                offenders.append(str(path.relative_to(ROOT)))
        self.assertEqual([], offenders)

    def test_no_module_lookup_during_static_initialization(self):
        offenders = []
        for path in (ROOT / "src/main/java/dev/schizo").rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            field_lookup = re.search(r"static[^;\n=]*=\s*ClientContext\.getModule\s*\(", text)
            block_lookup = any(
                "ClientContext.getModule(" in block
                for block in re.findall(r"static\s*\{.{0,2500}?\n\s*\}", text, flags=re.S)
            )
            if field_lookup or block_lookup:
                offenders.append(str(path.relative_to(ROOT)))
        self.assertFalse(
            offenders,
            "Static module lookup during class initialization:\n" + "\n".join(offenders),
        )

    def test_no_empty_broad_exception_catches(self):
        offenders = []
        broad = re.compile(
            r"catch\s*\(\s*(?:Throwable|Exception|RuntimeException)\b[^)]*\)\s*\{\s*\}",
            flags=re.S,
        )
        for path in (ROOT / "src/main/java/dev/schizo").rglob("*.java"):
            if broad.search(path.read_text(encoding="utf-8")):
                offenders.append(str(path.relative_to(ROOT)))
        self.assertFalse(
            offenders,
            "Empty broad exception catches:\n" + "\n".join(offenders),
        )

    def test_root_branding_does_not_ship_legacy_zio_image(self):
        self.assertFalse((ROOT / "zio.png").exists())


if __name__ == "__main__":
    unittest.main()
