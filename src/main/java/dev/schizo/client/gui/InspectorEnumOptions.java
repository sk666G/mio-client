package dev.schizo.client.gui;

import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.utils.ExperimentalFeatures;
import java.util.ArrayList;
import java.util.List;

/** Strict canonical matching: never invoke the enum codec's first-entry fallback for invalid input. */
public final class InspectorEnumOptions {
   private InspectorEnumOptions() {}

   public static List<String> selectable(Setting<?> setting) {
      if(!(setting instanceof EnumSetting<?>) || !(setting.getValue() instanceof Enum<?> value)) return List.of();
      var names=new ArrayList<String>();
      for(Enum<?> candidate:value.getDeclaringClass().getEnumConstants()) {
         if(candidate instanceof NamedEntry named && !ExperimentalFeatures.isEnumConstantHidden(candidate)) names.add(named.getName());
      }
      return List.copyOf(names);
   }

   public static String canonical(List<String> options,String input) {
      if(input==null || input.isBlank()) return null;
      String canonical=null;
      for(String option:options) {
         if(option.equalsIgnoreCase(input)) {
            if(canonical!=null) return null;
            canonical=option;
         }
      }
      return canonical;
   }
}
