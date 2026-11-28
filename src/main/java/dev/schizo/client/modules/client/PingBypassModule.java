package dev.schizo.client.modules.client;

import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftTickEvent;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.utils.network.CompanionHealthProbe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Companion connectivity only; does not proxy gameplay or change server ping. */
public final class PingBypassModule extends Module {
   private final NumberSetting<Integer> interval = this.add(new NumberSetting<>("IntervalSeconds", 10, 5, 60));
   private CompanionHealthProbe probe;
   private CompletableFuture<CompanionHealthProbe.Result> pending;
   private long lastAttempt;
   private boolean attempted;
   private String status = "Disabled";
   private long latency = -1;

   public PingBypassModule() {
      super("PingBypass", "Authenticated remote companion health and latency diagnostics. Does not reroute game traffic.",
         ModuleCategory.CLIENT, "CompanionHealth");
      this.setDrawn(false);
   }

   @Override public void onEnable() {
      latency = -1;
      attempted = false;
      try {
         // Both values are operator-provided environment configuration, never imported presets.
         probe = new CompanionHealthProbe(System.getenv("SCHIZO_COMPANION_URL"),
            System.getenv("SCHIZO_COMPANION_TOKEN"));
         status = "Waiting for sample";
      } catch (IllegalArgumentException | SecurityException invalid) {
         status = "Configure companion environment";
      }
   }

   @EventHandler
   private void onTick(MinecraftTickEvent event) {
      if (probe == null) return;
      if (pending != null) {
         if (!pending.isDone()) return;
         try {
            var result = pending.join();
            status = result.status();
            latency = result.millis();
         } catch (java.util.concurrent.CompletionException | java.util.concurrent.CancellationException failure) {
            status = "Connection failed";
            latency = -1;
         } finally {
            pending = null;
         }
      }
      long now = System.nanoTime();
      if (attempted && now - lastAttempt < TimeUnit.SECONDS.toNanos(interval.getValue())) return;
      lastAttempt = now;
      attempted = true;
      try {
         pending = probe.sample();
         status = "Checking companion";
      } catch (RuntimeException failure) {
         status = "Connection failed";
         latency = -1;
      }
   }

   @Override public void onDisable() {
      if (pending != null) pending.cancel(true);
      pending = null;
      if (probe != null) probe.shutdown();
      probe = null;
      status = "Disabled";
      latency = -1;
   }

   @Override public String getInfo() {
      return latency >= 0 ? latency + " ms HTTPS" : status;
   }

   @Override public String getRuntimeStatus() {
      if (!isToggled()) return "Disabled";
      return latency >= 0 ? status + " - " + latency + " ms HTTPS" : status;
   }
}
