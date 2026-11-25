/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

public enum IntegrationHealth {
   AVAILABLE("available"),
   UNAVAILABLE("unavailable"),
   DISABLED("disabled"),
   CONNECTING("connecting"),
   CONNECTED("connected"),
   FAILED("failed"),
   NOT_CONFIGURED("not configured"),
   PAUSED("paused"),
   ACTIVE("active"),
   IDLE("idle");

   private final String label;

   IntegrationHealth(String label) {
      this.label = label;
   }

   public String label() {
      return this.label;
   }
}
