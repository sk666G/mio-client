import importlib.util
import stat
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "rebrand_open_zio.py"
SPEC = importlib.util.spec_from_file_location("rebrand_open_zio", SCRIPT)
REBRAND = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REBRAND)


class RebuildSecurityTest(unittest.TestCase):
    def test_rejects_unsafe_archive_paths(self):
        for path in ("../escape", "/absolute", r"C:\\outside", "root/../../escape", "root\\escape"):
            with self.subTest(path=path), self.assertRaises(ValueError):
                REBRAND.safe_relative_member(path)

    def test_archive_is_fully_preflighted_before_yield(self):
        with tempfile.TemporaryDirectory() as temporary:
            archive_path = Path(temporary) / "malicious.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.comment = REBRAND.EXPECTED_COMMIT.encode("ascii")
                for index in range(1199):
                    archive.writestr(f"upstream/file-{index}.txt", "ok")
                archive.writestr("upstream/../../escape.txt", "bad")
            with self.assertRaises(ValueError):
                list(REBRAND.iter_source(archive_path))

    def test_rejects_windows_reserved_and_ambiguous_paths(self):
        for path in ("root/CON.txt", "root/folder./file", "root/trailing /file", "root/file:stream"):
            with self.subTest(path=path), self.assertRaises(ValueError):
                REBRAND.safe_relative_member(path)

    def test_rejects_symlink_directory_entry_before_extracting(self):
        with tempfile.TemporaryDirectory() as temporary:
            archive_path = Path(temporary) / "symlink.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.comment = REBRAND.EXPECTED_COMMIT.encode("ascii")
                for index in range(1199):
                    archive.writestr(f"upstream/file-{index}.txt", "ok")
                link = zipfile.ZipInfo("upstream/link/")
                link.external_attr = (stat.S_IFLNK | 0o777) << 16
                archive.writestr(link, "outside")
            with self.assertRaisesRegex(SystemExit, "Symbolic links"):
                list(REBRAND.iter_source(archive_path))

    def test_accepts_regular_zip_entries_with_permissions_but_no_type_bits(self):
        with tempfile.TemporaryDirectory() as temporary:
            archive_path = Path(temporary) / "permissions-only.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.comment = REBRAND.EXPECTED_COMMIT.encode("ascii")
                for index in range(1200):
                    info = zipfile.ZipInfo(f"upstream/file-{index}.txt")
                    info.external_attr = 0o600 << 16
                    archive.writestr(info, "ok")

            members = list(REBRAND.iter_source(archive_path))
            self.assertEqual(1200, len(members))

    def test_rejects_explicit_non_regular_unix_entry_types(self):
        with tempfile.TemporaryDirectory() as temporary:
            archive_path = Path(temporary) / "special-file.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.comment = REBRAND.EXPECTED_COMMIT.encode("ascii")
                for index in range(1200):
                    archive.writestr(f"upstream/file-{index}.txt", "ok")
                special = zipfile.ZipInfo("upstream/fifo")
                special.external_attr = (stat.S_IFIFO | 0o600) << 16
                archive.writestr(special, "not a regular file")

            with self.assertRaisesRegex(SystemExit, "Unsupported archive entry type"):
                list(REBRAND.iter_source(archive_path))

    def test_rebuild_excludes_removed_modules_and_keeps_their_registrations_out(self):
        with tempfile.TemporaryDirectory() as temporary:
            stage = Path(temporary)
            (stage / "src/main/resources").mkdir(parents=True)
            (stage / "src/main/resources/fabric.mod.json").write_text(
                '{"entrypoints": {}}', encoding="utf-8"
            )
            (stage / "gradle.properties").write_text("version=1\ngroup=zio\n", encoding="utf-8")
            (stage / "build.gradle").write_text("archivesName = 'zio'\n", encoding="utf-8")
            (stage / ".gitignore").write_text("", encoding="utf-8")
            members = [
                (
                    "src/main/java/dev/zio/client/modules/exploit/GrimDisablerModule.java",
                    b"package dev.zio;",
                ),
                (
                    "src/main/java/dev/zio/client/modules/exploit/RaytraceBypassModule.java",
                    b"package dev.zio;",
                ),
                (
                    "src/main/java/dev/zio/client/managers/ModuleRegistry.java",
                    b'if (!this.registerModule(new dev.zio.client.modules.exploit.GrimDisablerModule())) '
                    b'throw new IllegalStateException("Registration rejected GrimDisablerModule");\n'
                    b'if (!this.registerModule(new dev.zio.client.modules.exploit.RaytraceBypassModule())) '
                    b'throw new IllegalStateException("Registration rejected RaytraceBypassModule");\n',
                ),
            ]

            REBRAND.build_staging_tree(stage, members)

            self.assertFalse((stage / "src/main/java/dev/schizo/client/modules/exploit/GrimDisablerModule.java").exists())
            self.assertFalse((stage / "src/main/java/dev/schizo/client/modules/exploit/RaytraceBypassModule.java").exists())
            registry = (stage / "src/main/java/dev/schizo/client/managers/ModuleRegistry.java").read_text()
            self.assertNotIn("GrimDisabler", registry)
            self.assertNotIn("RaytraceBypass", registry)
            license_path = stage / "THIRD_PARTY_LICENSES/DiscordIPC-Apache-2.0.txt"
            self.assertIn("Apache License", license_path.read_text(encoding="utf-8"))

    def test_refuses_non_repository_destination(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaises(ValueError):
                REBRAND.validate_destination(Path(temporary))


if __name__ == "__main__":
    unittest.main()
