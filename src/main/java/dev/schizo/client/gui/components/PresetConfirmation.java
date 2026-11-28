package dev.schizo.client.gui.components;

import dev.schizo.client.config.PresetData;
import dev.schizo.client.settings.options.PresetCategory;

/** Two explicit clicks on the same action and immutable reviewed target. */
final class PresetConfirmation {
   private record Target(String action,PresetCategory category,PresetData preset) {}
   private Target pending;

   boolean armed(String action) {
      return pending!=null && pending.action().equals(action);
   }

   boolean confirm(String action,PresetCategory category,PresetData preset) {
      var next=new Target(action,category,preset);
      if(next.equals(pending)) {
         clear();
         return true;
      }
      pending=next;
      return false;
   }

   void clear() {pending=null;}
}
