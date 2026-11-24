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

import dev.schizo.client.integrations.discord.DiscordIpcFrameCodec;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.impl.WinRegistry;
import java.io.File;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsPipe extends Pipe {
   private static final Logger LOGGER = LoggerFactory.getLogger(WindowsPipe.class);
   private static final Float javaSpec = Float.parseFloat(System.getProperty("java.specification.version"));
   private final int targetKey = -2147483647;
   private final long targetLongKey = -2147483647L;
   public RandomAccessFile file;
   private final FileChannel channel;
   private final Object writeLock = new Object();

   WindowsPipe(IPCClient client, HashMap callbacks, File endpoint) {
      super(client, callbacks);

      try {
         this.file = new RandomAccessFile(endpoint, "rw");
         this.channel = this.file.getChannel();
      } catch (FileNotFoundException failure) {
         throw new RuntimeException("Unable to access '" + endpoint + "', check file permissions", failure);
      }
   }

   @Override
   public void write(byte[] bytes) throws IOException {
      synchronized (this.writeLock) {
         ByteBuffer buffer = ByteBuffer.wrap(bytes);
         while (buffer.hasRemaining()) {
            int written = this.channel.write(buffer);
            if (written < 0) {
               throw new EOFException("Discord IPC pipe closed while writing");
            }
            if (written == 0) {
               Thread.yield();
            }
         }
      }
   }

   @Override
   public Packet read() throws IOException {
      if (this.status == PipeStatus.DISCONNECTED) {
         throw new IOException("Disconnected!");
      }
      if (this.status == PipeStatus.CLOSED) {
         return new Packet(Packet.OpCode.CLOSE, null, this.ipcClient.getEncoding());
      }

      try {
         Packet.OpCode[] opcodes = Packet.OpCode.values();
         ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
         this.readFully(header);
         header.flip();

         int opcodeId = DiscordIpcFrameCodec.validateOpcode(header.getInt(), opcodes.length);
         int length = DiscordIpcFrameCodec.validatePayloadLength(header.getInt());
         ByteBuffer payload = ByteBuffer.allocate(length);
         this.readFully(payload);
         return this.receive(opcodes[opcodeId], payload.array());
      } catch (IOException failure) {
         if (this.status == PipeStatus.CLOSED) {
            return new Packet(Packet.OpCode.CLOSE, null, this.ipcClient.getEncoding());
         }
         throw failure;
      }
   }

   private void readFully(ByteBuffer buffer) throws IOException {
      while (buffer.hasRemaining()) {
         if (this.status == PipeStatus.CLOSED) {
            throw new IOException("Discord IPC pipe was closed");
         }
         int read = this.channel.read(buffer);
         if (read < 0) {
            throw new EOFException("Discord IPC pipe closed during a frame");
         }
      }
   }

   @Override
   public void abort() throws IOException {
      this.status = PipeStatus.CLOSED;
      this.channel.close();
   }

   @Override
   public void close() throws IOException {
      if (this.status == PipeStatus.CLOSED) return;
      if (this.ipcClient.isDebugMode()) {
         this.ipcClient.getCurrentLogger(LOGGER).info("[DEBUG] Closing IPC pipe...");
      }

      this.status = PipeStatus.CLOSING;
      try {
         this.send(Packet.OpCode.CLOSE, new JsonObject());
      } finally {
         this.status = PipeStatus.CLOSED;
         this.channel.close();
      }
   }

   @Override
   public void registerApp(String applicationId, String command) {
      String javaHome = System.getProperty("java.home");
      File javaExecutable = new File(javaHome.split(";")[0] + "/bin/java.exe");
      File javawExecutable = new File(javaHome.split(";")[0] + "/bin/javaw.exe");
      String javaPath = javaExecutable.exists() ? javaExecutable.getAbsolutePath() : (javawExecutable.exists() ? javawExecutable.getAbsolutePath() : null);
      if (javaPath == null) {
         throw new RuntimeException("Unable to find java path");
      }

      String registrationCommand;
      if (command != null) {
         registrationCommand = command;
      } else {
         registrationCommand = javaPath;
      }

      String protocol = "discord-" + applicationId;
      String description = "URL:Run game " + applicationId + " protocol";
      String protocolKey = "Software\\Classes\\" + protocol;
      String iconKey = protocolKey + "\\DefaultIcon";
      String commandKey = protocolKey + "\\shell\\open\\command";

      try {
         if (javaSpec >= 11.0F) {
            WinRegistry.createKey(-2147483647L, protocolKey);
            WinRegistry.writeStringValue(-2147483647L, protocolKey, "", description);
            WinRegistry.writeStringValue(-2147483647L, protocolKey, "URL Protocol", "\u0000");
            WinRegistry.createKey(-2147483647L, iconKey);
            WinRegistry.writeStringValue(-2147483647L, iconKey, "", javaPath);
            WinRegistry.createKey(-2147483647L, commandKey);
            WinRegistry.writeStringValue(-2147483647L, commandKey, "", registrationCommand);
         } else {
            WinRegistry.createKey(-2147483647, protocolKey);
            WinRegistry.writeStringValue(-2147483647, protocolKey, "", description);
            WinRegistry.writeStringValue(-2147483647, protocolKey, "URL Protocol", "\u0000");
            WinRegistry.createKey(-2147483647, iconKey);
            WinRegistry.writeStringValue(-2147483647, iconKey, "", javaPath);
            WinRegistry.createKey(-2147483647, commandKey);
            WinRegistry.writeStringValue(-2147483647, commandKey, "", registrationCommand);
         }
      } catch (Throwable failure) {
         throw new RuntimeException("Unable to modify Discord registry keys", failure);
      }
   }

   @Override
   public void registerSteamGame(String applicationId, String steamId) {
      try {
         String steamPath;
         if (javaSpec >= 11.0F) {
            steamPath = WinRegistry.readString(-2147483647L, "Software\\\\Valve\\\\Steam", "SteamExe");
         } else {
            steamPath = WinRegistry.readString(-2147483647, "Software\\\\Valve\\\\Steam", "SteamExe");
         }

         if (steamPath == null) {
            throw new RuntimeException("Steam exe path not found");
         }

         steamPath = steamPath.replaceAll("/", "\\");
         String command = "\"" + steamPath + "\" steam://rungameid/" + steamId;
         this.registerApp(applicationId, command);
      } catch (Exception failure) {
         throw new RuntimeException("Unable to register Steam game", failure);
      }
   }
}
