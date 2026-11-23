# Schizo visual identity

Schizo's default interface palette is designed to be recognizable without relying on generic cyberpunk styling: deep charcoal surfaces, clear typography and an electric-lime accent.

## UI palette

Open the **UI** module and find **Palette**:

| Choice | Look |
| --- | --- |
| **Schizo** | Deep charcoal with electric-lime accent (the default identity) |
| **Midnight** | Dark blue-charcoal surfaces with a clear blue accent |
| **Frost** | Cool charcoal surfaces with a soft cyan accent |
| **Amethyst** | Deep plum-charcoal surfaces with a violet accent |
| **Custom** | Keeps individually edited colors without applying a preset |

Editing any individual palette color switches the selection to **Custom**. Selecting a named palette reapplies all six palette colors and updates the notification watermark/bracket accent. Selecting **Schizo** restores the signature charcoal/lime identity.

The Schizo palette uses:
- Accent: electric lime
- Primary surfaces: deep charcoal
- Secondary surfaces and buttons: slightly lighter charcoal
- Enabled/highlight surfaces: translucent lime
- Overlay tint: near-black charcoal
- Subtle gradient: muted lime

The notification watermark and brackets use the same accent by default and are updated when the Schizo palette is explicitly applied.

## Existing configurations

Users with an existing configuration that predates the Palette setting keep their stored UI colors. Their palette selection is treated as **Custom** rather than replacing saved colors on first launch.

The Palette selection and individual UI colors are stored in the normal client configuration. Saved HUD layouts do not contain theme colors.

## Other built-in palettes

**Midnight** uses blue highlights over deep navy-charcoal surfaces. **Frost** uses a restrained cyan accent over cool charcoal. **Amethyst** uses violet highlights over plum-charcoal surfaces. All three retain high-contrast text, dark surfaces and translucent enabled states; none changes module behavior or sends network requests.

The selected palette and all six individual colors are saved in the ordinary UI module configuration. Existing installations with no Palette field continue to load as Custom so their existing color choices are preserved.
