/*
 * Source: CDAGaming/DiscordIPC.
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

package com.jagrosh.discordipc.impl;

import java.util.Random;

public class Backoff {
   private final long minAmount;
   private final long maxAmount;
   private final Random randGenerator;
   private long current;
   private int fails;

   public Backoff(long minAmount, long maxAmount) {
      this.minAmount = minAmount;
      this.maxAmount = maxAmount;
      this.current = minAmount;
      this.fails = 0;
      this.randGenerator = new Random();
   }

   public void reset() {
      this.fails = 0;
      this.current = this.minAmount;
   }

   public long nextDelay() {
      this.fails++;
      double increment = this.current * 2.0 * this.rand01();
      this.current = Math.min(this.current + (long)increment, this.maxAmount);
      return this.current;
   }

   private double rand01() {
      return this.randGenerator.nextDouble();
   }
}
