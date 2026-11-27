/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.KeyboardInputTickEvent;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.utils.InputUtils;
import java.util.function.Consumer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

public final class InputOverrideManager {
    private final PendingValue<Boolean> pendingSneak = new PendingValue<>();
    private final PendingValue<Boolean> pendingJump = new PendingValue<>();
    private final PendingValue<Boolean> pendingForward = new PendingValue<>();
    private final PendingValue<Boolean> pendingBack = new PendingValue<>();
    private final PendingValue<Boolean> pendingLeft = new PendingValue<>();
    private final PendingValue<Boolean> pendingRight = new PendingValue<>();
    private final PendingValue<Boolean> pendingSprint = new PendingValue<>();
    private final PendingValue<Float> pendingForwardMovement = new PendingValue<>();
    private final PendingValue<Float> pendingSidewaysMovement = new PendingValue<>();
    private final PendingFlag sprintBlock = new PendingFlag();

    public InputOverrideManager() {
        ClientAccess.eventBus.subscribe(this);
    }

    public void requestSneak(boolean value) {
        pendingSneak.set(value);
    }

    public void requestSneak(Object owner, boolean value) {
        pendingSneak.set(owner, value);
    }

    public void clearSneakRequest(Object owner) {
        pendingSneak.clear(owner);
    }

    public void requestJump(boolean value) {
        pendingJump.set(value);
    }

    public void requestJump(Object owner, boolean value) {
        pendingJump.set(owner, value);
    }

    public void clearJumpRequest(Object owner) {
        pendingJump.clear(owner);
    }

    public void requestForward(boolean value) {
        pendingForward.set(value);
    }

    public void requestBack(boolean value) {
        pendingBack.set(value);
    }

    public void requestLeft(boolean value) {
        pendingLeft.set(value);
    }

    public void requestRight(boolean value) {
        pendingRight.set(value);
    }

    public void requestSprint(boolean value) {
        pendingSprint.set(value);
    }

    public void requestForwardMovement(float value) {
        pendingForwardMovement.set(value);
    }

    public void requestSidewaysMovement(float value) {
        pendingSidewaysMovement.set(value);
    }

    public boolean isSprintBlocked() {
        return sprintBlock.isPending();
    }

    public void blockSprint() {
        sprintBlock.markPending();
    }

    public Input getInput() {
        if (ClientAccess.schizoMinecraft.player == null) {
            throw new IllegalStateException("Player is null");
        }
        return ClientAccess.schizoMinecraft.player.input.keyPresses;
    }

    public void requestMovement(float forward, float sideways) {
        requestForwardMovement(forward);
        requestSidewaysMovement(sideways);
    }

    public void requestReset() {
        requestSneak(false);
        requestSprint(false);
        requestJump(false);
        requestForward(false);
        requestBack(false);
        requestLeft(false);
        requestRight(false);
        requestMovement(0.0F, 0.0F);
    }

    @EventHandler(priority = -100)
    private void onLocalPlayerTickEvent(LocalPlayerTickEvent event) {
        sprintBlock.reset();
    }

    @EventHandler
    private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
        pendingSneak.clear();
        pendingJump.clear();
        pendingForward.clear();
        pendingBack.clear();
        pendingLeft.clear();
        pendingRight.clear();
        pendingSprint.clear();
        pendingForwardMovement.clear();
        pendingSidewaysMovement.clear();
        sprintBlock.reset();
    }

    @EventHandler(priority = -100)
    private void onKeyboardInputTickEvent(KeyboardInputTickEvent event) {
        ClientInput input = event.getInput();
        pendingSneak.consume(value -> InputUtils.setSneak(input, value));
        pendingJump.consume(value -> InputUtils.setJump(input, value));
        pendingForward.consume(value -> InputUtils.setForward(input, value));
        pendingBack.consume(value -> InputUtils.setBack(input, value));
        pendingLeft.consume(value -> InputUtils.setLeft(input, value));
        pendingRight.consume(value -> InputUtils.setRight(input, value));
        pendingSprint.consume(value -> InputUtils.setSprint(input, value));
        pendingForwardMovement.consume(value -> InputUtils.setForwardMovement(input, value));
        pendingSidewaysMovement.consume(value -> InputUtils.setSidewaysMovement(input, value));
    }

    private static final class PendingValue<T> extends PendingFlag {
        private T value;
        private Object owner;

        public void set(T value) {
            this.set(null, value);
        }

        public void set(Object owner, T value) {
            this.owner = owner;
            this.value = value;
            markPending();
        }

        void consume(Consumer<T> consumer) {
            if (isPending()) {
                consumer.accept(value);
                clear();
            }
        }

        void clear(Object owner) {
            if (isPending() && this.owner == owner) {
                clear();
            }
        }

        void clear() {
            value = null;
            owner = null;
            reset();
        }
    }

    private static class PendingFlag {
        private boolean pending;

        boolean isPending() {
            return pending;
        }

        void markPending() {
            pending = true;
        }

        void reset() {
            pending = false;
        }
    }

}
