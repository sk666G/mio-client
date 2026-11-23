# Module Favorites and Recent Access

Schizo remembers favorite modules and the most recently enabled modules. This is separate from module configuration, so favorites do not change when you reset a module's settings or load a module preset.

## ClickGUI

Each module row has a star control at its right edge:

- **★** means the module is a favorite.
- **☆** means it is not a favorite.

Left-click the star to add or remove a favorite. It is a separate hit target from the module toggle, so clicking it does not enable or disable the module.

## Commands

| Command | Action |
| --- | --- |
| `;favorite toggle "Auto Crystal"` | Toggle a module's favorite state |
| `;favorite add "Surround"` | Add a module to favorites |
| `;favorite remove "Surround"` | Remove a module from favorites |
| `;favorite list` | List favorite modules |
| `;favorite recent` | List recently enabled modules |
| `;favorite clear` | Clear favorites |
| `;favorite clearrecent` | Clear recent history |

The command accepts the normal module name suggestions. Aliases are `;favorites` and `;fav`.

## Command Palette

Open the command palette with **Right Shift + K**. With an empty search, favorite modules appear first in favorite order, then recently enabled modules (most recent first), followed by other modules and commands. For a non-empty search, normal relevance ranking takes priority; favorites and recent usage break ties.

Selecting a module still toggles it. The preference ordering does not change module keybinds, category placement, enabled state or module settings.

## Persistence and limits

Favorites and recent usage are stored in `module-preferences.json` independently of `modules.json`.

- Up to 128 favorites are retained. Adding another when full evicts the oldest favorite.
- Up to 20 recently enabled modules are retained. Enabling a module again moves it to the front.
- Unknown module names and malformed entries are ignored when loading the file.
- The registry stores module names only. It does not record when, where or why a module was enabled.
