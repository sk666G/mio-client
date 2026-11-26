package dev.schizo.client.utils;

import java.util.Objects;

/** Client-thread retry accounting, retained across disconnect screen replacements. */
public final class ReconnectAttempts {
   private String address;
   private int attempts;

   public void select(String address) {
      if (!Objects.equals(this.address, address)) {
         this.address = address;
         this.attempts = 0;
      }
   }

   public boolean claim(int maximum) {
      if (this.attempts >= maximum) return false;
      this.attempts++;
      return true;
   }

   public boolean exhausted(int maximum) {
      return this.attempts >= maximum;
   }

   public double delaySeconds(double configured, boolean backoff) {
      if (!backoff || this.attempts == 0) return configured;
      return Math.min(Math.max(60.0, configured), Math.max(1.0, configured) * (1L << Math.min(6, this.attempts)));
   }

   public void reset() {
      this.address = null;
      this.attempts = 0;
   }

}
