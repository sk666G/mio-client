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

import java.util.function.Consumer;

public class Callback {
   private final Consumer success;
   private final Consumer failure;

   public Callback() {
      this(null, null);
   }

   public Callback(Consumer success) {
      this(success, null);
   }

   public Callback(Consumer success, Consumer failure) {
      this.success = success;
      this.failure = failure;
   }

   public boolean isEmpty() {
      return this.success == null && this.failure == null;
   }

   public void succeed(Packet packet) {
      if (this.success != null) {
         this.success.accept(packet);
      }
   }

   public void fail(String message) {
      if (this.failure != null) {
         this.failure.accept(message);
      }
   }
}
