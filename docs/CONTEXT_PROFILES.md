# Automatic Context Profiles

Context profiles apply Schizo presets when the active server, dimension or game mode changes.

The feature is **disabled by default**. Enable it with `;contextprofile enable`.

## Context selectors

A rule has four selectors plus a preset:

- **Server**: an exact server address including port, `singleplayer`, or `*` for any server.
- **Dimension**: `any`, `overworld`, `nether` or `end`.
- **Game mode**: `any`, `survival`, `creative`, `adventure` or `spectator`.
- **Category**: `Modules`, `Binds`, `Hud`, `Colors`, `Visuals`, `Macro` or `All`.
- **Preset**: the name of an existing preset in that category.

Server names are matched case-insensitively. A server selector can include its port, for example `play.example.net:25565`.

## Commands

| Command | Action |
| --- | --- |
| `;contextprofile enable` | Enable automatic context switching |
| `;contextprofile disable` | Disable it and restore the pre-profile settings |
| `;contextprofile status` | Show enabled state and the last detected context |
| `;contextprofile list` | List configured rules |
| `;contextprofile add "<server>" <dimension> <game_mode> <category> "<preset>"` | Add or replace one rule |
| `;contextprofile remove "<server>" <dimension> <game_mode> <category>` | Remove a rule |
| `;contextprofile clear` | Remove all rules |

Example setup:

```text
;contextprofile add "*" any any All "Default"
;contextprofile add "play.example.net" overworld survival Modules "Survival"
;contextprofile add "play.example.net" the_nether any Hud "Nether HUD"
;contextprofile add "creative.example.net" any creative Visuals "Creative Visuals"
;contextprofile enable
```

The wildcard rule is optional. It provides a general baseline preset that can be overlaid by more specific rules.

## Matching and precedence

Rules matching the current context are applied from least specific to most specific:

1. Wildcard server / any dimension / any game mode
2. Server-specific and/or dimension- or game-mode-specific rules
3. The most specific rule for each preset category wins

Within a category, a later rule wins when two rules have equal specificity. The `All` category is applied before category-specific presets, so a matching Modules, HUD or Visuals preset can overlay the general All preset.

The active server's ordinary AutoPreset is applied first. Context profiles then use the resulting settings as the baseline for that connection. When the dimension or game mode changes, Schizo restores that baseline before applying the rules for the new context. On a new server connection, the previous profile is restored before the ordinary AutoPreset runs, and the new baseline is captured afterwards.

Disabling context profiles restores the baseline. Disconnecting also restores it. Manual setting changes made while a context profile is active are not automatically promoted to the baseline; save them as a preset before switching contexts if they should be reusable.

The rules and enabled state are saved in `context-profiles.json`. At most 512 rules are loaded. Missing preset references are skipped with a warning rather than crashing the client.
