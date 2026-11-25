/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.config.EnumJsonConverter;
import dev.schizo.client.modules.misc.SocialEntry;
import dev.schizo.client.utils.entity.PlayerState;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.Registry;
import dev.schizo.client.modules.client.ColorsModule;
import dev.schizo.client.settings.JsonConfigurable;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.options.SocialRole;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.awt.Color;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class SocialRegistry extends Registry<SocialEntry> implements ClientAccess, JsonConfigurable {
   private static final ColorsModule colorsModule;
   private static final EnumJsonConverter<SocialRole> roleConverter;

   static {
      colorsModule = ClientContext.getModule(ColorsModule.class);
      roleConverter = new EnumJsonConverter<>(SocialRole.class);
   }

   public boolean isFriendName(String name) {
      return this.getRole(name) == SocialRole.FRIEND;
   }

   public void addFriend(String name) {
      this.removeSocial(name);
      this.registerSocial(new SocialEntry(name, SocialRole.FRIEND));
   }

   public Color getEnemyColor() {
      Setting<Color> colorSetting = colorsModule.enemyColorSetting;
      return colorSetting.getValue();
   }

   public Color getFriendColor() {
      Setting<Color> colorSetting = colorsModule.friendColorSetting;
      return colorSetting.getValue();
   }

   public boolean isFriendEntity(Entity entity) {
      if (entity instanceof Player player) {
         return this.isFriendPlayer(player);
      } else {
         return false;
      }
   }

   @Override
   public boolean register(Object entry) {
      return this.registerSocial((SocialEntry)entry);
   }

   public boolean registerSocial(SocialEntry entry) {
      if (entry == null || entry.getName() == null) {
         return false;
      }

      boolean duplicateName = this.entries.stream().anyMatch(existing ->
         existing != null
            && existing.getName() != null
            && existing.getName().equalsIgnoreCase(entry.getName())
      );
      if (duplicateName || !super.registerEntry(entry)) {
         return false;
      }

      this.updatePlayerRole(entry.getName(), entry.role());
      return true;
   }

   @Override
   public void fromJson(JsonElement json) {
      JsonObject root = json.getAsJsonObject();

      if (root.has("socials")) {
         root = json.getAsJsonObject();

         JsonArray socials = root.getAsJsonArray("socials");
         Iterator socialsIterator = socials.iterator();

         while (socialsIterator.hasNext()) {
            JsonElement entryElement = (JsonElement)socialsIterator.next();
            JsonObject entryObject = entryElement.getAsJsonObject();

            if (entryObject.has("name")) {
               if (entryObject.has("role")) {
                  JsonElement nameElement = entryObject.get("name");
                  String name = nameElement.getAsString();
                  EnumJsonConverter<SocialRole> capturedConverter = roleConverter;

                  JsonElement roleElement = entryObject.get("role");
                  SocialRole role = (SocialRole)capturedConverter.fromJson(
                     roleElement
                  );
                  SocialEntry allocatedSocial = new SocialEntry(name, role);

                  this.registerSocial(allocatedSocial);
               }
            }
         }
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      JsonArray socials = new JsonArray();
      Iterator<SocialEntry> socialsIterator = this.entries.iterator();

      while (socialsIterator.hasNext()) {
         SocialEntry social = socialsIterator.next();
         JsonObject entryObject = new JsonObject();

         String name = social.getName();
         entryObject.addProperty("name", name);

         EnumJsonConverter<SocialRole> capturedConverter = roleConverter;
         SocialRole capturedRole = social.role();

         JsonElement roleElement = capturedConverter.toJson(capturedRole);
         entryObject.add("role", roleElement);
         socials.add(entryObject);
      }

      root.add("socials", socials);
      return root;
   }

   public void addEnemy(String name) {
      this.removeSocial(name);
      this.registerSocial(new SocialEntry(name, SocialRole.ENEMY));
   }

   public boolean isEnemyName(String name) {
      return this.getRole(name) == SocialRole.ENEMY;
   }

   @Override
   public String getConfigName() {
      return "socials.json";
   }

   public SocialRole getRole(String name) {
      Optional<SocialRole> mappedRole = this.findFirst((Predicate<SocialEntry>)entry -> {
         String capturedName = entry.getName();
         return capturedName.equalsIgnoreCase(name);
      }).map((java.util.function.Function<SocialEntry, SocialRole>)SocialEntry::role);
      return mappedRole.orElse(null);
   }

   public boolean isEnemyEntity(Entity entity) {
      if (entity instanceof Player player) {
         return this.isEnemyPlayer(player);
      } else {
         return false;
      }
   }

   public Color getColor(String name, Color defaultColor) {
      if (this.isFriendName(name)) {
         return this.getFriendColor();
      } else if (this.isEnemyName(name)) {
         return this.getEnemyColor();
      } else {
         return defaultColor;
      }
   }

   private void updatePlayerRole(String name, SocialRole role) {
      if (!this.isWorldUnavailable()) {
         ClientLevel level = ClientAccess.schizoMinecraft.level;
         Iterator playersIterator = level.players().iterator();

         while (playersIterator.hasNext()) {
            AbstractClientPlayer player = (AbstractClientPlayer)playersIterator.next();
            GameProfile profile = player.getGameProfile();
            String capturedPlayerName = profile.name();
            if (capturedPlayerName.equalsIgnoreCase(name) && player instanceof PlayerState roleHolder) {
               roleHolder.schizo$setRole(role);
               return;
            }
         }
      }
   }

   @Override
   public boolean registerEntry(NamedEntry entry) {
      return this.registerSocial((SocialEntry)entry);
   }

   public boolean removeSocial(String name) {
      this.updatePlayerRole(name, null);
      return this.entries.removeIf((Predicate<SocialEntry>)entry -> {
            String capturedName = entry.getName();
            return capturedName.equalsIgnoreCase(name);
         }
      );
   }

   public boolean isEnemyPlayer(Player player) {
      PlayerState roleHolder = (PlayerState)player;
      return roleHolder.schizo$getRole() == SocialRole.ENEMY;
   }

   public boolean isFriendPlayer(Player player) {
      PlayerState roleHolder = (PlayerState)player;
      return roleHolder.schizo$getRole() == SocialRole.FRIEND;
   }

}
