# Startup Safe Mode

Schizo's startup Safe Mode is a reversible diagnostic startup that blocks all modules from being enabled. It keeps the ClickGUI, commands and configuration diagnostics available so a broken module or keybind setup can be repaired.

## Enter Safe Mode

From a normal session, run:

```text
;safemode arm
```

Restart Schizo. The client sees the `.safe-mode` marker in its configuration directory and starts with every module disabled. No module is enabled from `modules.json`, and module state is not overwritten when the client exits in Safe Mode.

## Check or leave Safe Mode

- `;safemode status` reports whether the current session is active, whether the next launch is armed, and whether the marker is unsafe.
- `;safemode disarm` removes the marker for the next launch.
- A session that started in Safe Mode stays in Safe Mode until it exits. Disarming does not enable modules mid-session; restart to return to normal startup.

The marker is a small file under the Schizo configuration root. A symlink or other non-regular marker activates fail-closed Safe Mode rather than being followed. If the configuration root itself cannot be verified during startup, Schizo also starts with modules blocked.

Safe Mode does not delete presets, reset settings, rewrite module keybinds or clear configuration. It deliberately skips saving `modules.json` during a Safe Mode session so that the user's previous module enablement choices are preserved.
