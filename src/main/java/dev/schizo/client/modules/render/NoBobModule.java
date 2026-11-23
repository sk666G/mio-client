/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;

import dev.schizo.client.ClientAccess;

import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import net.minecraft.client.player.LocalPlayer;

public class NoBobModule extends Module {
    public final Setting<Float> multiplierSetting;

    public NoBobModule() {
        super("NoBob", "Modifies the bobbing animation.", ModuleCategory.RENDER, new String[0]);
        multiplierSetting = add(new NumberSetting<>("Multiplier", 0.0F, 0.0F, 1.0F));
    }

    @EventHandler
    private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
        if (isWorldUnavailable()) return;

        Float multiplier = multiplierSetting.getValue();
        if (multiplier <= 0.0F) {
            LocalPlayer player = ClientAccess.schizoMinecraft.player;
            player.avatarState().walkDist = 0.0F;
        }
    }
}
