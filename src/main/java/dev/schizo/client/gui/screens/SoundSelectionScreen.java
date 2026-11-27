/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.components.ButtonComponent;
import dev.schizo.client.render.FontRenderer;
import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.managers.SoundManager;
import dev.schizo.client.config.ConfigPathGuard;
import dev.schizo.client.utils.collections.NamedIdentifier;
import dev.schizo.client.settings.Setting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.screens.Screen;

public class SoundSelectionScreen extends ClientScreen {
   private final Setting<dev.schizo.client.utils.collections.NamedIdentifier> selectedSetting;
   private final Screen parentScreen;

   public SoundSelectionScreen(Setting<dev.schizo.client.utils.collections.NamedIdentifier> selectedSetting, Screen parentScreen) {
      this.selectedSetting = selectedSetting;
      this.parentScreen = parentScreen;
      this.rebuildSoundPanels();
   }

   @Override
   public void closeWithTransition() {
      if (this.parentScreen instanceof ClientScreen clientScreen) {
         clientScreen.open();
      } else {
         ClientAccess.schizoMinecraft.setScreen(
            this.parentScreen
         );
      }
   }

   @Override
   public void onFilesDrop(List paths) {
      Path customSoundsDirectory = ConfigurationManager.SOUNDS_DIRECTORY.resolve("custom");
      int imported = 0;
      try {
         this.prepareCustomSoundsDirectory(customSoundsDirectory);
      } catch (IOException directoryFailure) {
         ClientAccess.logger.warn("Could not prepare custom sound directory", directoryFailure);
         return;
      }

      for (Object pathElement : paths) {
         Path droppedFile = (Path)pathElement;
         if (!droppedFile.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
            continue;
         }

         try {
            if (Files.isSymbolicLink(droppedFile)
               || !Files.isRegularFile(droppedFile, LinkOption.NOFOLLOW_LINKS)) {
               throw new IOException("Dropped sound is not a regular file");
            }
            long sourceSize = Files.size(droppedFile);
            if (!isSupportedImportSize(sourceSize)) {
               throw new IOException("Dropped sound exceeds the supported size limit");
            }
            Files.copy(droppedFile, customSoundsDirectory.resolve(droppedFile.getFileName()));
            imported++;
         } catch (IOException exception) {
            ClientAccess.logger.warn("Could not import custom sound {}", droppedFile, exception);
         }
      }

      if (imported > 0) {
         ClientContext.soundManager.reload();
         this.rebuildSoundPanels();
      }
   }

   private void prepareCustomSoundsDirectory(Path directory) throws IOException {
      ConfigPathGuard.ensureDirectory(ConfigurationManager.ROOT_DIRECTORY, directory);
   }

   static boolean isSupportedImportSize(long bytes) {
      return bytes >= 0L && bytes <= SoundManager.MAX_CUSTOM_SOUND_BYTES;
   }

   private void rebuildSoundPanels() {
      this.getPanels()
         .clear();
      synchronized (ClientContext.soundManager
         .getSoundIds()) {
         AtomicInteger panelIndex = new AtomicInteger(0);
         ClientContext.soundManager
            .getSoundIds()
            .stream()
            .map((java.util.function.Function<dev.schizo.client.utils.collections.NamedIdentifier, java.lang.String>)NamedIdentifier::getCategory)
            .filter((java.util.function.Predicate<java.lang.String>)(category -> !category.isEmpty()))
            .distinct()
            .forEach(
               category -> {
                  Panel categoryPanel = new Panel(
                     FontRenderer.capitalize(category)
                  );
                  this.getPanels()
                     .add(categoryPanel);
                  categoryPanel.setX(
                     categoryPanel.getX()
                        + (
                              categoryPanel.getWidth()
                                 + 3
                           )
                           * panelIndex.getAndIncrement()
                  );
               }
            );

         for (Object soundElement : ClientContext.soundManager
            .getSoundIds()) {
            NamedIdentifier soundId = (NamedIdentifier)soundElement;
            for (Panel panel : this.getPanels()) {
               if (panel.getName().equalsIgnoreCase(soundId.getCategory())
                  && !soundId.equals(BuiltinSound.HORROR.getIdentifier()))
                {
                  panel.getChildren()
                     .add(new SoundOptionButton(this, panel, soundId));
                  break;
               }
            }
         }

         for (Panel panel : this.getPanels()) {
            panel.getChildren()
               .sort(
                  Comparator.comparing(
                     child -> child instanceof ButtonComponent button
                        ? button.getLabel()
                        : ""
                  )
               );
         }
      }
   }

   public Setting<dev.schizo.client.utils.collections.NamedIdentifier> getSelectedSetting() {
      return this.selectedSetting;
   }

}
