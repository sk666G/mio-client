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

package com.jagrosh.discordipc;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;

public interface IPCListener {
   void onPacketSent(IPCClient client, Packet packet);

   void onPacketReceived(IPCClient client, Packet packet);

   void onActivityJoin(IPCClient client, String secret);

   void onActivitySpectate(IPCClient client, String secret);

   void onActivityJoinRequest(IPCClient client, String secret, User user);

   void onReady(IPCClient client);

   void onClose(IPCClient client, JsonObject response);

   void onDisconnect(IPCClient client, Throwable cause);
}
