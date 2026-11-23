package dev.schizo.client.feature;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selects the fastest tool already present in the hotbar for the block being mined.
 * This is a local inventory convenience and does not alter server-side mining rules.
 */
public final class AutoToolFeature extends Feature {
    public AutoToolFeature() {
        super("auto-tool", "Auto Tool",
            "Selects the best hotbar tool while targeting a block with the attack key.",
            FeatureCategory.PLAYER);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled() || client.player == null || client.level == null
                    || client.screen != null || !client.options.keyAttack.isDown()) {
                return;
            }
            if (client.hitResult == null
                    || client.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                    || !(client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)) {
                return;
            }

            BlockState state = client.level.getBlockState(hit.getBlockPos());
            int bestSlot = client.player.getInventory().getSelectedSlot();
            double bestScore = score(client.player.getInventory().getItem(bestSlot), state);

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = client.player.getInventory().getItem(slot);
                double score = score(stack, state);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = slot;
                }
            }
            client.player.getInventory().setSelectedSlot(bestSlot);
        });
    }

    private static double score(ItemStack stack, BlockState state) {
        double speed = stack.getDestroySpeed(state);
        return speed + (stack.isCorrectToolForDrops(state) ? 1000.0 : 0.0);
    }

    @Override
    protected void onDisable() {
        // The selected slot remains under the player's control when the module is disabled.
    }
}
