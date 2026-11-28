/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import dev.schizo.client.modules.Module;
import dev.schizo.client.settings.Setting;

import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;

public class ExperimentalFeatures {
   // Cache only after Fabric has a game provider. An early query must fail closed
   // without permanently poisoning class initialization or later startup.
   private static volatile Boolean experimentalFeaturesAllowed;

   public static boolean isExperimentalDisabled() {
      Boolean allowed = experimentalFeaturesAllowed;
      if (allowed != null) return !allowed;
      String[] arguments;
      try {
         arguments = FabricLoader.getInstance().getLaunchArguments(true);
      } catch (IllegalStateException providerUnavailable) {
         return true;
      }
      boolean resolved = false;
      for (String argument : arguments) {
         if ("--schizo-allow-experimental".equals(argument)) {
            resolved = true;
            break;
         }
      }
      resolved |= "true".equalsIgnoreCase(System.getProperty("schizo.allowExperimental"));
      experimentalFeaturesAllowed = resolved;
      return !resolved;
   }

   public static boolean isFieldHidden(Field field) {
      return isExperimentalDisabled() && field.isAnnotationPresent(Experimental.class);
   }

   public static boolean isEnumConstantHidden(Enum enumConstant) {
      try {
         if (!isExperimentalDisabled()) {
            return false;
         }
         Class declaringClass = enumConstant.getDeclaringClass();
         String constantName = enumConstant.name();
         return declaringClass.getDeclaredField(constantName).isAnnotationPresent(Experimental.class);
      } catch (NoSuchFieldException missingField) {
         return false;
      }
   }

   public static boolean isClassHidden(Class type) {
      return isExperimentalDisabled() && type.isAnnotationPresent(Experimental.class);
   }

   public static void unregisterHiddenSettings(Module module) {
      Class moduleType = module.getClass();

      for (Field field : moduleType.getDeclaredFields()) {
         if (!isFieldHidden(field)) {
            continue;
         }
         field.setAccessible(true);
         try {
            Setting<?> setting = (Setting<?>)field.get(module);
            module.unregister(setting);
         } catch (IllegalAccessException accessFailure) {
            dev.schizo.client.ClientAccess.logger.warn("Could not remove hidden experimental setting {} from {}", field.getName(), module.getName(), accessFailure);
         }
      }
   }
}
