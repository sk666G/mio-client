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

import com.google.gson.JsonObject;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class Packet {
   private final Packet.OpCode op;
   private final JsonObject data;
   private final String encoding;

   public Packet(Packet.OpCode op, JsonObject data, String encoding) {
      this.op = op;
      this.data = data;
      this.encoding = encoding;
   }

   @Deprecated
   public Packet(Packet.OpCode op, JsonObject data) {
      this(op, data, "UTF-8");
   }

   public byte[] toBytes() {
      String json = this.data.toString();

      byte[] bytes;
      try {
         bytes = json.getBytes(this.encoding);
      } catch (UnsupportedEncodingException exception) {
         bytes = json.getBytes();
      }

      ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 8);
      buffer.putInt(Integer.reverseBytes(this.op.ordinal()));
      buffer.putInt(Integer.reverseBytes(bytes.length));
      buffer.put(bytes);
      return buffer.array();
   }

   public Packet.OpCode getOp() {
      return this.op;
   }

   public JsonObject getJson() {
      return this.data;
   }

   @Override
   public String toString() {
      return "Pkt:" + this.getOp() + this.getJson().toString();
   }

   public String toDecodedString() {
      try {
         return "Pkt:" + this.getOp() + new String(this.getJson().toString().getBytes(this.encoding));
      } catch (UnsupportedEncodingException exception) {
         return "Pkt:" + this.getOp() + this.getJson().toString();
      }
   }

   public enum OpCode {
      HANDSHAKE,
      FRAME,
      CLOSE,
      PING,
      PONG;
   }
}
