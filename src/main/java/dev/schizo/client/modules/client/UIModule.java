/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.client;

import dev.schizo.client.utils.collections.NamedIdentifier;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.MinecraftTickEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.bus.EventBus;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.screens.ClickGuiScreen;
import dev.schizo.client.gui.screens.ClientScreen;
import dev.schizo.client.managers.TaskScheduler;
import dev.schizo.client.gui.CursorType;
import dev.schizo.client.gui.UiMotionPolicy;
import dev.schizo.client.render.text.LanguageTable;
import dev.schizo.client.utils.world.Season;
import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.SchizoClient;
import dev.schizo.client.modules.Keybind;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.ColorSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.IdentifierSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import java.awt.Color;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import org.lwjgl.glfw.GLFW;

public final class UIModule extends Module {
   public static UIModule INSTANCE;
   private final AtomicBoolean panelResetPending = new AtomicBoolean(false);
   private final Setting<Boolean> resetSetting;
   public final Setting<StyleChoice> styleSetting;
   public final Setting<Float> animSpeedSetting;
   public final Setting<Boolean> reducedMotionSetting;
   public final Setting<Float> scaleSetting;
   public final Setting<Boolean> snowSetting;
   public final Setting<Boolean> gamesSetting;
   public final Setting<Boolean> descriptionsSetting;
   final Setting<LanguageChoice> languageSetting;
   public final Setting<Boolean> settingsSetting;
   public final Setting<Boolean> elementsSetting;
   public final Setting<Boolean> aeroSetting;
   public final Setting<Boolean> elementGradientSetting;
   public final Setting<Boolean> lineSetting;
   public final Setting<Boolean> bindsSetting;
   public final Setting<Boolean> gearSetting;
   public final Setting<Boolean> soundsSetting;
   public final Setting<Boolean> hoverSetting;
   public final Setting<NamedIdentifier> hoverSoundSetting;
   public final Setting<Float> hoverVolumeSetting;
   public final Setting<Boolean> leftClickSetting;
   public final Setting<NamedIdentifier> leftClickSoundSetting;
   public final Setting<Float> leftClickVolumeSetting;
   public final Setting<Boolean> rightClickSetting;
   public final Setting<NamedIdentifier> rightClickSoundSetting;
   public final Setting<Float> rightClickVolumeSetting;
   public final Setting<Boolean> textSetting;
   public final Setting<Color> textColorSetting;
   public final Setting<Color> enabledColorSetting;
   final Setting<Boolean> colorsSetting;
   public final Setting<PaletteChoice> paletteSetting;
   public final Setting<Color> colorSetting;
   public final Setting<Color> backgroundSetting;
   public final Setting<Color> buttonSetting;
   public final Setting<Color> enabledSetting;
   public final Setting<Color> tintSetting;
   public final Setting<Color> gradientColorSetting;
   private long guiOpenGeneration;

   @Override
   public void onEnable() {
      long generation = ++this.guiOpenGeneration;
      if (this.isWorldUnavailable() || ClientAccess.schizoMinecraft.screen instanceof ClickGuiScreen) {
         return;
      }
      if (ClientAccess.schizoMinecraft.screen instanceof ChatScreen chatScreen) {
         TaskScheduler guiScheduler = ClientContext.taskScheduler;
         Runnable guiOpenTask = () -> {
            if (!this.isToggled()
               || this.guiOpenGeneration != generation
               || ClientAccess.schizoMinecraft.screen != chatScreen) {
               return;
            }
            ClickGuiScreen deferredScreen = ClientContext.getClickGui();
            deferredScreen.open();
         };
         if (!guiScheduler.scheduleRotationTask(this, guiOpenTask, 0)) {
            guiOpenTask.run();
         }
      } else {
         ClickGuiScreen screen = ClientContext.getClickGui();
         screen.open();
      }
   }

   @Override
   public Keybind getKeybind() {
      Keybind keybind = super.getKeybind();
      if (keybind.mode() != Keybind.Mode.TOGGLE) {
         keybind = keybind.withMode(Keybind.Mode.TOGGLE);
         this.setKeybind(keybind);
      }
      return keybind;
   }

   @Override
   public void onDisable() {
      this.guiOpenGeneration++;
      if (ClientAccess.schizoMinecraft.screen instanceof ClickGuiScreen) {
         ClientContext.getClickGui()
            .closeWithTransition();
      }
   }

