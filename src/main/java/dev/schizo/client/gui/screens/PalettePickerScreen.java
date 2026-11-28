package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.PaletteChannels;
import dev.schizo.client.gui.PaletteHex;
import dev.schizo.client.gui.PaletteHsv;
import dev.schizo.client.gui.PaletteRecentColors;
import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Pointer rails and native keyboard-focusable step buttons edit only a local color. */
final class PalettePickerScreen extends StudioScreen {
   private static final String[] RGB = {"Red", "Green", "Blue", "Alpha"};
   private static final String[] HSV = {"Hue", "Sat", "Value", "Alpha"};
   private static final int SEGMENTS=32;
   private static final PaletteRecentColors RECENT=new PaletteRecentColors();
   private final StudioButton[][] steps=new StudioButton[4][2];
   private final int[][] samples=new int[4][SEGMENTS];
   private final int[] channelValues=new int[4];
   private final String[] channelLabels=new String[4];
   private PaletteHsv hsv;
   private boolean hsvMode, samplesDirty=true;
   private StudioButton modeButton;
   private String hexLabel;
   private final Screen parent;
   private final String label;
   private final Consumer<Color> accepted;
   private Color color;
   private int left, top, panelWidth, railLeft, railWidth;
   private int dragging = -1;
   private boolean compact;
   private StudioViewportLayouts.Picker layout;

   PalettePickerScreen(Screen parent, String label, Color initial, Consumer<Color> accepted) {
      super(Component.literal("Pick " + label));
      this.parent = parent;
      this.label = label;
      this.color = initial;
      this.hsv = PaletteHsv.from(initial);
      this.accepted = accepted;
   }

   @Override protected void init() {
      super.init();
      dragging = -1;
      modeButton=null;
      for(var row:steps) java.util.Arrays.fill(row,null);
      layout = StudioViewportLayouts.picker(width,height);
      panelWidth=layout.width(); left=layout.left(); top=layout.top(); compact=layout.compact();
      railLeft=layout.railLeft(); railWidth=layout.railWidth();
      if (compact) {
         button("Back", left, Math.max(1, height - 25), panelWidth, this::onClose);
         return;
      }
      for (int i = 0; i < 4; i++) {
         int channel = i;
         int y = rowY(i);
         steps[i][0]=button("step:"+i+":down",left+panelWidth-50,y,23,() -> step(channel,-1));
         steps[i][1]=button("step:"+i+":up",left+panelWidth-24,y,23,() -> step(channel,1));
      }
      modeButton=button("mode",left+panelWidth-52,layout.sampleY(),52,this::toggleMode)
         .help("Switch between RGB and HSV editing. The color is unchanged until you adjust a channel.");
      refreshModeLabels();
      int third=(panelWidth-8)/3;
      button("Recent",left,layout.footerY(),third,() ->
         openStudioChild(new RecentColorsScreen(this,RECENT.snapshot(),picked -> {
            color=picked;
            hsv=PaletteHsv.from(picked);
            samplesDirty=true;
         }))).help("Browse recently accepted colors. Selection changes only this draft.");
      button("Use color",left+third+4,layout.footerY(),third,() -> {
         accepted.accept(color);
         RECENT.record(color);
         onClose();
      });
      button("Cancel",left+2*(third+4),layout.footerY(),third,this::onClose);
   }

   private StudioButton button(String name, int x, int y, int w, Runnable action) {
      return trackResizeFocus("button:"+name, addRenderableWidget(new StudioButton(x,y,w,18,Component.literal(name),button -> action.run())));
   }

   private String[] channels() { return hsvMode?HSV:RGB; }
   private int maximum(int channel) { return hsvMode?PaletteHsv.maximum(channel):255; }

   private void refreshModeLabels() {
      modeButton.setMessage(Component.literal(hsvMode?"RGB":"HSV"));
      for(int i=0;i<4;i++) {
         String name=channels()[i];
         steps[i][0].setMessage(Component.literal(name.substring(0,1)+"-"));
         steps[i][1].setMessage(Component.literal(name.substring(0,1)+"+"));
         steps[i][0].help("Decrease "+name+" by one step. Shift: larger step.");
         steps[i][1].help("Increase "+name+" by one step. Shift: larger step.");
      }
      samplesDirty=true;
   }

   private void toggleMode() {
      dragging=-1;
      hsvMode=!hsvMode;
      refreshModeLabels();
   }

