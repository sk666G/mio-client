package dev.schizo.client.utils.world;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

final class MapCacheReader {
   static final int MAX_MAPS = 1024;
   private static final int COLOR_BYTES = 128 * 128;

   private MapCacheReader() {}

   interface Decoder<T> {
      T decode(byte scale, byte[] colors);
   }

   static <T> Map<String, T> read(Path path, Decoder<T> decoder) throws IOException {
      if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
         throw new IOException("Map cache must be a regular file");
      }
      try (DataInputStream input = new DataInputStream(new BufferedInputStream(
            new InflaterInputStream(Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS))))) {
         int count = input.readInt();
         if (count < 0 || count > MAX_MAPS) {
            throw new IOException("Invalid map count: " + count);
         }
         Map<String, T> entries = new HashMap<>();
         for (int i = 0; i < count; i++) {
            String name = input.readUTF();
            byte scale = input.readByte();
            byte[] colors = new byte[COLOR_BYTES];
            input.readFully(colors);
            entries.put(name, decoder.decode(scale, colors));
         }
         if (input.read() != -1) {
            throw new IOException("Unexpected trailing map data");
         }
         return entries;
      }
   }
}
