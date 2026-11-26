package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.utils.TravelSession;

public final class TravelMonitor {
   public static final TravelSession SESSION = new TravelSession();

   public TravelMonitor() {
      ClientAccess.eventBus.subscribe(this);
   }

   @EventHandler
   private void onTick(LocalPlayerTickEvent event) {
      var player = ClientAccess.schizoMinecraft.player;
      if (player == null) {
         SESSION.clear();
         return;
      }
      SESSION.sample(player, ClientAccess.schizoMinecraft.level, player.getX(), player.getY(), player.getZ(), System.nanoTime());
   }

   @EventHandler
   private void onDisconnect(MinecraftDisconnectEvent event) {
      SESSION.clear();
   }
}
