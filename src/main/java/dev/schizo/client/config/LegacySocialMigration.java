/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import dev.schizo.client.managers.ConfigurationManager;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.utils.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LegacySocialMigration implements ClientAccess {
   private LegacySocialMigration() {
   }

   public static void migrate() throws IOException {
      Path friendsPath = ConfigurationManager.ROOT_DIRECTORY.resolve("friends.json");
      Path socialsPath = ConfigurationManager.ROOT_DIRECTORY.resolve("socials.json");
      if (!Files.exists(friendsPath, LinkOption.NOFOLLOW_LINKS)) {
         return;
      }
      if (Files.isSymbolicLink(friendsPath)
         || !Files.isRegularFile(friendsPath, LinkOption.NOFOLLOW_LINKS)) {
         ClientAccess.logger.warn("Skipping legacy friends migration because {} is not a regular file", friendsPath);
         return;
      }
      if (Files.exists(socialsPath, LinkOption.NOFOLLOW_LINKS)) {
         if (Files.isSymbolicLink(socialsPath)
            || !Files.isRegularFile(socialsPath, LinkOption.NOFOLLOW_LINKS)) {
            ClientAccess.logger.warn(
               "Skipping legacy friends migration because destination {} is not a regular file",
               socialsPath
            );
         }
         return;
      }

      String friendsText = ConfigFileReader.readUtf8(
         friendsPath,
         (int)ConfigurationBackupManager.MAX_CONFIG_BYTES
      );
      List<String> friendNames = parseFriends(friendsText);
      SocialRegistry registry = ClientContext.socialRegistry;
      for (String name : friendNames) {
         registry.addFriend(name);
      }

      Gson gson = ClientAccess.gson;
      JsonElement socials = registry.toJson();
      FileUtils.writeUtf8Atomically(socialsPath, gson.toJson(socials));
      Files.delete(friendsPath);
   }

   static List<String> parseFriends(String serialized) {
      JsonElement parsed = JsonParser.parseString(serialized);
      if (!parsed.isJsonObject()) {
         throw new IllegalArgumentException("Legacy friends configuration must be an object");
      }
      JsonElement friendsElement = parsed.getAsJsonObject().get("friends");
      if (friendsElement == null || !friendsElement.isJsonArray()) {
         throw new IllegalArgumentException("Legacy friends configuration is missing its friends array");
      }

      JsonArray friends = friendsElement.getAsJsonArray();
      List<String> friendNames = new ArrayList<>(friends.size());
      for (int index = 0; index < friends.size(); index++) {
         JsonElement friend = friends.get(index);
         if (!friend.isJsonPrimitive() || !friend.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Legacy friends entry " + index + " is not a string");
         }
         String name = friend.getAsString();
         if (name.isBlank()) {
            throw new IllegalArgumentException("Legacy friends entry " + index + " is blank");
         }
         friendNames.add(name);
      }
      return List.copyOf(friendNames);
   }
}