   private void updateChannel(int channel,int value) {
      if(hsvMode) {
         hsv=hsv.with(channel,value);
         color=hsv.color();
      } else {
         color=PaletteChannels.replace(color,channel,value);
         hsv=PaletteHsv.from(color);
      }
      samplesDirty=true;
   }

   private void refreshSamples() {
      if(!samplesDirty) return;
      samplesDirty=false;
      int packed=color.getRGB();
      hexLabel=PaletteHex.format(color);
      for(int channel=0;channel<4;channel++) {
         int value=hsvMode?hsv.get(channel):PaletteChannels.get(color,channel);
         channelValues[channel]=value;
         channelLabels[channel]=channels()[channel]+" "+value;
         for(int segment=0;segment<SEGMENTS;segment++) {
            int sample=Math.round(segment*maximum(channel)/(float)(SEGMENTS-1));
            samples[channel][segment]=hsvMode?hsv.gradientArgb(channel,sample):PaletteChannels.gradientArgb(packed,channel,sample);
         }
      }
   }

   private int rowY(int channel) { return layout.rowY(channel); }

   private void step(int channel, int direction) {
      int amount = dev.schizo.client.utils.KeyUtils.isShiftDown() ? (hsvMode && channel!=3?10:16) : 1;
      int value=hsvMode?hsv.get(channel):PaletteChannels.get(color,channel);
      updateChannel(channel,value+direction*amount);
   }

   private void updateRail(double x) {
      updateChannel(dragging,PaletteChannels.fromPointer(x,railLeft,railWidth,maximum(dragging)));
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (!compact && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
         && event.x() >= railLeft && event.x() < railLeft + railWidth) {
         for (int i = 0; i < 4; i++) {
            if (event.y() >= rowY(i) && event.y() < rowY(i) + 18) {
               dragging = i;
               updateRail(event.x());
               return true;
            }
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (dragging >= 0 && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (Double.isFinite(event.x())) updateRail(event.x());
         return true;
      }
      return super.mouseDragged(event, dx, dy);
   }

   @Override public boolean mouseReleased(MouseButtonEvent event) {
      if (dragging >= 0 && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (Double.isFinite(event.x())) updateRail(event.x());
         dragging = -1;
         return true;
      }
      return super.mouseReleased(event);
   }

   private void checker(GuiGraphics graphics, int x, int y, int w, int h) {
      for (int iy = 0; iy < h; iy += 6) {
         for (int ix = 0; ix < w; ix += 6) {
            graphics.fill(x + ix, y + iy, x + Math.min(w, ix + 6), y + Math.min(h, iy + 6),
               ((ix / 6 + iy / 6) & 1) == 0 ? 0xff777781 : 0xff303139);
         }
      }
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      if (compact) {
         graphics.drawString(font, font.plainSubstrByWidth("Reduce GUI scale to use picker", panelWidth), left, top, 0xffeeeeef, false);
      } else {
         graphics.fill(left - 4, top, left + panelWidth + 4, layout.panelBottom(), theme.panel());
         graphics.fill(left - 4, top, left + panelWidth + 4, top + 1, theme.accent());
         graphics.drawString(font, "PICK / " + label, left, top + 7, theme.accent(), false);
         refreshSamples();
         int sampleY=layout.sampleY(), sampleHeight=layout.sampleHeight(), packed=color.getRGB();
         if (color.getAlpha()<255) checker(graphics, left, sampleY, 48, sampleHeight);
         graphics.fill(left, sampleY, left + 48, sampleY + sampleHeight, packed);
         graphics.drawString(font,font.plainSubstrByWidth(hexLabel,panelWidth-112),left+56,sampleY+(sampleHeight-font.lineHeight)/2,0xffeeeeef,false);
         for (int i = 0; i < 4; i++) {
            int y = rowY(i), value = channelValues[i];
            graphics.drawString(font,channelLabels[i],left,y+5,0xffeeeeef,false);
            if (i == 3) checker(graphics, railLeft, y + 3, railWidth, 12);
            for (int segment=0;segment<SEGMENTS;segment++) {
               int start=railLeft+segment*railWidth/SEGMENTS,end=railLeft+(segment+1)*railWidth/SEGMENTS;
               int argb=samples[i][segment];
               graphics.fill(start, y + 3, end, y + 15, argb);
            }
            int marker = railLeft + Math.round(value * (railWidth - 1) / (float)maximum(i));
            graphics.fill(marker - 1, y + 1, marker + 2, y + 17, 0xff101116);
            graphics.fill(marker, y + 1, marker + 1, y + 17, 0xffeeeeef);
         }
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { dragging = -1; returnToParent(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
