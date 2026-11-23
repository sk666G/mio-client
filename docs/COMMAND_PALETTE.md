# Command Palette

Open the global command palette with **Right Shift + K** while no other screen is open.

The palette searches Schizo modules, registered commands and individual module settings. Search is case-insensitive and supports exact, prefix, partial and fuzzy subsequence matches.

## Keyboard controls

| Key | Action |
| --- | --- |
| Up / Down | Move through results |
| Enter | Run the selected action |
| Escape | Close the palette |
| Mouse wheel | Move the current selection |
| Left click | Select and run a visible result |

## Result actions

- **Module**: toggles the selected module and closes the palette
- **Command**: opens chat with the command prefix and command name filled in, so arguments can be entered before execution
- **Setting**: opens the ClickGUI, searches for the owning module and expands its settings

The palette limits results to the best 40 matches and displays up to eight at a time. It uses the active UI accent and surface colors, so the Schizo palette and Custom palette both apply.

The shortcut is currently fixed to Right Shift + K. It only opens while the game has no screen open; it does not replace or steal keyboard input from chat, menus or other screens.
