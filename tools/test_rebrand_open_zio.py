#!/usr/bin/env python3
"""Regression tests for the destructive Open Zio import helper."""

import io
import stat
import sys
import tempfile
import unittest
import subprocess
import zipfile
from pathlib import Path
from unittest.mock import patch
from contextlib import redirect_stderr

from rebrand_open_zio import (
    main,
    prepare_members,
    validate_destination,
    validate_relative_path,
    validate_zip_entry,
)


class ArchivePathValidationTests(unittest.TestCase):
    def test_accepts_normal_relative_posix_paths(self):
        self.assertEqual("src/main/java/Client.java",
                         validate_relative_path("src/main/java/Client.java"))

    def test_rejects_absolute_traversal_and_platform_specific_paths(self):
        invalid_paths = (
            "",
            ".",
            "..",
            "../outside.txt",
            "root/../../outside.txt",
            "/tmp/outside.txt",
            "C:/outside.txt",
            "C:\\outside.txt",
            "root\\..\\outside.txt",
            "root//file.txt",
            "root/./file.txt",
            "root/file\x00.txt",
        )
        for path in invalid_paths:
            with self.subTest(path=path):
                with self.assertRaises(ValueError):
                    validate_relative_path(path)

    def test_rejects_zip_symbolic_link_entries(self):
        entry = zipfile.ZipInfo("root/link")
        entry.create_system = 3
        entry.external_attr = (stat.S_IFLNK | 0o777) << 16
        with self.assertRaisesRegex(ValueError, "symbolic links"):
            validate_zip_entry(entry)

    def test_rejects_collisions_after_rebrand_path_mapping(self):
        members = [
            ("dev/zio/client/Example.java", b"upstream"),
            ("dev/schizo/client/Example.java", b"collision"),
        ]
        with self.assertRaisesRegex(ValueError, "same destination"):
            prepare_members(members)

    def test_rejects_file_directory_prefix_collisions(self):
        members = [
            ("config", b"file"),
            ("config/settings.json", b"child"),
        ]
        with self.assertRaisesRegex(ValueError, "conflicts with a required directory"):
            prepare_members(members)

    def test_ignores_preserved_checkout_directories(self):
        members = [
            ("tools/keep.py", b"local tool"),
            (".github/workflows/local.yml", b"local workflow"),
            ("src/main/Example.java", b"source"),
        ]
        prepared = prepare_members(members)
        self.assertEqual(["src/main/Example.java"], [item[0] for item in prepared])


class DestinationSafetyTests(unittest.TestCase):
    def test_requires_an_existing_git_checkout(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary) / "not-a-repository"
            destination.mkdir()
            with self.assertRaises(SystemExit):
                validate_destination(destination)

    def test_rejects_a_dirty_git_checkout(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary) / "checkout"
            destination.mkdir()
            subprocess.run(["git", "init", str(destination)], check=True, capture_output=True)
            (destination / "local-change.txt").write_text("keep me", encoding="utf-8")

            with self.assertRaisesRegex(SystemExit, "must be clean"):
                validate_destination(destination)

    def test_destructive_import_requires_explicit_confirmation_before_cleanup(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary) / "checkout"
            destination.mkdir()
            preserved = destination / "keep.txt"
            preserved.write_text("must survive", encoding="utf-8")

            stderr = io.StringIO()
            with patch.object(sys, "argv", ["rebrand_open_zio.py", "source.zip", str(destination)]):
                with redirect_stderr(stderr):
                    with self.assertRaises(SystemExit):
                        main()

            self.assertTrue(preserved.exists())
            self.assertEqual("must survive", preserved.read_text(encoding="utf-8"))
            self.assertIn("--replace-existing", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
