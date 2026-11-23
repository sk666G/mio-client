# HUD Editor

Schizo's HUD editor lets you arrange HUD widgets using the existing anchor, snap, drag and resize controls.

## Layout history

The editor keeps up to 100 layout changes in its in-memory history for the current editor session.

| Shortcut | Action |
| --- | --- |
| `Ctrl+Z` | Undo the last layout change |
| `Ctrl+Y` | Redo the last undone layout change |
| `Ctrl+Shift+Z` | Redo the last undone layout change |

A drag-and-drop operation is recorded as one edit when the mouse is released. Resizing is recorded as one edit from the start of the resize until the mouse button is released. Arrow-key nudges and the existing left/right anchor swap shortcut are recorded as individual edits. Nudging an anchored widget moves it into the free-position group so the new position is retained.

Undo and redo restore each widget's anchor group, order within that group, normalized position, alignment and resize scale. If a drag ends without snapping to an anchor, the widget remains in the free-position group.

The history is local to the current editor session and is not a separate named layout/profile system. Toggling a module on or off, changing its arbitrary settings, and editing safe margins are not currently included in layout undo/redo. Closing the editor while a drag or resize is still in progress cancels that unfinished gesture.
