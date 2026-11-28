package dev.schizo.client.profiling;

import java.lang.management.ManagementFactory;

/** JVM heap and cumulative GC counters, not native/GPU memory or measured pause duration. */
public record JvmMemorySnapshot(long usedBytes, long maximumBytes, long collections, long collectionMillis) {
   public static JvmMemorySnapshot capture() {
      Runtime runtime = Runtime.getRuntime();
      long count = 0, millis = 0;
      boolean countSupported = false, timeSupported = false;
      for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
         long n = bean.getCollectionCount();
         long ms = bean.getCollectionTime();
         if (n >= 0) { count += n; countSupported = true; }
         if (ms >= 0) { millis += ms; timeSupported = true; }
      }
      return new JvmMemorySnapshot(Math.max(0, runtime.totalMemory() - runtime.freeMemory()),
         runtime.maxMemory(), countSupported ? count : -1, timeSupported ? millis : -1);
   }
}
