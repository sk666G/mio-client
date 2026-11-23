package dev.schizo.client.integrations.discord;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import org.junit.jupiter.api.Test;

class DiscordIpcFrameCodecTest {
   @Test
   void readsLittleEndianOpcodeAndPayload() throws IOException {
      byte[] serialized = frame(3, new byte[]{65, 66, 67});

      DiscordIpcFrameCodec.Frame decoded = DiscordIpcFrameCodec.read(
         Channels.newChannel(new ByteArrayInputStream(serialized))
      );

      assertEquals(3, decoded.opcode());
      assertArrayEquals(new byte[]{65, 66, 67}, decoded.body());
   }

   @Test
   void rejectsNegativePayloadLengthBeforeAllocation() {
      byte[] header = header(1, -1);
      assertThrows(IOException.class, () -> DiscordIpcFrameCodec.read(
         Channels.newChannel(new ByteArrayInputStream(header))
      ));
   }

   @Test
   void rejectsOversizedPayloadLengthBeforeAllocation() {
      byte[] header = header(1, DiscordIpcFrameCodec.MAX_PAYLOAD_BYTES + 1);
      assertThrows(IOException.class, () -> DiscordIpcFrameCodec.read(
         Channels.newChannel(new ByteArrayInputStream(header))
      ));
   }

   @Test
   void rejectsTruncatedHeader() {
      byte[] header = new byte[DiscordIpcFrameCodec.HEADER_BYTES - 1];
      assertThrows(IOException.class, () -> DiscordIpcFrameCodec.read(
         Channels.newChannel(new ByteArrayInputStream(header))
      ));
   }

   @Test
   void rejectsTruncatedPayload() {
      byte[] partial = frame(1, new byte[]{1, 2});
      ByteBuffer.wrap(partial).order(ByteOrder.LITTLE_ENDIAN)
         .putInt(4, 5);
      assertThrows(IOException.class, () -> DiscordIpcFrameCodec.read(
         Channels.newChannel(new ByteArrayInputStream(partial))
      ));
   }

   @Test
   void writesTheCompleteSerializedFrame() throws IOException {
      byte[] serialized = frame(2, new byte[]{9, 8, 7});
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      DiscordIpcFrameCodec.writeFully(Channels.newChannel(output), serialized);

      assertArrayEquals(serialized, output.toByteArray());
   }

   @Test
   void rejectsOutboundFramesWithMismatchedHeaderLength() {
      byte[] serialized = frame(2, new byte[]{1});
      ByteBuffer.wrap(serialized).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 2);

      assertThrows(IOException.class, () -> DiscordIpcFrameCodec.writeFully(
         Channels.newChannel(new ByteArrayOutputStream()), serialized
      ));
   }

   @Test
   void rejectsOutboundFramesAboveMaximumSize() {
      byte[] serialized = new byte[DiscordIpcFrameCodec.HEADER_BYTES + DiscordIpcFrameCodec.MAX_PAYLOAD_BYTES + 1];
      assertThrows(IOException.class, () -> DiscordIpcFrameCodec.writeFully(
         Channels.newChannel(new ByteArrayOutputStream()), serialized
      ));
   }

   private static byte[] frame(int opcode, byte[] body) {
      ByteBuffer buffer = ByteBuffer.allocate(DiscordIpcFrameCodec.HEADER_BYTES + body.length)
         .order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(opcode);
      buffer.putInt(body.length);
      buffer.put(body);
      return buffer.array();
   }

   private static byte[] header(int opcode, int payloadLength) {
      return ByteBuffer.allocate(DiscordIpcFrameCodec.HEADER_BYTES)
         .order(ByteOrder.LITTLE_ENDIAN)
         .putInt(opcode)
         .putInt(payloadLength)
         .array();
   }
}
