package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;
import java.util.List;

/** Representative opaque/composited color pairs; advisory, never mutates the palette. */
final class ThemeContrastReport {
   record Pair(String label,int foreground,int background,double target) {
      double ratio() { return StudioScreenTheme.contrast(foreground,background); }
      boolean meetsTarget() { return ratio()>=target; }
   }

   static List<Pair> preview(UiPalette palette,boolean light) {
      var c=ThemeSampleColors.from(palette,light);
      return List.of(
         new Pair("Primary / panel",0xffeeeeef,c.surface(),4.5),
         new Pair("Secondary / input",0xffb8b9c7,c.button(),4.5),
         new Pair("Button label",0xffeeeeef,c.button(),4.5),
         new Pair("Enabled row",0xffeeeeef,c.enabled(),4.5),
         new Pair("Navigation label",0xffeeeeef,c.nav(),4.5),
         new Pair("Navigation accent",c.navAccent(),c.nav(),4.5),
         new Pair("Switch accent",c.switchAccent(),c.enabled(),3.0)
      );
   }

   static List<Pair> chrome(UiPalette palette) {
      var c=StudioScreenTheme.from(palette);
      return List.of(
         new Pair("Primary / panel",0xffeeeeef,c.panel(),4.5),
         new Pair("Secondary / panel",0xffb8b9c7,c.panel(),4.5),
         new Pair("Normal button",0xffeeeeef,c.row(),4.5),
         new Pair("Hovered button",0xffeeeeef,c.hover(),4.5),
         new Pair("Selected row",0xffeeeeef,c.selected(),4.5),
         new Pair("Disabled button",0xff9899a5,StudioScreenTheme.blend(c.panel(),c.row(),0.4),4.5),
         new Pair("Focus indicator",c.accent(),c.row(),3.0)
      );
   }
}
