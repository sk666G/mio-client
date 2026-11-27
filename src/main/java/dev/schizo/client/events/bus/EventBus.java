/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.events.bus;

import dev.schizo.client.events.Cancellable;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.modules.Module;
import dev.schizo.client.profiling.EventProfiler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus implements EventDispatcher {
   private final EventProfiler profiler;
   private final EventFailureTracker failureTracker;
   private final Map<Object, List<EventListener>> instanceListenerCache =
      Collections.synchronizedMap(new IdentityHashMap<>());
   private final Map<Class<?>, List<EventListener>> staticListenerCache = new ConcurrentHashMap<>();
   private final Map<Class<?>, EventListener[]> listenersByEventClass = new ConcurrentHashMap<>();

   public EventBus() {
      this(EventProfiler.GLOBAL, EventFailureTracker.GLOBAL);
   }

   public EventBus(EventProfiler profiler) {
      this(profiler, EventFailureTracker.GLOBAL);
   }

   EventBus(EventProfiler profiler, EventFailureTracker failureTracker) {
      this.profiler = java.util.Objects.requireNonNull(profiler);
      this.failureTracker = java.util.Objects.requireNonNull(failureTracker);
   }

   @Override
   public void subscribe(Object subscriber) {
      this.subscribeListeners(this.getOrCreateListeners(subscriber.getClass(), subscriber), false);
   }

   @Override
   public void unsubscribe(Object subscriber) {
      List<EventListener> listeners;
      synchronized (this.instanceListenerCache) {
         listeners = this.instanceListenerCache.remove(subscriber);
      }
      if (listeners != null) {
         this.unsubscribeListeners(listeners, false);
      }
   }

   int cachedSubscriberCount() {
      synchronized (this.instanceListenerCache) {
         return this.instanceListenerCache.size();
      }
   }

   @Override
   public void subscribeStatic(Class<?> listenerClass) {
      this.subscribeListeners(this.getOrCreateListeners(listenerClass, null), true);
   }

   @Override
   public void unsubscribeStatic(Class<?> listenerClass) {
      List<EventListener> listeners = this.staticListenerCache.get(listenerClass);
      if (listeners != null) {
         this.unsubscribeListeners(listeners, true);
      }
   }

   private List<EventListener> getOrCreateListeners(Class<?> listenerClass, Object subscriber) {
      if (subscriber == null) {
         return this.staticListenerCache.computeIfAbsent(listenerClass, type -> this.collectListeners(type, null));
      }
      synchronized (this.instanceListenerCache) {
         return this.instanceListenerCache.computeIfAbsent(subscriber, key -> this.collectListeners(listenerClass, subscriber));
      }
   }

   private List<EventListener> collectListeners(Class<?> listenerClass, Object subscriber) {
      List<EventListener> listeners = new ArrayList<>();
      String moduleName = subscriber instanceof Module module ? module.getName() : null;
      String ownerName = moduleName == null ? listenerClass.getSimpleName() : moduleName;
      for (Class<?> type = listenerClass; type != null; type = type.getSuperclass()) {
         for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)
               && method.getReturnType() == void.class
               && method.getParameterCount() == 1
               && !method.getParameterTypes()[0].isPrimitive()) {
               listeners.add(new MethodEventListener(type, subscriber, method, this.profiler, ownerName, moduleName));
            }
         }
      }
      return List.copyOf(listeners);
   }

   private void subscribeListeners(List<EventListener> listeners, boolean staticOnly) {
      for (EventListener listener : listeners) {
         if (!staticOnly || listener.isStatic()) {
            this.subscribeListener(listener);
         }
      }
   }

   private void unsubscribeListeners(List<EventListener> listeners, boolean staticOnly) {
      for (EventListener listener : listeners) {
         if (!staticOnly || listener.isStatic()) {
            this.unsubscribeListener(listener);
         }
      }
   }

   @Override
   public void subscribeListener(EventListener listener) {
      Class<?> eventType = listener.getEventType();
      this.listenersByEventClass.compute(eventType, (ignored, current) -> {
         EventListener[] previous = current == null ? new EventListener[0] : current;
         for (EventListener existing : previous) {
            if (existing == listener || existing.equals(listener)) {
               return previous;
            }
         }

         int insertionIndex = 0;
         while (insertionIndex < previous.length
            && listener.getPriority() <= previous[insertionIndex].getPriority()) {
            insertionIndex++;
         }

         EventListener[] updated = new EventListener[previous.length + 1];
         System.arraycopy(previous, 0, updated, 0, insertionIndex);
         updated[insertionIndex] = listener;
         System.arraycopy(previous, insertionIndex, updated, insertionIndex + 1,
            previous.length - insertionIndex);
         return updated;
      });
   }

   @Override
   public void unsubscribeListener(EventListener listener) {
      this.listenersByEventClass.computeIfPresent(listener.getEventType(), (ignored, current) -> {
         int kept = 0;
         for (EventListener existing : current) {
            if (existing != listener && !existing.equals(listener)) {
               kept++;
            }
         }
         if (kept == current.length) {
            return current;
         }
         if (kept == 0) {
            return null;
         }

         EventListener[] updated = new EventListener[kept];
         int target = 0;
         for (EventListener existing : current) {
            if (existing != listener && !existing.equals(listener)) {
               updated[target++] = existing;
            }
         }
         return updated;
      });
   }

   /**
    * Returns whether an event has subscribers without allocating an event object.
    */
   public boolean hasListeners(Class<?> eventType) {
      EventListener[] listeners = this.listenersByEventClass.get(eventType);
      return listeners != null && listeners.length != 0;
   }

   @Override
   public Object post(Object event) {
      EventListener[] listeners = this.listenersByEventClass.get(event.getClass());
      if (listeners != null) {
         for (int index = 0; index < listeners.length; index++) {
            this.invokeListener(listeners[index], event);
         }
      }
      return event;
   }

   /**
    * Dispatches cleanup events to every listener even when an earlier listener throws a RuntimeException.
    * Errors remain fail-fast. The first runtime failure is rethrown after dispatch with later failures suppressed.
    */
   public Object postCleanup(Object event) {
      EventListener[] listeners = this.listenersByEventClass.get(event.getClass());
      RuntimeException firstFailure = null;
      if (listeners != null) {
         for (EventListener listener : listeners) {
            try {
               this.invokeListener(listener, event);
            } catch (RuntimeException failure) {
               if (firstFailure == null) {
                  firstFailure = failure;
               } else if (firstFailure != failure) {
                  firstFailure.addSuppressed(failure);
               }
            }
         }
      }
      if (firstFailure != null) {
         throw firstFailure;
      }
      return event;
   }

   @Override
   public Cancellable postCancellable(Cancellable event) {
      EventListener[] listeners = this.listenersByEventClass.get(event.getClass());
      if (listeners != null) {
         event.setCancelled(false);
         for (int index = 0; index < listeners.length; index++) {
            EventListener listener = listeners[index];
            this.invokeListener(listener, event);
            if (event.isCancelled()) {
               break;
            }
         }
      }
      return event;
   }
   private void invokeListener(EventListener listener, Object event) {
      try {
         if (this.profiler.isEnabled()) {
            this.callProfiled(listener, event);
         } else {
            listener.call(event);
         }
      } catch (RuntimeException | Error failure) {
         this.failureTracker.record(listener, event, failure);
         throw failure;
      }
   }

   private void callProfiled(EventListener listener, Object event) {
      EventProfiler.CallbackHandle handle = listener.profileHandle();
      if (handle == null) {
         listener.call(event);
         return;
      }
      long startedAt = this.profiler.nanoTime();
      Throwable callbackFailure = null;
      try {
         listener.call(event);
      } catch (RuntimeException | Error failure) {
         callbackFailure = failure;
         throw failure;
      } finally {
         try {
            this.profiler.recordCallback(handle, this.profiler.nanoTime() - startedAt);
         } catch (RuntimeException | Error profilingFailure) {
            if (callbackFailure == null) throw profilingFailure;
            if (callbackFailure != profilingFailure) callbackFailure.addSuppressed(profilingFailure);
         }
      }
   }
}

