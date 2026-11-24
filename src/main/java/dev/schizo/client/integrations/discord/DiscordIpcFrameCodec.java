/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Original Schizo implementation.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations.discord;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/** Reads and writes the eight-byte little-endian Discord IPC frame header and bounded payload. */
public final class DiscordIpcFrameCodec {
   public static final int HEADER_BYTES = Integer.BYTES * 2;
   public static final int MAX_PAYLOAD_BYTES = 16 * 1024 * 1024;

   private DiscordIpcFrameCodec() {}

   public static Frame read(ReadableByteChannel input) throws IOException {
      Objects.requireNonNull(input, "input");
      ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
      readFully(input, header);
      header.flip();

      int opcode = header.getInt();
      int payloadLength = validatePayloadLength(header.getInt());

      ByteBuffer payload = ByteBuffer.allocate(payloadLength);
      readFully(input, payload);
      return new Frame(opcode, payload.array());
   }

   public static void writeFully(WritableByteChannel output, byte[] serializedFrame) throws IOException {
      Objects.requireNonNull(output, "output");
      Objects.requireNonNull(serializedFrame, "serializedFrame");
      if (serializedFrame.length < HEADER_BYTES || serializedFrame.length > HEADER_BYTES + MAX_PAYLOAD_BYTES) {
         throw new IOException("Discord IPC frame size is outside the supported range");
      }

      ByteBuffer header = ByteBuffer.wrap(serializedFrame).order(ByteOrder.LITTLE_ENDIAN);
      header.getInt();
      int payloadLength = header.getInt();
      if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES
         || payloadLength != serializedFrame.length - HEADER_BYTES) {
         throw new IOException("Discord IPC frame header does not match its payload size");
      }

      ByteBuffer frame = ByteBuffer.wrap(serializedFrame);
      while (frame.hasRemaining()) {
         int written = output.write(frame);
         if (written < 0) {
            throw new EOFException("Discord IPC stream closed during write");
         }
         if (written == 0) {
            throw new IOException("Discord IPC writer made no progress");
         }
      }
   }

   public static int validatePayloadLength(int payloadLength) throws IOException {
      if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES) {
         throw new IOException("Discord IPC payload length is outside the supported range");
      }
      return payloadLength;
   }

   private static void readFully(ReadableByteChannel input, ByteBuffer target) throws IOException {
      while (target.hasRemaining()) {
         int read = input.read(target);
         if (read < 0) {
            throw new EOFException("Discord IPC stream ended inside a frame");
         }
         if (read == 0) {
            throw new IOException("Discord IPC reader made no progress");
         }
      }
   }

   public record Frame(int opcode, byte[] body) {
   }
}
