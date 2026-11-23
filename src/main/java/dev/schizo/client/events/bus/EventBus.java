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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus implements EventDispatcher {
   private final Map<Object, List<EventListener>> instanceListenerCache =
      Collections.synchronizedMap(new IdentityHashMap<>());
   private final Map<Class<?>, List<EventListener>> staticListenerCache = new ConcurrentHashMap<>();
   private final Map<Class<?>, EventListener[]> listenersByEventClass = new ConcurrentHashMap<>();

   @Override
   public void subscribe(Object subscriber) {
      this.subscribeListeners(this.getOrCreateListeners(subscriber.getClass(), subscriber), false);
   }

   @Override
   public void unsubscribe(Object subscriber) {
      this.unsubscribeListeners(this.getOrCreateListeners(subscriber.getClass(), subscriber), false);
   }

   @Override
   public void subscribeStatic(Class<?> listenerClass) {
      this.subscribeListeners(this.getOrCreateListeners(listenerClass, null), true);
   }

   @Override
   public void unsubscribeStatic(Class<?> listenerClass) {
      this.unsubscribeListeners(this.getOrCreateListeners(listenerClass, null), true);
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
      for (Class<?> type = listenerClass; type != null; type = type.getSuperclass()) {
         for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)
               && method.getReturnType() == void.class
               && method.getParameterCount() == 1
               && !method.getParameterTypes()[0].isPrimitive()) {
               listeners.add(new MethodEventListener(type, subscriber, method));
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
         int removalIndex = -1;
         for (int index = 0; index < current.length; index++) {
            if (current[index].equals(listener)) {
               removalIndex = index;
               break;
            }
         }
         if (removalIndex < 0) {
            return current;
         }
         if (current.length == 1) {
            return null;
         }

         EventListener[] updated = new EventListener[current.length - 1];
         System.arraycopy(current, 0, updated, 0, removalIndex);
         System.arraycopy(current, removalIndex + 1, updated, removalIndex,
            current.length - removalIndex - 1);
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
            listeners[index].call(event);
         }
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
            listener.call(event);
            if (event.isCancelled()) {
               break;
            }
         }
      }
      return event;
   }
}
