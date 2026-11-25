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

    def test_preserves_apache_license_header_during_rebrand(self):
        header = """/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"""
        source = header + "package dev.zio.example;\n\nclass ZioClient {}\n"
        result = REBRAND.mapped_text(source)

        self.assertTrue(result.startswith(header))
        self.assertIn("package dev.schizo.example;", result)
        self.assertIn("class SchizoClient", result)

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

    def test_install_preserves_project_owned_build_and_license_files(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            destination = root / "destination"
            stage = root / "stage"
            destination.mkdir()
            stage.mkdir()

            (destination / "build.gradle").write_text("hardened-build", encoding="utf-8")
            (destination / "LICENSE").write_text("project-license", encoding="utf-8")
            licenses = destination / "LICENSES"
            licenses.mkdir()
            (licenses / "dependency.txt").write_text("dependency-license", encoding="utf-8")
            (destination / "replace.txt").write_text("old", encoding="utf-8")
            (stage / "replace.txt").write_text("new", encoding="utf-8")

            REBRAND.install_staging_tree(destination, stage)

            self.assertEqual("hardened-build", (destination / "build.gradle").read_text(encoding="utf-8"))
            self.assertEqual("project-license", (destination / "LICENSE").read_text(encoding="utf-8"))
            self.assertEqual(
                "dependency-license",
                (destination / "LICENSES" / "dependency.txt").read_text(encoding="utf-8"),
            )
            self.assertEqual("new", (destination / "replace.txt").read_text(encoding="utf-8"))

    def test_refuses_non_repository_destination(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaises(ValueError):
                REBRAND.validate_destination(Path(temporary))


if __name__ == "__main__":
    unittest.main()
