package dev.schizo.client.gui.screens;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Screen-local role registry; never restores a stale widget instance. */
final class StudioResizeFocus<T> {
   private final Map<String,T> widgets = new LinkedHashMap<>();

   void register(String role, T widget) {
      widgets.put(Objects.requireNonNull(role), Objects.requireNonNull(widget));
   }

   void clear() { widgets.clear(); }

   String roleOf(Object focused) {
      if (focused == null) return null;
      for (var entry : widgets.entrySet()) {
         if (entry.getValue() == focused) return entry.getKey();
      }
      return null;
   }

   T resolve(String role, Predicate<T> eligible) {
      if (role == null) return null;
      T widget = widgets.get(role);
      return widget != null && eligible.test(widget) ? widget : null;
   }
}
