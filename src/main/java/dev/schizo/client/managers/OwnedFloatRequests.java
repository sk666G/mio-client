package dev.schizo.client.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Latest-request-wins ownership for one shared float value. */
final class OwnedFloatRequests<O> {
   private record Request(float value, long sequence) {
   }

   private final float fallback;
   private final Map<O, Request> requests = new HashMap<>();
   private long sequence;
   private float effective;

   OwnedFloatRequests(float fallback) {
      this.fallback = fallback;
      this.effective = fallback;
   }

   synchronized void request(O owner, float value) {
      Objects.requireNonNull(owner, "owner");
      if (!Float.isFinite(value)) {
         throw new IllegalArgumentException("Requested value must be finite");
      }
      this.requests.put(owner, new Request(value, ++this.sequence));
      this.effective = value;
   }

   synchronized void release(O owner) {
      if (owner == null || this.requests.remove(owner) == null) {
         return;
      }
      this.recompute();
   }

   synchronized float get() {
      return this.effective;
   }

   synchronized int size() {
      return this.requests.size();
   }

   synchronized void clear() {
      this.requests.clear();
      this.effective = this.fallback;
   }

   private void recompute() {
      Request latest = null;
      for (Request request : this.requests.values()) {
         if (latest == null || request.sequence() > latest.sequence()) {
            latest = request;
         }
      }
      this.effective = latest == null ? this.fallback : latest.value();
   }
}
