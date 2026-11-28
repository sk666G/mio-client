/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import com.google.gson.JsonElement;
import dev.schizo.client.managers.PresetRegistry;
import dev.schizo.client.settings.options.PresetCategory;
import java.io.IOException;

public sealed interface PresetUndoAction {
   void undo() throws IOException;

   @FunctionalInterface
   interface Guard {
      void verify() throws IOException;
   }

   /** Capture the state produced by a successful operation before it enters history. */
   default Guard guardAfterSuccess() {
      return switch(this) {
         case Load load -> {
            JsonElement expected=load.category().toJson().deepCopy();
            yield () -> {
               if(!expected.equals(load.category().toJson())) throw new IOException("Live configuration changed since preset load");
            };
         }
         case Save save -> reviewedGuard(save.registry(),save.name());
         case Delete delete -> () -> delete.registry().requireUndoTargetAbsent(delete.preset().getName());
         case Rename rename -> {
            Guard target=reviewedGuard(rename.registry(),rename.newName());
            yield () -> {
               target.verify();
               rename.registry().requireUndoTargetAbsent(rename.oldName());
            };
         }
      };
   }

   private static Guard reviewedGuard(PresetRegistry registry,String name) {
      PresetData expected=registry.getPreset(name);
      return () -> {
         if(expected==null || !registry.matchesReviewedPreset(expected))
            throw new IOException("Preset changed since the recorded operation");
      };
   }

   static PresetUndoAction beforeLoad(PresetCategory category, String name) {
      if (category == null) {
         throw new IllegalArgumentException("Preset category is required");
      }
      return new Load(name, category, category.toJson());
   }

   static PresetUndoAction beforeSave(PresetRegistry registry, String name) {
      return new Save(registry, name, registry.getPreset(name));
   }

   static PresetUndoAction beforeDelete(PresetRegistry registry, String name) {
      PresetData preset = registry.getPreset(name);
      return preset == null ? null : new Delete(registry, preset);
   }

   static PresetUndoAction beforeRename(PresetRegistry registry, String oldName, String newName) {
      return new Rename(registry, oldName, newName, registry.getPreset(newName), registry.getPreset(oldName));
   }

   record Load(String name, PresetCategory category, JsonElement snapshot) implements PresetUndoAction {
      @Override
      public void undo() {
         JsonStateTransaction.apply(this.category::toJson,this.category::fromJson,this.snapshot);
      }
   }

   record Save(PresetRegistry registry, String name, PresetData previous) implements PresetUndoAction {
      @Override
      public void undo() throws IOException {
         if (this.previous == null) {
            PresetData current=this.registry.getPreset(this.name);
            if(current==null) throw new IOException("Preset no longer exists");
            this.registry.deletePresetForUndo(current.getName());
         } else {
            PresetData current=this.registry.getPreset(this.name);
            if(current==null) throw new IOException("Preset no longer exists");
            this.registry.restorePreset(this.previous,current.getName());
         }
      }
   }

   record Delete(PresetRegistry registry, PresetData preset) implements PresetUndoAction {
      @Override
      public void undo() throws IOException {
         this.registry.restoreDeletedPreset(this.preset);
      }
   }

   record Rename(PresetRegistry registry, String oldName, String newName, PresetData previousTarget, PresetData source) implements PresetUndoAction {
      @Override
      public void undo() throws IOException {
         PresetData current=this.registry.getPreset(this.newName);
         if(current==null) throw new IOException("Renamed preset no longer exists");
         this.registry.renamePresetForUndo(current.getName(),this.oldName,this.previousTarget,this.source);
      }
   }
}
