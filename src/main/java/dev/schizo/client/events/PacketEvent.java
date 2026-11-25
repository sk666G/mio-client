/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.events;

import dev.schizo.client.ClientAccess;
import net.minecraft.network.protocol.Packet;

public abstract sealed class PacketEvent
   extends CancellableEvent
   permits PacketEvent.Send,
   PacketEvent.Receive,
   PacketEvent.PostSend {
   private final Packet packet;
   private final ThreadOrigin threadOrigin;

   public Packet getPacket() {
      return this.packet;
   }

   public ThreadOrigin getThreadOrigin() {
      return this.threadOrigin;
   }

   public boolean isClientThread() {
      return this.threadOrigin == ThreadOrigin.CLIENT;
   }

   /**
    * Runs client-state work immediately when already on the Minecraft thread, otherwise
    * schedules it onto that thread. Packet cancellation itself must remain synchronous.
    */
   public void executeOnClientThread(Runnable action) {
      java.util.Objects.requireNonNull(action, "action");
      if (ClientAccess.schizoMinecraft.isSameThread()) {
         action.run();
      } else {
         ClientAccess.schizoMinecraft.execute(action);
      }
   }

   protected PacketEvent(Packet packet) {
      this.packet = java.util.Objects.requireNonNull(packet, "packet");
      this.threadOrigin = ClientAccess.schizoMinecraft.isSameThread()
         ? ThreadOrigin.CLIENT
         : ThreadOrigin.ASYNC;
   }

   public enum ThreadOrigin {
      CLIENT,
      ASYNC
   }

   public static final class PostSend extends PacketEvent {
      public PostSend(Packet packet) {
         super(packet);
      }
   }

   public static final class Receive extends PacketEvent {
      public Receive(Packet packet) {
         super(packet);
      }
   }

   public static final class Send extends PacketEvent {
      public Send(Packet packet) {
         super(packet);
      }
   }
}
