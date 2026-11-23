package dev.schizo.client.gui;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.config.ClientConfig;
import dev.schizo.client.feature.Feature;
import dev.schizo.client.feature.FeatureCategory;
import dev.schizo.client.setting.BooleanSetting;
import dev.schizo.client.setting.ModeSetting;
import dev.schizo.client.setting.NumberSetting;
import dev.schizo.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lightweight in-game screen for toggling modules and editing their settings. */
public final class ClickGuiScreen extends Screen {
    private static final int BUTTON_WIDTH = 230;
    private static final int BUTTON_HEIGHT = 20;
    private final Screen parent;
    private final FeatureCategory selectedCategory;

    public ClickGuiScreen(Screen parent) {
        this(parent, null);
    }

    private ClickGuiScreen(Screen parent, FeatureCategory selectedCategory) {
        super(Component.literal("Schizo"));
        this.parent = parent;
        this.selectedCategory = selectedCategory;
    }

    @Override
    protected void init() {
        int left = (width - BUTTON_WIDTH) / 2;
        int categoryY = 28;
        FeatureCategory[] categories = FeatureCategory.values();
        int categoryWidth = Math.min(78, (width - 16) / categories.length);
        int totalWidth = categoryWidth * categories.length;
        int categoryLeft = (width - totalWidth) / 2;

        for (int i = 0; i < categories.length; i++) {
            FeatureCategory category = categories[i];
            String label = category == selectedCategory ? "[" + shortName(category) + "]" : shortName(category);
            addRenderableWidget(Button.builder(Component.literal(label), button ->
                    minecraft.setScreen(new ClickGuiScreen(parent, category)))
                .bounds(categoryLeft + i * categoryWidth, categoryY, categoryWidth - 2, BUTTON_HEIGHT)
                .build());
        }

        addRenderableWidget(Button.builder(Component.literal("All"), button ->
                minecraft.setScreen(new ClickGuiScreen(parent, null)))
            .bounds(left, 52, 52, BUTTON_HEIGHT).build());

        int y = 78;
        int visible = 0;
        for (Feature feature : FeatureRegistry.all()) {
            if (selectedCategory != null && feature.category() != selectedCategory) continue;
            if (visible++ >= 18 || y + BUTTON_HEIGHT > height - 28) break;

            addRenderableWidget(Button.builder(moduleLabel(feature), button -> {
                feature.toggle();
                button.setMessage(moduleLabel(feature));
                ClientConfig.save();
            }).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            y += 23;

            for (Setting<?> setting : feature.settings()) {
                if (y + BUTTON_HEIGHT > height - 28) break;
                addRenderableWidget(settingButton(setting, left + 12, y));
                y += 22;
            }
            y += 3;
        }

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
            .bounds(left, height - 24, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private Button settingButton(Setting<?> setting, int x, int y) {
        return Button.builder(settingLabel(setting), button -> {
            cycle(setting);
            button.setMessage(settingLabel(setting));
            ClientConfig.save();
        }).bounds(x, y, BUTTON_WIDTH - 12, BUTTON_HEIGHT).build();
    }

    private static void cycle(Setting<?> setting) {
        if (setting instanceof BooleanSetting bool) {
            bool.set(!bool.get());
        } else if (setting instanceof NumberSetting number) {
            double next = number.get() + number.step();
            number.set(next > number.max() ? number.min() : next);
        } else if (setting instanceof ModeSetting mode) {
            int current = mode.modes().indexOf(mode.get());
            mode.set(mode.modes().get((current + 1) % mode.modes().size()));
        }
    }

    private static Component moduleLabel(Feature feature) {
        return Component.literal((feature.isEnabled() ? "[ON] " : "[OFF] ")
            + feature.name() + "  -  " + feature.category().name());
    }

    private static Component settingLabel(Setting<?> setting) {
        return Component.literal("  " + setting.name() + ": " + setting.get());
    }

    private static String shortName(FeatureCategory category) {
        return switch (category) {
            case MOVEMENT -> "Move";
            case PLAYER -> "Player";
            case RENDER -> "Render";
            case COMBAT -> "Combat";
            case WORLD -> "World";
            case MISC -> "Misc";
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
