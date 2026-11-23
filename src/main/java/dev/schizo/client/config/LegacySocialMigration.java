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
import java.nio.file.Path;
import java.util.Iterator;

public final class LegacySocialMigration implements ClientAccess {
   private LegacySocialMigration() {
   }

   public static void migrate() throws IOException {
      Path friendsPath = ConfigurationManager.ROOT_DIRECTORY.resolve("friends.json");
      Path socialsPath = ConfigurationManager.ROOT_DIRECTORY.resolve("socials.json");
      if (!friendsPath.toFile().exists()) {
         return;
      }
      if (socialsPath.toFile().exists()) {
         return;
      }

      String friendsText = FileUtils.readUtf8(friendsPath);
      JsonObject friendsRoot = JsonParser.parseString(friendsText).getAsJsonObject();
      JsonArray friends = friendsRoot.getAsJsonArray("friends");
      Iterator<JsonElement> friendIterator = friends.iterator();
      while (friendIterator.hasNext()) {
         JsonElement friend = friendIterator.next();
         SocialRegistry registry = ClientContext.socialRegistry;
         String name = friend.getAsString();
         registry.addFriend(name);
      }

      Gson gson = ClientAccess.gson;
      SocialRegistry registry = ClientContext.socialRegistry;
      JsonElement socials = registry.toJson();
      FileUtils.writeUtf8Atomically(socialsPath, gson.toJson(socials));
      java.nio.file.Files.delete(friendsPath);
   }
}
