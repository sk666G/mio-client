/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;


import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.WorldRenderEvent;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.settings.SettingLabelCondition;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.modules.exploit.MultiTaskModule;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.math.MathUtils;

import com.mojang.blaze3d.platform.InputConstants.Key;
import dev.schizo.mixin.MinecraftAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AutoClickerModule extends Module {
   private static MultiTaskModule multiTaskModule;
   private final Setting<ModeChoice> modeSetting;
   private final Setting<Float> cpsSetting;
   private final Setting<Float> delaySetting;
   private final Setting<Boolean> friendProtectSetting;
   private final ElapsedTimer clickTimer;
   private float delayMultiplier;
   private final List<Long> recentClickTimes;
   @Override
   public String getInfo() {
      int recentClickCount = this.recentClickTimes.size();
      return String.valueOf(recentClickCount);
   }

   @EventHandler
   private void onWorldRender(WorldRenderEvent.BeforeBufferSource event) {
      this.recentClickTimes.removeIf(timestamp -> System.currentTimeMillis() - timestamp >= 1000L);

      float clicksPerSecond = this.cpsSetting.getValue();
      long clickIntervalMillis = (long)(1000.0F / clicksPerSecond);

      if (!this.delaySetting.isAtMinimum()) {

         clickIntervalMillis *= (long)this.delayMultiplier;
      }

      if (this.clickTimer.hasElapsed(clickIntervalMillis)) {
         this.performClick();
         this.recentClickTimes.add(System.currentTimeMillis());

         this.clickTimer.reset();

         float upperBound = this.delaySetting.getValue();
         this.delayMultiplier = MathUtils.randomFloat(
            1.0F, upperBound
         );
      }
   }

   private void performClick() {
      if (multiTaskModule.isToggled()) {
         if (ClientAccess.schizoMinecraft.player.isUsingItem() || ClientAccess.schizoMinecraft.screen != null) {
            (this.modeSetting.getValue()).clickDirectly();
            return;
         }
      }

      Key key = (this.modeSetting.getValue()).getKeyMapping().key;

      KeyMapping.click(key);
   }

   public boolean shouldProtectFriend(Entity target) {
      if (this.isToggled()) {
         if (this.friendProtectSetting.getValue() && target instanceof Player playerTarget) {
            Player friendTarget = playerTarget;

            if (ClientContext.socialRegistry.isFriendPlayer(friendTarget)) {
               return true;
            }
         }
      }

      return false;
   }

   public static void bindModules(ModuleRegistry registry) {
      multiTaskModule = (MultiTaskModule)registry.getModule(
         MultiTaskModule.class
      );
   }

   public AutoClickerModule() {
      super("AutoClicker", "Spam mouse buttons.", ModuleCategory.COMBAT, new String[0]);
      this.modeSetting = this.add(new EnumSetting<>("Mode", AutoClickerModule.ModeChoice.LEFT));
      this.cpsSetting = this.add(new NumberSetting<>("CPS", 8.0F, 1.0F, 20.0F));
      this.delaySetting = this.add(new NumberSetting<>("Delay", 1.0F, 1.0F, 3.0F)
         .withUnit("x").setBoundaryLabel("Off", SettingLabelCondition.MIN));
      this.friendProtectSetting = this.add(new BooleanSetting("FriendProtect", true));
      this.clickTimer = new ElapsedTimer();

      this.delayMultiplier = 1.0F;
      this.recentClickTimes = new ArrayList<>();
   }

   public enum ModeChoice implements NamedEntry {
      LEFT("Left"),
      RIGHT("Right");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      ModeChoice(String displayName) {
         this.displayName = displayName;
      }

      public KeyMapping getKeyMapping() {
         return this == LEFT
            ? ClientAccess.schizoMinecraft.options.keyAttack
            : ClientAccess.schizoMinecraft.options.keyUse;
      }

      public void clickDirectly() {
         if (this == LEFT) {
            ((MinecraftAccessor)ClientAccess.schizoMinecraft).attack();
         } else {
            ((MinecraftAccessor)ClientAccess.schizoMinecraft).interact();
         }
      }
   }
}