   @EventHandler
   private void onMinecraftTick(MinecraftTickEvent event) {
      Minecraft minecraft = ClientAccess.schizoMinecraft;
      Window window = minecraft.getWindow();
      long windowHandle = window.handle();
      if (GLFW.glfwGetMouseButton(windowHandle, 0) == 1) {
         return;
      }
      AtomicBoolean pendingReset = this.panelResetPending;
      if (!pendingReset.get()) {
         return;
      }
      ClickGuiScreen screen = ClientContext.getClickGui();
      screen.resetPanelPositions();
      this.resetSetting.setValue(true);
      this.panelResetPending.set(false);
   }

   @EventHandler
   private void onLocalPlayerTick(LocalPlayerTickEvent event) {
      if (!(ClientAccess.schizoMinecraft.screen instanceof ClientScreen)) {
         this.toggle();
      }
   }

   @EventHandler
   private static void onCursorTick(MinecraftTickEvent event) {
      if (!(ClientAccess.schizoMinecraft.screen instanceof ClientScreen)) {
         CursorType cursor = CursorType.STANDARD;
         cursor.apply();
      }
   }

   @EventHandler
   private void onPacketReceive(
      PacketEvent.Receive event
   ) {
      if (event.getPacket() instanceof ClientboundDisconnectPacket) {
         ClientAccess.schizoMinecraft.executeIfPossible(() -> {
            if (this.isToggled()) {
               this.disable();
            }
         });
      }
   }

   public float getAnimationSpeed() {
      return this.animSpeedSetting.getValue();
   }

   private boolean applyingPalette;

   /** Snapshot actual colors, including custom edits, without invoking palette callbacks. */
   public UiPalette snapshotPalette() {
      return new UiPalette(this.colorSetting.getValue(), this.backgroundSetting.getValue(),
         this.buttonSetting.getValue(), this.enabledSetting.getValue(), this.tintSetting.getValue(),
         this.gradientColorSetting.getValue());
   }


   private void applySelectedPalette() {
      if (this.applyingPalette) {
         return;
      }

      UiPalette palette = this.paletteSetting.getValue().getPalette();
      if (palette == null) {
         return;
      }

      this.applyingPalette = true;
      try {
         this.colorSetting.setValue(palette.accent());
         this.backgroundSetting.setValue(palette.background());
         this.buttonSetting.setValue(palette.button());
         this.enabledSetting.setValue(palette.enabled());
         this.tintSetting.setValue(palette.tint());
         this.gradientColorSetting.setValue(palette.gradient());

         NotificationsModule notifications = ClientContext.getModule(NotificationsModule.class);
         if (notifications != null) {
            Color accent = new Color(
               palette.accent().getRed(),
               palette.accent().getGreen(),
               palette.accent().getBlue()
            );
            notifications.watermarkSetting.setValue(accent);
            notifications.bracketsSetting.setValue(accent);
         }
      } finally {
         this.applyingPalette = false;
      }
   }

   private void markPaletteCustom() {
      if (!this.applyingPalette && this.paletteSetting.getValue() != PaletteChoice.CUSTOM) {
         this.paletteSetting.setValue(PaletteChoice.CUSTOM);
      }
   }

   @Override
   public void fromJson(JsonElement json) {
      this.applyingPalette = true;
      try {
         super.fromJson(json);
      } finally {
         this.applyingPalette = false;
      }

      if (!(json instanceof JsonObject configuration)
         || !configuration.has("settings")
         || !configuration.get("settings").isJsonObject()
         || !configuration.getAsJsonObject("settings").has(this.paletteSetting.getConfigName())) {
         // Older configurations have no palette marker; preserve their saved colors as Custom.
         this.paletteSetting.setValue(PaletteChoice.CUSTOM);
      } else {
         UiPaletteMigration.Action action = UiPaletteMigration.resolve(this.paletteSetting.getValue(), this.snapshotPalette());
         if (action == UiPaletteMigration.Action.MIGRATE_LEGACY) this.applySelectedPalette();
         else if (action == UiPaletteMigration.Action.PRESERVE_CUSTOM) this.paletteSetting.setValue(PaletteChoice.CUSTOM);
      }
   }

