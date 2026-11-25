import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class RepositoryHygieneTest(unittest.TestCase):
    def test_core_event_mixins_are_required(self):
        connection = (ROOT / "src/main/java/dev/schizo/mixin/ConnectionMixin.java").read_text(encoding="utf-8")
        minecraft = (ROOT / "src/main/java/dev/schizo/mixin/MinecraftMixin.java").read_text(encoding="utf-8")
        for method in ("channelRead0*", "sendPacket", "doSendPacket"):
            pattern = rf'@Inject\(method = "{re.escape(method)}"[^\n]*require = 1'
            self.assertRegex(connection, pattern, f"Core connection hook is soft: {method}")
        for method in ("destroy", "setScreen", "tick", "runTick"):
            pattern = rf'@Inject\(method = "{re.escape(method)}"[^\n]*require = 1'
            self.assertRegex(minecraft, pattern, f"Core Minecraft hook is soft: {method}")

    def test_mixin_inventory_matches_sources_and_java_runtime(self):
        config_path = ROOT / "src/main/resources/schizo.mixins.json"
        config = json.loads(config_path.read_text(encoding="utf-8"))
        self.assertEqual("JAVA_21", config.get("compatibilityLevel"))

        entries = config.get("mixins", [])
        self.assertEqual(len(entries), len(set(entries)), "Mixin config contains duplicate entries")
        mixin_root = ROOT / "src/main/java/dev/schizo/mixin"
        missing = [name for name in entries if not (mixin_root / f"{name}.java").is_file()]
        self.assertEqual([], missing, f"Mixin sources missing for configured entries: {missing}")

    def test_production_source_uses_structured_logging(self):
        source_root = ROOT / "src/main/java"
        print_stack = []
        raw_console = []
        for path in source_root.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            relative = str(path.relative_to(source_root))
            if ".printStackTrace(" in text:
                print_stack.append(relative)
            if "System.out." in text or "System.err." in text:
                raw_console.append(relative)
        self.assertEqual([], sorted(print_stack), f"printStackTrace remains in production source: {print_stack}")
        self.assertEqual([], sorted(raw_console), f"Raw console logging remains in production source: {raw_console}")

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

    def test_early_loaded_services_do_not_resolve_modules_in_static_initializers(self):
        source_root = ROOT / "src/main/java/dev/schizo"
        guarded_paths = list((source_root / "client/managers").glob("*.java"))
        guarded_paths += [
            source_root / "mixin/ConnectionMixin.java",
            source_root / "mixin/MinecraftMixin.java",
            source_root / "mixin/LevelRendererMixin.java",
            source_root / "mixin/ClientPacketListenerMixin.java",
            source_root / "mixin/LocalPlayerMixin.java",
            source_root / "mixin/GameRendererMixin.java",
        ]
        offenders = []
        static_field = re.compile(r"static[^;\n]*=\s*ClientContext\.getModule\(")

        def static_blocks(text):
            for match in re.finditer(r"\bstatic\s*\{", text):
                start = match.start()
                cursor = text.find("{", start)
                depth = 0
                while cursor < len(text):
                    character = text[cursor]
                    if character == "{":
                        depth += 1
                    elif character == "}":
                        depth -= 1
                        if depth == 0:
                            yield text[start:cursor + 1]
                            break
                    cursor += 1

        for path in guarded_paths:
            text = path.read_text(encoding="utf-8")
            eager_block = any("ClientContext.getModule(" in block for block in static_blocks(text))
            if eager_block or static_field.search(text):
                offenders.append(str(path.relative_to(source_root)))
        self.assertEqual([], sorted(offenders), f"Eager early-load module lookups remain: {offenders}")

    def test_registered_modules_with_eager_module_caches_have_late_binding(self):
        registry_text = (ROOT / "src/main/java/dev/schizo/client/managers/ModuleRegistry.java").read_text(encoding="utf-8")
        dependencies = (ROOT / "src/main/java/dev/schizo/client/bootstrap/ModuleDependencies.java").read_text(encoding="utf-8")
        registrations = re.findall(
            r"registerModule\(new\s+(?:dev\.schizo\.client\.modules\.)?([A-Za-z0-9_\.]+)\(",
            registry_text,
        )
        module_root = ROOT / "src/main/java/dev/schizo/client/modules"
        offenders = []
        for registered in registrations:
            class_name = registered.split(".")[-1]
            if "." in registered:
                source = module_root / pathlib.Path(*registered.split(".")).with_suffix(".java")
            else:
                matches = list(module_root.rglob(f"{class_name}.java"))
                source = matches[0] if len(matches) == 1 else None
            if source is None or not source.is_file():
                continue
            text = source.read_text(encoding="utf-8")
            static_field = re.search(r"static[^;\n]*=\s*ClientContext\.getModule\(", text)
            eager_block = False
            for match in re.finditer(r"\bstatic\s*\{", text):
                start = match.start()
                cursor = text.find("{", start)
                depth = 0
                while cursor < len(text):
                    character = text[cursor]
                    if character == "{":
                        depth += 1
                    elif character == "}":
                        depth -= 1
                        if depth == 0:
                            eager_block = "ClientContext.getModule(" in text[start:cursor + 1]
                            break
                    cursor += 1
                if eager_block:
                    break
            if static_field is None and not eager_block:
                continue
            if re.search(rf"\b{re.escape(class_name)}\.bind[A-Za-z0-9_]*\(registry\)", dependencies) is None:
                offenders.append(class_name)
        self.assertEqual(
            [],
            sorted(set(offenders)),
            f"Registered modules cache dependencies eagerly without post-registration binding: {offenders}",
        )

    def test_module_dependency_binding_precedes_client_events_and_config_load(self):
        initializer = (ROOT / "src/main/java/dev/schizo/client/bootstrap/ClientInitializer.java").read_text(encoding="utf-8")
        bind_index = initializer.index("ModuleDependencies.bind(ClientContext.moduleRegistry);")
        client_event_index = initializer.index("ClientAccess.eventBus.post(new ClientInitializationEvent());")
        config_load_index = initializer.index("configuration.load();")
        self.assertLess(bind_index, client_event_index)
        self.assertLess(bind_index, config_load_index)

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
        allowed_repeated = {"EnemyFriendCommand"}
        accidental_duplicates = sorted({
            name for name in registrations
            if registrations.count(name) > 1 and name not in allowed_repeated
        })
        self.assertEqual([], accidental_duplicates, "Duplicate explicit command registration")

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
