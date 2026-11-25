import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src" / "main" / "java"

FIELD_LOOKUP = re.compile(
    r"\bstatic\b[^;=\n]*=\s*(?:\([^)]*\)\s*)?ClientContext\.getModule\("
)
STATIC_BLOCK = re.compile(r"\bstatic\s*\{")


class StaticModuleInitializationTest(unittest.TestCase):
    def test_module_lookups_are_not_cached_during_class_initialization(self):
        violations = []

        for path in JAVA_ROOT.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            for line_number, line in enumerate(text.splitlines(), 1):
                code = line.split("//", 1)[0]
                if FIELD_LOOKUP.search(code):
                    violations.append(
                        f"{path.relative_to(ROOT)}:{line_number}: eager static field module lookup"
                    )

            lines = text.splitlines()
            index = 0
            while index < len(lines):
                code = lines[index].split("//", 1)[0]
                if not STATIC_BLOCK.search(code):
                    index += 1
                    continue

                depth = code.count("{") - code.count("}")
                block_start = index + 1
                block = [code]
                index += 1
                while index < len(lines) and depth > 0:
                    code = lines[index].split("//", 1)[0]
                    block.append(code)
                    depth += code.count("{") - code.count("}")
                    index += 1

                if "ClientContext.getModule(" in "\n".join(block):
                    violations.append(
                        f"{path.relative_to(ROOT)}:{block_start}: module lookup in static initializer"
                    )

        self.assertEqual([], violations, "\n" + "\n".join(violations))


if __name__ == "__main__":
    unittest.main()