   public UIModule() {
      super("UI", "Displays the client's click gui.", ModuleCategory.CLIENT, new String[0]);
      this.resetSetting = this.add(new BooleanSetting(("Reset"), true));
      this.styleSetting = this.add(new EnumSetting<>(("Style"), UIModule.StyleChoice.SCHIZO));
      this.animSpeedSetting = this.add(new NumberSetting<>("AnimSpeed", 1.0F, 0.5F, 2.5F));
      this.reducedMotionSetting = this.add(new BooleanSetting("ReducedMotion", false));
      this.reducedMotionSetting.setChangeCallback(
         () -> UiMotionPolicy.setReducedMotion(this.reducedMotionSetting.getValue())
      );
      UiMotionPolicy.setReducedMotion(this.reducedMotionSetting.getValue());
      this.scaleSetting = this.add(new NumberSetting<>("Scale", 1.0F, 0.1F, 2.0F));
      this.snowSetting = this.add(new BooleanSetting("Snow", true));
      this.gamesSetting = this.add(new BooleanSetting("Games", true));
      BooleanSetting descriptionsOption = new BooleanSetting("Descriptions", true);
      descriptionsOption.markVisibilityDependency();
      this.descriptionsSetting = this.add(descriptionsOption);
      this.languageSetting = this.add(new EnumSetting<>("Language", UIModule.LanguageChoice.ENGLISH)
         .dependsOn(this.descriptionsSetting));
      this.settingsSetting = this.add(new BooleanSetting("Settings", true)
         .dependsOn(this.descriptionsSetting));
      BooleanSetting elementsOption = new BooleanSetting("Elements", true);
      elementsOption.markAsGroup();
      elementsOption.markVisibilityDependency();
      this.elementsSetting = this.add(elementsOption);
      this.aeroSetting = this.add(new BooleanSetting("Aero", false)
         .dependsOn(this.elementsSetting));
      this.elementGradientSetting = this.add(new BooleanSetting("Gradient", true)
         .dependsOn(this.elementsSetting));
      Predicate<Object> lineVisibility = ignored -> this.elementsSetting.isExpanded();
      this.lineSetting = this.add(new BooleanSetting("Line", true, lineVisibility));
      Predicate<Object> bindsVisibility = ignored -> this.elementsSetting.isExpanded();
      this.bindsSetting = this.add(new BooleanSetting("Binds", false, bindsVisibility));
      Predicate<Object> gearVisibility = ignored -> this.elementsSetting.isExpanded();
      this.gearSetting = this.add(new BooleanSetting("Gear", true, gearVisibility));
      BooleanSetting soundsOption = new BooleanSetting("Sounds", true);
      soundsOption.markAsGroup();
      soundsOption.markVisibilityDependency();
      this.soundsSetting = this.add(soundsOption);
      Predicate<Object> hoverVisibility = ignored -> this.soundsSetting.isExpanded();
      BooleanSetting hoverOption = new BooleanSetting("Hover", true, hoverVisibility);
      hoverOption.markVisibilityDependency();
      this.hoverSetting = this.add(hoverOption);
      String hoverSoundKey = "Sound";
      this.hoverSoundSetting = this.add(new IdentifierSetting(hoverSoundKey, BuiltinSound.HOVER.getIdentifier())
         .dependsOn(this.soundsSetting, this.hoverSetting));
      NumberSetting<Float> hoverVolumeOption = new NumberSetting<>("Volume", 0.5F, 0.0F, 1.0F);
      this.hoverVolumeSetting = this.add(hoverVolumeOption.dependsOn(this.soundsSetting, this.hoverSetting));
      Predicate<Object> leftClickVisibility = ignored -> this.soundsSetting.isExpanded();
      BooleanSetting leftClickOption = new BooleanSetting("LeftClick", true, leftClickVisibility);
      leftClickOption.markVisibilityDependency();
      this.leftClickSetting = this.add(leftClickOption);
      this.leftClickSoundSetting = this.add(new IdentifierSetting("Sound", BuiltinSound.CLICK.getIdentifier())
         .dependsOn(this.soundsSetting, this.leftClickSetting));
      NumberSetting<Float> leftClickVolumeOption = new NumberSetting<>("Volume", 0.5F, 0.0F, 1.0F);
      this.leftClickVolumeSetting = this.add(leftClickVolumeOption.dependsOn(this.soundsSetting, this.leftClickSetting));
      Predicate<Object> rightClickVisibility = ignored -> this.soundsSetting.isExpanded();
      BooleanSetting rightClickOption = new BooleanSetting("RightClick", true, rightClickVisibility);
      rightClickOption.markVisibilityDependency();
      this.rightClickSetting = this.add(rightClickOption);
      this.rightClickSoundSetting = this.add(new IdentifierSetting("Sound", BuiltinSound.RCLICK.getIdentifier())
         .dependsOn(this.soundsSetting, this.rightClickSetting));
      NumberSetting<Float> rightClickVolumeOption = new NumberSetting<>("Volume", 0.5F, 0.0F, 1.0F);
      this.rightClickVolumeSetting = this.add(rightClickVolumeOption.dependsOn(this.soundsSetting, this.rightClickSetting));
      BooleanSetting textOption = new BooleanSetting("Text", false);
      textOption.markAsGroup();
      textOption.markVisibilityDependency();
      this.textSetting = this.add(textOption);
      String textColorKey = "TextColor";
      this.textColorSetting = this.add(new ColorSetting(textColorKey, new Color(-1973791, true))
         .dependsOn(this.textSetting));
      this.enabledColorSetting = this.add(new ColorSetting("EnabledColor", new Color(-1, true))
         .dependsOn(this.textSetting));
      this.paletteSetting = this.add(new EnumSetting<>("Palette", PaletteChoice.SCHIZO));
      BooleanSetting colorsOption = new BooleanSetting("Colors", false);
      colorsOption.markAsGroup();
      colorsOption.markVisibilityDependency();
      this.colorsSetting = this.add(colorsOption);
      Predicate<Object> colorVisibility = ignored -> this.colorsSetting.isExpanded();
      this.colorSetting = this.add(new ColorSetting("Color", new Color(231, 57, 77, 235), colorVisibility));
      Predicate<Object> backgroundVisibility = ignored -> this.colorsSetting.isExpanded();
      this.backgroundSetting = this.add(new ColorSetting("Background", new Color(19, 20, 26, 250), backgroundVisibility));
      Predicate<Object> buttonVisibility = ignored -> this.colorsSetting.isExpanded();
      this.buttonSetting = this.add(new ColorSetting("Button", new Color(33, 34, 42, 220), buttonVisibility));
      Predicate<Object> enabledVisibility = ignored -> this.colorsSetting.isExpanded();
      this.enabledSetting = this.add(new ColorSetting("Enabled", new Color(161, 34, 51, 150), enabledVisibility));
      Predicate<Object> tintVisibility = ignored -> this.colorsSetting.isExpanded();
      this.tintSetting = this.add(new ColorSetting("Tint", new Color(8, 9, 13, 195), tintVisibility));
      Predicate<Object> gradientColorVisibility = ignored -> this.colorsSetting.isExpanded();
      this.gradientColorSetting = this.add(new ColorSetting("Gradient", new Color(111, 19, 36, 18), gradientColorVisibility));

      this.paletteSetting.setChangeCallback(this::applySelectedPalette);
      Runnable customPaletteCallback = this::markPaletteCustom;
      this.colorSetting.setChangeCallback(customPaletteCallback);
      this.backgroundSetting.setChangeCallback(customPaletteCallback);
      this.buttonSetting.setChangeCallback(customPaletteCallback);
      this.enabledSetting.setChangeCallback(customPaletteCallback);
      this.tintSetting.setChangeCallback(customPaletteCallback);
      this.gradientColorSetting.setChangeCallback(customPaletteCallback);
      this.applySelectedPalette();

      Class<UIModule> eventOwner = UIModule.class;
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribeStatic(eventOwner);

      this.ignoreState();

      this.setDrawn(false);
      Function<Keybind, Keybind> keybindTransform = keybind -> {
         return keybind.withCode(344);
      };
      this.modifyKeybind(keybindTransform);
      INSTANCE = this;
      Setting<Boolean> gamesCallbackSetting = this.gamesSetting;
      Runnable gamesChangeCallback = () -> {
         ClickGuiScreen screen = ClientContext.clickGui;
         if (screen == null || screen.isGamePanelUnlocked()) {
            return;
         }

         ArrayList gamePanels = screen.getPanels();
         Predicate<Panel> gamePanelPredicate = panel -> panel.getName().equals("Game");
         gamePanels.removeIf(gamePanelPredicate);
         if (this.gamesSetting.getValue()) {
            screen.addGamePanel();
         }
      };
      gamesCallbackSetting.setChangeCallback(gamesChangeCallback);
      Setting<Boolean> resetCallbackSetting = this.resetSetting;
      Runnable resetChangeCallback = () -> {
         Setting<Boolean> reset = this.resetSetting;
         if (!(reset.getValue())) {
            this.resetSetting.setValue(true);
            if (ClientContext.clickGui != null) {

               ClickGuiScreen screen = ClientContext.getClickGui();

               screen.resetPanelPositions();
            }
         }
      };
      resetCallbackSetting.setChangeCallback(resetChangeCallback);
      Setting<StyleChoice> styleCallbackSetting = this.styleSetting;
      Runnable styleChangeCallback = () -> this.panelResetPending.set(true);
      styleCallbackSetting.setChangeCallback(styleChangeCallback);
      Setting<LanguageChoice> languageCallbackSetting = this.languageSetting;
      Runnable languageChangeCallback = () -> {
         LanguageTable tableCapture = ClientContext.languageTable;
         Setting<LanguageChoice> language = this.languageSetting;
         UIModule.LanguageChoice selectedLanguage = language.getValue();
         InputStream languageStream = selectedLanguage.openStream();
         tableCapture.load(languageStream);
         if (selectedLanguage == LanguageChoice.CHINESE && FontsModule.INSTANCE.isToggled()) {
            FontsModule.INSTANCE.validateFont(false);
         }
      };
      languageCallbackSetting.setChangeCallback(languageChangeCallback);
      this.hoverSoundSetting.setConfigName("HoverSound");
      this.hoverVolumeSetting.setConfigName("HoverVolume");
      this.leftClickSoundSetting.setConfigName("LeftClickSound");
      this.leftClickVolumeSetting.setConfigName("LeftClickVolume");
      this.rightClickSoundSetting.setConfigName("RightClickSound");
      this.rightClickVolumeSetting.setConfigName("RightClickVolume");
      this.backgroundSetting.setConfigName("BgColor");
      this.buttonSetting.setConfigName("BgButton");
      this.enabledSetting.setConfigName("BgEnabled");
      this.gradientColorSetting.setConfigName("GradientColor");
      Season winterCapture = Season.WINTER;
      LocalDate today = LocalDate.now();
      int month = today.getMonthValue();
      if (!winterCapture.containsMonth(month)) {
         this.snowSetting.setValue(false);
         this.unregister(this.snowSetting);
      }
   }

