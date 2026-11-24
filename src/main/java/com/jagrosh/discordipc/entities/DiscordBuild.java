/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Downstream changes from the Open Zio and Schizo source snapshots are included.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jagrosh.discordipc.entities;

public enum DiscordBuild {
   CANARY("//canary.discord.com/api"),
   PTB("//ptb.discord.com/api"),
   STABLE("//discord.com/api"),
   ANY;

   private final String endpoint;

   DiscordBuild(String endpoint) {
      this.endpoint = endpoint;
   }

   DiscordBuild() {
      this(null);
   }

   public static DiscordBuild from(int ordinal) {
      for (DiscordBuild build : values()) {
         if (build.ordinal() == ordinal) {
            return build;
         }
      }

      return ANY;
   }

   public static DiscordBuild from(String endpoint) {
      for (DiscordBuild build : values()) {
         if (build.endpoint != null && build.endpoint.equals(endpoint)) {
            return build;
         }
      }

      return ANY;
   }
}
