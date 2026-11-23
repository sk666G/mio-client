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

    def test_refuses_non_repository_destination(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaises(ValueError):
                REBRAND.validate_destination(Path(temporary))


if __name__ == "__main__":
    unittest.main()
