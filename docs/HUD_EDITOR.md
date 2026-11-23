# HUD Editor

Schizo's HUD editor lets you arrange HUD widgets using the existing anchor, snap, drag and resize controls.

## Layout history

The editor keeps up to 100 layout changes in its in-memory history for the current editor session.

| Shortcut | Action |
| --- | --- |
| `Ctrl+Z` | Undo the last layout change |
| `Ctrl+Y` | Redo the last undone layout change |
| `Ctrl+Shift+Z` | Redo the last undone layout change |

A drag-and-drop operation is recorded as one edit when the mouse is released. Resizing is recorded as one edit from the start of the resize until the mouse button is released. Arrow-key nudges and the existing left/right anchor swap shortcut are recorded as individual edits. Use an arrow key for a one-pixel nudge, or hold **Shift** while pressing an arrow key for a ten-pixel nudge. Nudging an anchored widget moves it into the free-position group so the new position is retained. Widget positions are clamped to the visible screen area during direct manipulation so a widget cannot be accidentally dragged completely off-screen.

Undo and redo restore each widget's anchor group, order within that group, normalized position, alignment and resize scale. If a drag ends without snapping to an anchor, the widget remains in the free-position group.

The history is local to the current editor session and is not a separate named layout/profile system. Toggling a module on or off, changing its arbitrary settings, and editing safe margins are not currently included in layout undo/redo. Closing the editor while a drag or resize is still in progress cancels that unfinished gesture.

## Saved HUD layouts

Save and manage named visual layouts from chat:

| Command | Action |
| --- | --- |
| `;hudlayout list` | List saved layouts |
| `;hudlayout save "Gameplay"` | Save the current widget placement and sizes |
| `;hudlayout load "Gameplay"` | Load a saved layout |
| `;hudlayout rename "Gameplay" "Minimal"` | Rename a layout |
| `;hudlayout delete "Gameplay"` | Delete a layout |

Names can contain spaces when quoted. Layout files are stored separately in the client's `schizo/hud-layouts` directory. Loading a saved layout restores widget positions, anchor groups, ordering, alignments and resizable widget scales. It does not change module enabled states, keybinds, widget settings, safe-zone margins or HUD theme settings.

Layouts use a versioned JSON format and are written atomically. Files with invalid names, unsupported schema versions, excessive size, invalid widget entries or symbolic-link paths are rejected. Widgets unknown to the current client are skipped; widgets added since a layout was saved keep their current placement. Resizable widgets keep the supported 0.5–2.0 size range. A resize gesture with zero or invalid content dimensions is ignored instead of storing a non-finite size. Widgets larger than the current viewport are pinned to the leading edge so their placement remains reachable.
