/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.gui.screens.HudEditorScreen;
import dev.schizo.client.render.HudAnchorGroup;
import dev.schizo.client.config.EnumJsonConverter;
import dev.schizo.client.config.FreeHudLayout;
import dev.schizo.client.render.HudWidgetRenderer;
import dev.schizo.client.render.HudVerticalAlignment;
import dev.schizo.client.render.HudHorizontalAlignment;
import dev.schizo.client.modules.client.HudColorMode;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.HudAnchor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.Iterator;

public abstract class HudWidgetModule extends Module {

   private static final EnumJsonConverter<HudAnchor> anchorCodec = new EnumJsonConverter<>(
      HudAnchor.class
   );
   protected static HUDModule hud = ClientContext.getModule(HUDModule.class);
   protected HudWidgetRenderer renderer;
   private final float[] dimensionBuffer = new float[2];

   public HudWidgetModule(String name, String... aliases) {
      super(name, ModuleCategory.HUD, aliases);
   }

   @Override
   public boolean isDrawn() {
      return false;
   }

   public int getAnchorIndex() {
      int index = 0;
      HudAnchorGroup anchorGroup = ClientContext.getHudEditor()
         .getAnchorGroup(
            this.renderer
               .getAnchor()
         );

      for (Iterator renderers = anchorGroup.getWidgets().iterator();
         renderers.hasNext();
         index++
      ) {
         HudWidgetRenderer candidate = (HudWidgetRenderer)renderers.next();
         if (candidate.getWidgetModule().equals(this)) {
            return index;
         }
      }

      return -1;
   }

   @Override
   public void fromJson(JsonElement json) {
      JsonObject config = json.getAsJsonObject();
      if (!config.has("hud")) {
         super.fromJson(json);
         return;
      }
      JsonObject layout = config.getAsJsonObject("hud");
      HudAnchor anchor = anchorCodec.fromJson(layout.get("anchor"));
      FreeHudLayout position = anchor == HudAnchor.NONE ? FreeHudLayout.read(layout) : null;

      super.fromJson(json);
      this.setAnchor(anchor);
      if (position != null) {
         this.renderer.setHorizontalAlignment(position.horizontal());
         this.renderer.setVerticalAlignment(position.vertical());
         this.renderer.setX(position.x(), false);
         this.renderer.setY(position.y(), false);
      }
      ClientContext.getHudEditor().requestDirectionRefresh();
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("enabled", this.isToggled());
      JsonObject settingsJson = new JsonObject();
      Iterator settings = this.getSettings().iterator();
      while (settings.hasNext()) {
         Setting<?> setting = (Setting<?>)settings.next();
         if (!setting.isGroupOnly()) {
            String configName = setting.getConfigName();
            JsonElement settingJson = ((JsonConfigurable)setting).toJson();
            settingsJson.add(configName, settingJson);
         }
      }
      root.add("settings", settingsJson);
      JsonObject hudJson = new JsonObject();
      EnumJsonConverter<HudAnchor> codec = anchorCodec;
      HudAnchor anchor = this.renderer.getAnchor();
      hudJson.add("anchor", codec.toJson(anchor));
      hudJson.addProperty("index", this.getAnchorIndex());
      if (this.renderer.getAnchor() == HudAnchor.NONE) {
         hudJson.addProperty("h", this.renderer.getHorizontalAlignment().ordinal());
         hudJson.addProperty("v", this.renderer.getVerticalAlignment().ordinal());
         hudJson.addProperty("x", this.renderer.getRelativeX());
         hudJson.addProperty("y", this.renderer.getRelativeY());
      }
      root.add("hud", hudJson);
      return root;
   }

   public float[] calculateDimensions() {
      throw new RuntimeException();
   }

   /** Returns this module's reusable calculation buffer. Renderers copy cached dimensions before rendering. */
   protected final float[] dimensions(float width, float height) {
      this.dimensionBuffer[0] = width;
      this.dimensionBuffer[1] = height;
      return this.dimensionBuffer;
   }

   public void renderContent(PoseStack poses) {
   }

   public void setAnchor(HudAnchor anchor) {
      HudEditorScreen editor = ClientContext.getHudEditor();
      editor.removeWidget(this.renderer);
      this.renderer.setAnchor(anchor);
      editor.addWidget(anchor, this.renderer);
   }

   protected void setRenderer(HudWidgetRenderer renderer) {
      this.renderer = renderer;
   }

   public Color getHudColor(float position) {
      Setting<HudColorMode> fadeSetting = hud.fadeSetting;
      HudColorMode fadeMode = fadeSetting.getValue();
      HUDModule capturedHud = hud;
      return fadeMode.getColor(capturedHud, position);
   }

   public HudWidgetRenderer getRenderer() {
      return this.renderer;
   }

   public HudAnchor getAnchor() {
      return this.renderer
         .getAnchor();
   }

   protected boolean isInHudEditor() {
      return ClientAccess.schizoMinecraft.screen instanceof HudEditorScreen;
   }

}
