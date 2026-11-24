package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ModulePresetCodecTest {
   @Test
   void decodesUtf8PayloadDeterministically() {
      String json = "{\"m\":\"Example\",\"label\":\"café 雪\"}";
      String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

      JsonObject decoded = ModulePresetCodec.decodePayload(encoded, 256);

      assertEquals("Example", decoded.get("m").getAsString());
      assertEquals("café 雪", decoded.get("label").getAsString());
   }

   @Test
   void acceptsCopiedPayloadsWithCrLfLineBreaks() {
      String json = "{\"m\":\"Example\"}";
      String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
      String wrapped = encoded.substring(0, 4) + "\r\n" + encoded.substring(4);

      JsonObject decoded = ModulePresetCodec.decodePayload(wrapped, 64);

      assertEquals("Example", decoded.get("m").getAsString());
   }

   @Test
   void rejectsDecodedPayloadBeyondTheConfiguredLimit() {
      String json = "{\"m\":\"Example\",\"x\":\"0123456789\"}";
      String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

      IllegalArgumentException failure = assertThrows(
         IllegalArgumentException.class,
         () -> ModulePresetCodec.decodePayload(encoded, 8)
      );
      assertTrue(failure.getMessage().contains("size limit"));
   }

   @Test
   void rejectsNonObjectJson() {
      String encoded = Base64.getEncoder().encodeToString("[1,2,3]".getBytes(StandardCharsets.UTF_8));

      assertThrows(IllegalArgumentException.class, () -> ModulePresetCodec.decodePayload(encoded, 64));
   }

   @Test
   void rejectsMissingPayloadAndInvalidLimit() {
      assertThrows(IllegalArgumentException.class, () -> ModulePresetCodec.decodePayload(null, 64));
      assertThrows(IllegalArgumentException.class, () -> ModulePresetCodec.decodePayload("", -1));
   }
}
