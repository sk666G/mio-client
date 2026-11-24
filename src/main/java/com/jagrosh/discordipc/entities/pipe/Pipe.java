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

package com.jagrosh.discordipc.entities.pipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Pipe {
   private static final Logger LOGGER = LoggerFactory.getLogger(Pipe.class);
   private static final int VERSION = 1;
   private static final String[] unixPaths = new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
   private static final String[] unixFolderPaths = new String[]{"/snap.discord", "/app/com.discordapp.Discord"};
   final IPCClient ipcClient;
   private final HashMap callbacks;
   volatile PipeStatus status = PipeStatus.CONNECTING;
   IPCListener listener;
   private DiscordBuild build;
   private User currentUser;

   Pipe(IPCClient client, HashMap callbacks) {
      this.ipcClient = client;
      this.callbacks = callbacks;
   }

   public static Pipe openPipe(IPCClient client, long clientId, HashMap callbacks, DiscordBuild... preferredBuilds) throws NoDiscordClientException {
      if (preferredBuilds == null || preferredBuilds.length == 0) {
         preferredBuilds = new DiscordBuild[]{DiscordBuild.ANY};
      }

      Pipe pipe = null;
      Pipe[] candidates = new Pipe[DiscordBuild.values().length];

      for (int index = 0; index < 10; index++) {
         try {
            client.checkConnectActive();
         } catch (CancellationException cancelled) {
            abortCandidates(candidates, client);
            throw cancelled;
         }
         String location = getPipeLocation(index);
         if (client.isDebugMode()) {
            client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Searching for IPC Pipe: \"%s\"", location));
         }

         try {
            File file = new File(location.replace("\\", "\\\\"));
            if (file.exists()) {
               if (client.isDebugMode()) {
                  client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Found valid file, attempting connection to IPC: \"%s\"", location));
               }

               pipe = createPipe(client, callbacks, file);
               client.trackConnectingPipe(pipe);
               JsonObject handshake = new JsonObject();
               handshake.addProperty("v", 1);
               handshake.addProperty("client_id", Long.toString(clientId));
               pipe.send(Packet.OpCode.HANDSHAKE, handshake);
               Packet response = pipe.read();
               client.checkConnectActive();
               JsonObject json = response.getJson();
               JsonObject data = json.getAsJsonObject("data");
               JsonObject userData = data.getAsJsonObject("user");
               pipe.build = DiscordBuild.from(data.getAsJsonObject("config").get("api_endpoint").getAsString());
               pipe.currentUser = new User(
                  userData.getAsJsonPrimitive("username").getAsString(),
                  userData.has("global_name") && userData.get("global_name").isJsonPrimitive() ? userData.getAsJsonPrimitive("global_name").getAsString() : null,
                  userData.has("discriminator") && userData.get("discriminator").isJsonPrimitive() ? userData.getAsJsonPrimitive("discriminator").getAsString() : "0",
                  Long.parseLong(userData.getAsJsonPrimitive("id").getAsString()),
                  userData.has("avatar") && userData.get("avatar").isJsonPrimitive() ? userData.getAsJsonPrimitive("avatar").getAsString() : null
               );
               if (client.isDebugMode()) {
                  client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Found a valid client (%s) with packet: %s", pipe.build.name(), response));
                  client.getCurrentLogger(LOGGER)
                     .info(String.format("[DEBUG] Found a valid user (%s) with id: %s", pipe.currentUser.getName(), pipe.currentUser.getId()));
               }

               if (pipe.build == preferredBuilds[0] || DiscordBuild.ANY == preferredBuilds[0]) {
                  if (client.isDebugMode()) {
                     client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Found preferred client: %s", pipe.build.name()));
                  }
                  break;
               }

               Pipe previousForBuild = candidates[pipe.build.ordinal()];
               Pipe previousAny = candidates[DiscordBuild.ANY.ordinal()];
               candidates[pipe.build.ordinal()] = pipe;
               candidates[DiscordBuild.ANY.ordinal()] = pipe;
               if (previousForBuild != null && previousForBuild != pipe) {
                  closeIfUnreferenced(candidates, previousForBuild, client);
               }
               if (previousAny != null && previousAny != pipe && previousAny != previousForBuild) {
                  closeIfUnreferenced(candidates, previousAny, client);
               }
               pipe.build = null;
               client.clearConnectingPipe(pipe);
               pipe = null;
            } else if (client.isDebugMode()) {
               client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Unable to locate IPC Pipe: \"%s\"", location));
            }
         } catch (IOException | RuntimeException handshakeFailure) {
            try {
               client.checkConnectActive();
            } catch (CancellationException cancelled) {
               abortPipe(pipe, client);
               abortCandidates(candidates, client);
               throw cancelled;
            }
            if (pipe == null) {
               if (handshakeFailure instanceof IOException ioFailure) {
                  throw new RuntimeException("Failed to connect to a Discord IPC endpoint", ioFailure);
               }
               throw (RuntimeException)handshakeFailure;
            }
            try {
               pipe.close();
            } catch (IOException | RuntimeException closeFailure) {
               if (client.isDebugMode()) {
                  client.getCurrentLogger(LOGGER).warn("Failed to close an IPC pipe after handshake failure", closeFailure);
               }
            }
            client.clearConnectingPipe(pipe);
            pipe = null;
         }
      }

      if (pipe == null) {
         for (int preferenceIndex = 1; preferenceIndex < preferredBuilds.length; preferenceIndex++) {
            DiscordBuild preferredBuild = preferredBuilds[preferenceIndex];
            if (client.isDebugMode()) {
               client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Looking for client build: %s", preferredBuild.name()));
            }

            if (candidates[preferredBuild.ordinal()] != null) {
               pipe = candidates[preferredBuild.ordinal()];
               candidates[preferredBuild.ordinal()] = null;
               if (preferredBuild == DiscordBuild.ANY) {
                  for (int buildIndex = 0; buildIndex < candidates.length; buildIndex++) {
                     if (candidates[buildIndex] == pipe) {
                        pipe.build = DiscordBuild.values()[buildIndex];
                        candidates[buildIndex] = null;
                     }
                  }
               } else {
                  pipe.build = preferredBuild;
               }

               if (client.isDebugMode()) {
                  client.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Found preferred client: %s", pipe.build.name()));
               }
               break;
            }
         }

         if (pipe == null) {
            closeCandidates(candidates, null, client);
            throw new NoDiscordClientException();
         }
      }

      closeCandidates(candidates, pipe, client);
      try {
         client.trackConnectingPipe(pipe);
         client.completeConnection(pipe);
      } catch (CancellationException cancelled) {
         abortPipe(pipe, client);
         throw cancelled;
      }
      return pipe;
   }

   private static void abortPipe(Pipe pipe, IPCClient client) {
      if (pipe == null) return;
      try {
         pipe.abort();
      } catch (IOException | RuntimeException abortFailure) {
         if (client.isDebugMode()) {
            client.getCurrentLogger(LOGGER).warn("Failed to abort an IPC pipe after cancellation", abortFailure);
         }
      }
   }

   private static void abortCandidates(Pipe[] candidates, IPCClient client) {
      IdentityHashMap<Pipe, Boolean> aborted = new IdentityHashMap<>();
      for (int index = 0; index < candidates.length; index++) {
         Pipe candidate = candidates[index];
         candidates[index] = null;
         if (candidate == null || aborted.put(candidate, Boolean.TRUE) != null) continue;
         abortPipe(candidate, client);
      }
   }

   private static void closeCandidates(Pipe[] candidates, Pipe keep, IPCClient client) {
      IdentityHashMap<Pipe, Boolean> closed = new IdentityHashMap<>();
      for (int index = 0; index < candidates.length; index++) {
         Pipe candidate = candidates[index];
         candidates[index] = null;
         if (candidate == null || candidate == keep || closed.put(candidate, Boolean.TRUE) != null) {
            continue;
         }
         try {
            candidate.close();
         } catch (IOException | RuntimeException closeFailure) {
            if (client.isDebugMode()) {
               client.getCurrentLogger(LOGGER).warn("Failed to close an unselected IPC candidate", closeFailure);
            }
         }
      }
   }

   private static void closeIfUnreferenced(Pipe[] candidates, Pipe previous, IPCClient client) {
      for (Pipe candidate : candidates) {
         if (candidate == previous) {
            return;
         }
      }
      try {
         previous.close();
      } catch (IOException | RuntimeException closeFailure) {
         if (client.isDebugMode()) {
            client.getCurrentLogger(LOGGER).warn("Failed to close a superseded IPC candidate", closeFailure);
         }
      }
   }

   private static Pipe createPipe(IPCClient client, HashMap callbacks, File endpoint) {
      String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
      if (os.startsWith("windows")) {
         return new WindowsPipe(client, callbacks, endpoint);
      }

      if (!os.contains("linux") && !os.contains("mac") && !os.contains("darwin")) {
         throw new RuntimeException("Unsupported OS: " + os);
      }

      try {
         return (os.contains("mac") || os.contains("darwin")) ? new MacPipe(client, callbacks, endpoint) : new UnixPipe(client, callbacks, endpoint);
      } catch (IOException failure) {
         throw new RuntimeException("Unable to create MacOS/Unix Pipe", failure);
      }
   }

   private static String generateNonce() {
      return UUID.randomUUID().toString();
   }

   private static String getPipeLocation(int index) {
      String directory = null;
      String pipeName = "discord-ipc-" + index;
      if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows")) {
         return "\\\\?\\pipe\\" + pipeName;
      }

      for (String environmentVariable : unixPaths) {
         directory = System.getenv(environmentVariable);
         if (directory != null) {
            break;
         }
      }

      if (directory == null) {
         directory = "/tmp";
      }

      for (String folder : unixFolderPaths) {
         String candidate = directory + folder;
         File candidateDirectory = new File(candidate);
         if (candidateDirectory.exists() && candidateDirectory.isDirectory() && candidateDirectory.list().length > 0) {
            directory = candidate;
            break;
         }
      }

      return directory + "/" + pipeName;
   }

   public void send(Packet.OpCode opcode, JsonObject json, Callback callback) {
      try {
         String nonce = generateNonce();
         json.addProperty("nonce", nonce);
         Packet packet = new Packet(opcode, json, this.ipcClient.getEncoding());
         if (callback != null && !callback.isEmpty()) {
            this.callbacks.put(nonce, callback);
         }

         this.write(packet.toBytes());
         if (this.ipcClient.isDebugMode()) {
            this.ipcClient.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Sent packet: %s", packet.toDecodedString()));
         }

         if (this.listener != null) {
            this.listener.onPacketSent(this.ipcClient, packet);
         }
      } catch (IOException failure) {
         this.ipcClient.getCurrentLogger(LOGGER).error("Encountered an IOException while sending a packet and disconnected!");
         this.status = PipeStatus.DISCONNECTED;
      }
   }

   public void send(Packet.OpCode opcode, JsonObject json) {
      this.send(opcode, json, null);
   }

   public Packet receive(Packet.OpCode opcode, byte[] payload) {
      JsonObject json = new JsonParser().parse(new String(payload)).getAsJsonObject();
      Packet packet = new Packet(opcode, json, this.ipcClient.getEncoding());
      if (this.ipcClient.isDebugMode()) {
         this.ipcClient.getCurrentLogger(LOGGER).info(String.format("[DEBUG] Received packet: %s", packet));
      }

      if (this.listener != null) {
         this.listener.onPacketReceived(this.ipcClient, packet);
      }

      return packet;
   }

   public abstract Packet read() throws IOException;

   public abstract void write(byte[] packet) throws IOException;

   public abstract void registerApp(String applicationId, String command);

   public abstract void registerSteamGame(String applicationId, String steamId);

   public PipeStatus getStatus() {
      return this.status;
   }

   public void setStatus(PipeStatus status) {
      this.status = status;
   }

   public void setListener(IPCListener listener) {
      this.listener = listener;
   }

   public abstract void close() throws IOException;

   /** Closes the transport immediately without sending an IPC protocol frame. */
   public abstract void abort() throws IOException;

   public DiscordBuild getDiscordBuild() {
      return this.build;
   }

   public User getCurrentUser() {
      return this.currentUser;
   }
}