   public enum LanguageChoice implements NamedEntry {
      ENGLISH("English", "en_us"),
      CHINESE("Chinese", "zh_cn");

      private final String displayName;
      private final String languageCode;

      public InputStream openStream() {
         String languageCode = this.languageCode;
         return SchizoClient.class.getResourceAsStream("/assets/schizo/lang/%s.ini".formatted(languageCode));
      }

      @Override
      public String getName() {
         return this.displayName;
      }

      LanguageChoice(String displayName, String languageCode) {
         this.displayName = displayName;
         this.languageCode = languageCode;
      }
   }

   public enum PaletteChoice implements NamedEntry {
      SCHIZO("Schizo", new UiPalette(
         new Color(231, 57, 77, 235),
         new Color(19, 20, 26, 250),
         new Color(33, 34, 42, 220),
         new Color(161, 34, 51, 150),
         new Color(8, 9, 13, 195),
         new Color(111, 19, 36, 18)
      )),
      MIDNIGHT("Midnight", new UiPalette(
         new Color(93, 168, 255, 220),
         new Color(14, 18, 28, 235),
         new Color(27, 36, 52, 215),
         new Color(83, 148, 255, 118),
         new Color(5, 8, 18, 160),
         new Color(42, 94, 180, 28)
      )),
      FROST("Frost", new UiPalette(
         new Color(120, 225, 240, 225),
         new Color(20, 29, 35, 235),
         new Color(39, 55, 63, 215),
         new Color(105, 215, 230, 122),
         new Color(6, 15, 20, 150),
         new Color(61, 154, 175, 28)
      )),
      AMETHYST("Amethyst", new UiPalette(
         new Color(190, 140, 255, 225),
         new Color(27, 20, 35, 235),
         new Color(45, 34, 57, 215),
         new Color(175, 115, 235, 122),
         new Color(15, 8, 22, 155),
         new Color(125, 70, 170, 28)
      )),
      CUSTOM("Custom", null);

      private final String displayName;
      private final UiPalette palette;

      public UiPalette getPalette() {
         return this.palette;
      }

      @Override
      public String getName() {
         return this.displayName;
      }

      PaletteChoice(String displayName, UiPalette palette) {
         this.displayName = displayName;
         this.palette = palette;
      }
   }

   public enum StyleChoice implements NamedEntry {
      SCHIZO("Schizo", 21, 7, 8),
      PLAIN("Plain", 14, 4, 7),
      SLIM("Slim", 12, 3, 4);

      private final String displayName;
      private final int headerHeight;
      private final int heightPadding;
      private final int widthPadding;

      public int getHeaderHeight() {
         return this.headerHeight;
      }

      public int getHeightPadding() {
         return this.heightPadding;
      }

      public int getWidthPadding() {
         return this.widthPadding;
      }

      @Override
      public String getName() {
         return this.displayName;
      }

      StyleChoice(String displayName, int headerHeight, int heightPadding, int widthPadding) {
         this.displayName = displayName;
         this.headerHeight = headerHeight;
         this.heightPadding = heightPadding;
         this.widthPadding = widthPadding;
      }
   }
}
