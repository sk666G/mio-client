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
import dev.schizo.client.diagnostics.RuntimeDiagnostics;
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
   private final Map<Object, List<EventListener>> instanceListenerCache =
      Collections.synchronizedMap(new IdentityHashMap<>());
   private final Map<Class<?>, List<EventListener>> staticListenerCache = new ConcurrentHashMap<>();
   private final Map<Class<?>, EventListener[]> listenersByEventClass = new ConcurrentHashMap<>();

   public EventBus() {
      this(EventProfiler.GLOBAL);
   }

   public EventBus(EventProfiler profiler) {
      this.profiler = java.util.Objects.requireNonNull(profiler);
   }

   @Override
   public void subscribe(Object subscriber) {
      this.subscribeListeners(this.getOrCreateListeners(subscriber.getClass(), subscriber), false);
   }

   @Override
   public void unsubscribe(Object subscriber) {
      List<EventListener> listeners;
      synchronized (this.instanceListenerCache) {
         listeners = this.instanceListenerCache.get(subscriber);
      }
      if (listeners != null) {
         this.unsubscribeListeners(listeners, false);
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

   /**
    * Dispatches an event while containing ordinary listener failures at the boundary.
    * Fatal {@link Error}s still propagate. This is intended for external/network callbacks
    * where one broken subscriber must not tear down the transport thread.
    */
   public Object postRecoverable(Object event) {
      EventListener[] listeners = this.listenersByEventClass.get(event.getClass());
      if (listeners == null) {
         return event;
      }

      boolean profiled = this.profiler.isEnabled();
      for (EventListener listener : listeners) {
         try {
            if (profiled) {
               this.callProfiled(listener, event);
            } else {
               this.callUnprofiled(listener, event);
            }
         } catch (RuntimeException ignoredFailure) {
            // callUnprofiled/callProfiled already attributed and recorded the failure.
         }
      }
      return event;
   }

   @Override
   public Object post(Object event) {
      EventListener[] listeners = this.listenersByEventClass.get(event.getClass());
      if (listeners != null) {
         if (!this.profiler.isEnabled()) {
            for (int index = 0; index < listeners.length; index++) {
               this.callUnprofiled(listeners[index], event);
            }
         } else {
            for (int index = 0; index < listeners.length; index++) {
               this.callProfiled(listeners[index], event);
            }
         }
      }
      return event;
   }

   @Override
   public Cancellable postCancellable(Cancellable event) {
      EventListener[] listeners = this.listenersByEventClass.get(event.getClass());
      if (listeners != null) {
         event.setCancelled(false);
         if (!this.profiler.isEnabled()) {
            for (int index = 0; index < listeners.length; index++) {
               EventListener listener = listeners[index];
               listener.call(event);
               if (event.isCancelled()) {
                  break;
               }
            }
         } else {
            for (int index = 0; index < listeners.length; index++) {
               EventListener listener = listeners[index];
               this.callProfiled(listener, event);
               if (event.isCancelled()) {
                  break;
               }
            }
         }
      }
      return event;
   }
   private void callUnprofiled(EventListener listener, Object event) {
      try {
         listener.call(event);
      } catch (RuntimeException | Error failure) {
         RuntimeDiagnostics.record(
            RuntimeDiagnostics.Area.EVENT,
            listener.diagnosticName() + " <- " + event.getClass().getSimpleName(),
            failure
         );
         throw failure;
      }
   }

   private void callProfiled(EventListener listener, Object event) {
      EventProfiler.CallbackHandle handle = listener.profileHandle();
      if (handle == null) {
         this.callUnprofiled(listener, event);
         return;
      }
      long startedAt = this.profiler.nanoTime();
      Throwable callbackFailure = null;
      try {
         listener.call(event);
      } catch (RuntimeException | Error failure) {
         callbackFailure = failure;
         RuntimeDiagnostics.record(
            RuntimeDiagnostics.Area.EVENT,
            listener.diagnosticName() + " <- " + event.getClass().getSimpleName(),
            failure
         );
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

