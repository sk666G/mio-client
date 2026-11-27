/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.PlayerIsStayingOnGroundSurfaceEvent;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.ElapsedTimer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SafeWalkModule extends Module {
    private final Setting<Boolean> sneakSetting;
    private final Setting<Integer> delaySetting;
    private final ElapsedTimer edgeDelayTimer;

    public SafeWalkModule() {
        super("SafeWalk", "Helps not to fall from blocks if you're bad.", ModuleCategory.MOVEMENT, new String[0]);
        BooleanSetting sneak = new BooleanSetting("Sneak", false);
        sneak.markVisibilityDependency();
        sneakSetting = add(sneak);

        NumberSetting<Integer> delay = new NumberSetting<>("Delay", 0, 0, 1000);
        delay.withUnit("ms");
        delaySetting = add(delay.dependsOn(sneakSetting));
        edgeDelayTimer = new ElapsedTimer();
    }

    @Override
    public void onDisable() {
        ClientContext.inputOverrides.clearSneakRequest(this);
    }

    @EventHandler
    private void onPlayerTick(LocalPlayerTickEvent event) {
        if (!sneakSetting.getValue()) return;


        BlockPos below = BlockPos.containing(ClientAccess.schizoMinecraft.player.position().add(0.0, -0.1F, 0.0));
        BlockState block = ClientAccess.schizoMinecraft.level.getBlockState(below);
        if (shouldSneakAtEdge(block)) {
            edgeDelayTimer.reset();
            ClientContext.inputOverrides.requestSneak(this, true);
        } else if (!edgeDelayTimer.hasElapsed((delaySetting.getValue()).longValue())) {
            ClientContext.inputOverrides.requestSneak(this, true);
        }
    }

    @EventHandler
    private void onGroundSurfaceQuery(PlayerIsStayingOnGroundSurfaceEvent event) {
        if (!ClientAccess.schizoMinecraft.player.isSwimming()
                && !ClientAccess.schizoMinecraft.player.isInLava()
                && !ClientAccess.schizoMinecraft.player.isInWater()
                && !sneakSetting.getValue()) event.cancel();
    }

    private boolean shouldSneakAtEdge(BlockState block) {
        return EntityUtils.hasCollisionBelow(ClientAccess.schizoMinecraft.player)
                && !ClientAccess.schizoMinecraft.player.isInWater()
                && !ClientAccess.schizoMinecraft.player.isInLava()
                && isUnsupportedBlock(block);
    }

    private boolean isUnsupportedBlock(BlockState block) {

        return block.is(Blocks.AIR) || block.getCollisionShape(ClientAccess.schizoMinecraft.level, BlockPos.ZERO).isEmpty();
    }
}
