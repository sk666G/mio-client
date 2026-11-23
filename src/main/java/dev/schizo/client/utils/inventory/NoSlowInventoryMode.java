/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.inventory;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.PacketEvent;
import dev.schizo.client.events.InventoryActionEvent;
import dev.schizo.client.events.LocalPlayerPreTickEvent;
import dev.schizo.client.events.MultiPlayerGameModeHandleInventoryMouseClickEvent;
import dev.schizo.client.modules.movement.NoSlowModule;

public class NoSlowInventoryMode implements ClientAccess {
   protected final NoSlowModule noSlow;

   public NoSlowInventoryMode(NoSlowModule noSlow) {
      this.noSlow = noSlow;
   }

   public void onPacketSend(
      PacketEvent.Send event
   ) {
   }

   public void onTickStart(LocalPlayerPreTickEvent event) {
   }

   public void onInventoryAction(InventoryActionEvent event) {
   }

   public void beforeMenuClick(
      MultiPlayerGameModeHandleInventoryMouseClickEvent.BeforeContainerClick event
   ) {
   }

   public void onInventoryClickIntercept(
      MultiPlayerGameModeHandleInventoryMouseClickEvent.AfterStackHash event
   ) {
   }

   public void onPlayerAiStep() {
   }

   public void onTickBeforePassengerCheck() {
   }
}
