import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "verify_release_jar.py"
SPEC = importlib.util.spec_from_file_location("verify_release_jar", SCRIPT)
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)
BINDINGS = VERIFY.BINDINGS
verify_jar = VERIFY.verify_jar


class ReleaseJarVerificationTest(unittest.TestCase):
    def make_jar(self, path: Path, banned: bool = False):
        metadata = {
            "id": "schizo",
            "accessWidener": "schizo.accesswidener",
            "mixins": ["schizo.mixins.json"],
        }
        mixins = {"mixins": [name for binding, target, method in BINDINGS for name in (binding, target)]}
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("fabric.mod.json", json.dumps(metadata))
            archive.writestr("schizo.mixins.json", json.dumps(mixins))
            archive.writestr("schizo.accesswidener", "accessWidener v2 named")
            for binding, target, method in BINDINGS:
                marker = method.encode("ascii")
                archive.writestr(f"dev/schizo/mixin/{binding}.class", marker)
                archive.writestr(f"dev/schizo/mixin/{target}.class", marker)
            if banned:
                archive.writestr("dev/schizo/client/modules/GrimDisablerModule.class", b"")

    def test_accepts_complete_remapped_jar_layout(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "schizo.jar"
            self.make_jar(path)
            self.assertEqual(3, len(verify_jar(path)))

    def test_rejects_reintroduced_removed_module(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "schizo.jar"
            self.make_jar(path, banned=True)
            with self.assertRaisesRegex(ValueError, "GrimDisablerModule"):
                verify_jar(path)


if __name__ == "__main__":
    unittest.main()
