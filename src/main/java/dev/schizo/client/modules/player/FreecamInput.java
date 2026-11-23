/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.player;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.movement.NoSlowModule;

import dev.schizo.client.utils.KeyUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;

public class FreecamInput extends ClientInput implements ClientAccess {
   private static NoSlowModule noSlow;

   static {
      noSlow = (NoSlowModule)ClientContext.getModule(NoSlowModule.class);
   }

   private boolean isMovementKeyDown(KeyMapping mapping) {
      if (noSlow.isToggled() && noSlow.isGuiMoveAllowed() && noSlow.guiMoveSetting.getValue()) {
         return KeyUtils.isPhysicallyDown(mapping);
      }
      return mapping.isDown();
   }

   @Override
   public void tick() {
      KeyMapping forwardKey = ClientAccess.schizoMinecraft.options.keyUp;
      boolean positiveDown = this.isMovementKeyDown(forwardKey);
      KeyMapping backKey = ClientAccess.schizoMinecraft.options.keyDown;
      boolean negativeDown = this.isMovementKeyDown(backKey);

      float forward = this.getAxisInput(positiveDown, negativeDown);
      KeyMapping leftKey = ClientAccess.schizoMinecraft.options.keyLeft;
      positiveDown = this.isMovementKeyDown(leftKey);
      KeyMapping rightKey = ClientAccess.schizoMinecraft.options.keyRight;
      negativeDown = this.isMovementKeyDown(rightKey);

      float sideways = this.getAxisInput(positiveDown, negativeDown);
      this.moveVector = new Vec2(sideways, forward);
   }

   private float getAxisInput(boolean positiveDown, boolean negativeDown) {
      return positiveDown == negativeDown ? 0.0F : positiveDown ? 1.0F : -1.0F;
   }

   public static void bindModules(ModuleRegistry registry) {
      noSlow = (NoSlowModule)registry.getModule(NoSlowModule.class);
   }
}
