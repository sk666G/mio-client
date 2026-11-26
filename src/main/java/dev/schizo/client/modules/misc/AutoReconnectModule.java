/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.misc;

import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.ReconnectAttempts;

public class AutoReconnectModule extends Module {
   public final Setting<Integer> minutesSetting;
   public final Setting<Float> secondsSetting;
   public final Setting<Integer> attemptsSetting;
   public final Setting<Boolean> backoffSetting;
   public final ReconnectAttempts attempts = new ReconnectAttempts();

   public AutoReconnectModule() {
      super("AutoReconnect", "Reconnects to the server automatically if you get kicked.", ModuleCategory.MISC, new String[0]);
      this.minutesSetting = this.add(new NumberSetting<>("Minutes", 0, 0, 15).withUnit("m"));
      this.secondsSetting = this.add(new NumberSetting<>("Seconds", 5.0F, 0.0F, 59.0F).withUnit("s"));
      this.secondsSetting.setConfigName("Delay");
      this.attemptsSetting = this.add(new NumberSetting<>("MaxAttempts", 10, 1, 100));
      this.backoffSetting = this.add(new BooleanSetting("Backoff", true));
      this.setDrawn(false);
   }

   @Override
   public void onToggle() {
      this.attempts.reset();
   }

   @EventHandler
   private void onPlayerTick(LocalPlayerTickEvent event) {
      // A successfully joined session starts a new retry sequence.
      this.attempts.reset();
   }
}
