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
import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.gui.components.ActionButtonComponent;
import dev.schizo.client.gui.components.ButtonComponent;
import dev.schizo.client.config.EnumJsonConverter;
import dev.schizo.client.config.ConfigFileReader;
import dev.schizo.client.config.PresetData;
import dev.schizo.client.settings.options.PresetCategory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

public final class PresetImportScreen extends ClientScreen {
   private final PresetCategory detectedCategory;
   private final Panel importPanel;
   private final Screen returnScreen;
   private final Path sourceFile;

   public PresetImportScreen(Screen previousScreen, Path sourcePath) {
      this.returnScreen = previousScreen;
      this.detectedCategory = this.readPresetCategory(
         sourcePath
      );
      this.importPanel = new Panel(
         this.getCategoryTitle(
            this.detectedCategory
         )
      );
      this.sourceFile = sourcePath;
      this.importPanel
         .getChildren()
         .add(
            new ButtonComponent(
               this.importPanel,
               "Preset: " + this.getPresetName(sourcePath)
            )
         );
      if (this.detectedCategory != null) {
         this.importPanel
            .registerChild(
               new ActionButtonComponent(
                  this.importPanel,
                  "Import",
                  () -> this.importPreset(
                     sourcePath, this.detectedCategory
                  )
               )
            );
      } else {
         for (PresetCategory category : PresetCategory.values()) {
            this.importPanel
               .registerChild(
                  new ActionButtonComponent(
                     this.importPanel,
                     EnumJsonConverter.getName(category),
                     () -> this.importPreset(sourcePath, category)
                  )
               );
         }
      }

      this.importPanel
         .registerChild(
            new ActionButtonComponent(
               this.importPanel,
               "Cancel",
               this::closeWithTransition
            )
         );
      this.getPanels()
         .add(this.importPanel);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key()
            == 257
         && this.detectedCategory != null) {
         this.importPreset(
            this.sourceFile,
            this.detectedCategory
         );
      }

      return super.keyPressed(event);
   }

   @Override
   public void closeWithTransition() {
      ClientAccess.schizoMinecraft.setScreen(
         this.returnScreen
      );
   }

   private String getPresetName(Path sourcePath) {
      return sourcePath.toFile().getName().replace(".json", "");
   }

   private void importPreset(Path sourcePath, PresetCategory targetCategory) {
      String fileName = sourcePath.toFile().getName();

      try {
         this.readPresetObject(sourcePath);
         Files.copy(sourcePath, targetCategory.getDirectory().resolve(fileName));
      } catch (IOException | RuntimeException copyFailure) {
         ClientAccess.logger.warn("Could not import dropped preset {}", sourcePath, copyFailure);
         return;
      }

      ClientContext.configurationManager
         .getPresetRegistry(targetCategory)
         .loadPresets();
      ClientAccess.schizoMinecraft.setScreen(
         this.returnScreen
      );
   }

   private JsonObject readPresetObject(Path sourcePath) throws IOException {
      if (Files.isSymbolicLink(sourcePath)
         || !Files.isRegularFile(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Dropped preset is not a regular file");
      }
      String jsonText = ConfigFileReader.readUtf8(sourcePath, PresetData.MAX_PRESET_BYTES);
      JsonElement parsedJson;
      try {
         parsedJson = JsonParser.parseString(jsonText);
      } catch (RuntimeException malformed) {
         throw new IOException("Dropped preset is not valid JSON", malformed);
      }
      if (!parsedJson.isJsonObject()) {
         throw new IOException("Dropped preset must contain a JSON object");
      }
      return parsedJson.getAsJsonObject();
   }

   private PresetCategory readPresetCategory(Path sourcePath) {
      try {
         JsonObject jsonObject = this.readPresetObject(sourcePath);
         if (!jsonObject.has("category")) {
            return null;
         }

         String categoryName = jsonObject.get("category").getAsString();
         return PresetCategory.findByName(categoryName);
      } catch (IOException | RuntimeException readFailure) {
         ClientAccess.logger.debug("Could not infer dropped preset category from {}", sourcePath, readFailure);
         return null;
      }
   }

   public static boolean openForDroppedFiles(Screen previousScreen, List droppedPaths) {
      ArrayList jsonPaths = new ArrayList();
      droppedPaths.stream().filter(path -> path.toString().endsWith(".json")).forEach(jsonPaths::add);
      if (jsonPaths.size() != 1) {
         return false;
      }

      ClientAccess.schizoMinecraft.setScreen(
         new PresetImportScreen(previousScreen, (Path)jsonPaths.getFirst())
      );
      return true;
   }

   private String getCategoryTitle(PresetCategory category) {
      return category == null ? "Unknown" : category.getName();
   }

}
