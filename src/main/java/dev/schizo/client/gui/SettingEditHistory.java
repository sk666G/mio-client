package dev.schizo.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Predicate;

/** Bounded history for scalar values without registered change callbacks. */
public final class SettingEditHistory {
   public static final int LIMIT = 64;
   public enum Result { APPLIED, UNCHANGED, STALE, UNSUPPORTED, EMPTY, REJECTED }
   private record Edit(Setting<?> setting, JsonElement before, JsonElement after) {}
   private final Deque<Edit> undo = new ArrayDeque<>();
   private final Deque<Edit> redo = new ArrayDeque<>();
   private final Predicate<Setting<?>> allowed;

   public SettingEditHistory() { this(setting -> true); }

   public SettingEditHistory(Predicate<Setting<?>> allowed) {
      this.allowed = Objects.requireNonNull(allowed);
   }

   public Setting<?> undoTarget() { return undo.isEmpty() ? null : undo.peekLast().setting(); }
   public Setting<?> redoTarget() { return redo.isEmpty() ? null : redo.peekLast().setting(); }

   public static boolean supports(Setting<?> setting) {
      return (setting instanceof BooleanSetting || setting instanceof NumberSetting<?> || setting instanceof EnumSetting<?>)
         && !setting.isGroupOnly() && !setting.isExcludedFromConfig()
         && !setting.hasChangeCallback();
   }

   public Result apply(Setting<?> setting, JsonElement expected, JsonElement desired) {
      if (!supports(setting) || !allowed.test(setting)) return Result.UNSUPPORTED;
      try {
         if (!setting.isVisible() || !setting.toJson().equals(expected)) return Result.STALE;
         if (!desired.isJsonPrimitive()) return Result.REJECTED;
         if (setting instanceof BooleanSetting && !desired.getAsJsonPrimitive().isBoolean()) return Result.REJECTED;
         if (setting instanceof NumberSetting<?> && !desired.getAsJsonPrimitive().isNumber()) return Result.REJECTED;
         if (setting instanceof EnumSetting<?>) {
            if(!desired.getAsJsonPrimitive().isString()) return Result.REJECTED;
            String canonical=InspectorEnumOptions.canonical(InspectorEnumOptions.selectable(setting),desired.getAsString());
            if(canonical==null) return Result.REJECTED;
            desired=new JsonPrimitive(canonical);
         }
         JsonElement before = setting.toJson().deepCopy();
         setting.fromJson(desired.deepCopy());
         JsonElement after = setting.toJson().deepCopy();
         if (before.equals(after)) return Result.UNCHANGED;
         undo.addLast(new Edit(setting, before, after));
         while (undo.size() > LIMIT) undo.removeFirst();
         redo.clear();
         return Result.APPLIED;
      } catch (RuntimeException failure) {
         return Result.REJECTED;
      }
   }

   public Result undo() { return move(undo, redo, true); }
   public Result redo() { return move(redo, undo, false); }
   public boolean canUndo() { return !undo.isEmpty(); }
   public boolean canRedo() { return !redo.isEmpty(); }

   private Result move(Deque<Edit> source, Deque<Edit> destination, boolean backward) {
      Edit edit = source.peekLast();
      if (edit == null) return Result.EMPTY;
      if (!supports(edit.setting()) || !allowed.test(edit.setting())) return Result.UNSUPPORTED;
      JsonElement expected = backward ? edit.after() : edit.before();
      JsonElement target = backward ? edit.before() : edit.after();
      try {
         if (!edit.setting().isVisible() || !edit.setting().toJson().equals(expected)) return Result.STALE;
         // A changed numeric bound must not silently clamp an undo operation.
         if (edit.setting() instanceof NumberSetting<?> number && number.isNumericBoundsEnabled()) {
            double value = target.getAsDouble();
            if (value < number.getMinimumValue().doubleValue() || value > number.getMaximumValue().doubleValue()) return Result.STALE;
         }
         if(edit.setting() instanceof EnumSetting<?>) {
            String canonical=InspectorEnumOptions.canonical(InspectorEnumOptions.selectable(edit.setting()),target.getAsString());
            if(canonical==null || !canonical.equals(target.getAsString())) return Result.STALE;
         }
         edit.setting().fromJson(target.deepCopy());
         if (!edit.setting().toJson().equals(target)) return Result.REJECTED;
         source.removeLast();
         destination.addLast(edit);
         return Result.APPLIED;
      } catch (RuntimeException failure) {
         return Result.REJECTED;
      }
   }

   public void clear() { undo.clear(); redo.clear(); }
}
