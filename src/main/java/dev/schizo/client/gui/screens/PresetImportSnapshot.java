package dev.schizo.client.gui.screens;

import com.google.gson.JsonObject;
import java.util.Objects;

/** The exact reviewed JSON, isolated from both the reader and the save path. */
final class PresetImportSnapshot {
   private final JsonObject reviewed;

   PresetImportSnapshot(JsonObject reviewed) {
      this.reviewed=Objects.requireNonNull(reviewed).deepCopy();
   }

   boolean matches(JsonObject current) {return reviewed.equals(current);}
   JsonObject copy() {return reviewed.deepCopy();}
}
