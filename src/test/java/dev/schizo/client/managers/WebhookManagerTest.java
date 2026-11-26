package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

class WebhookManagerTest {
   @Test
   void usesAnExplicitJsonConfigurationFile() {
      WebhookManager manager = new WebhookManager(request ->
         CompletableFuture.completedFuture(response(204, ""))
      );

      assertEquals("webhook.json", manager.getConfigName());
   }

   @Test
   void retriesTransientFailureAndAcknowledgesSuccess() {
      AtomicInteger calls = new AtomicInteger();
      WebhookManager manager = new WebhookManager(request -> {
         if (calls.incrementAndGet() == 1) return CompletableFuture.failedFuture(new IOException("offline"));
         return CompletableFuture.completedFuture(response(204, ""));
      });
      manager.setUrl("https://example.test/webhook");

      manager.sendContent("hello").join();

      assertEquals(2, calls.get());
   }

   @Test
   void treatsPartialChunkFailureAsFailedBatchAndBoundsAttempts() {
      AtomicInteger calls = new AtomicInteger();
      WebhookManager manager = new WebhookManager(request -> {
         int attempt = calls.incrementAndGet();
         if (attempt == 1) return CompletableFuture.completedFuture(response(204, ""));
         return CompletableFuture.completedFuture(response(503, ""));
      });
      manager.setUrl("https://example.test/webhook");

      assertThrows(CompletionException.class, () -> manager.sendContent("x".repeat(2500)).join());
      assertEquals(4, calls.get()); // first chunk once, second chunk plus two retries
   }

   @Test
   void doesNotRetryPermanentHttpFailure() {
      AtomicInteger calls = new AtomicInteger();
      WebhookManager manager = new WebhookManager(request -> {
         calls.incrementAndGet();
         return CompletableFuture.completedFuture(response(401, ""));
      });
      manager.setUrl("https://example.test/webhook");

      assertThrows(CompletionException.class, () -> manager.sendContent("hello").join());
      assertEquals(1, calls.get());
   }

   @Test
   void telegramTokenIsRuntimeOnlyAndApiRejectionFailsDelivery() {
      String token = "123456:SecretToken_Value";
      AtomicInteger calls = new AtomicInteger();
      WebhookManager manager = new WebhookManager(request -> {
         calls.incrementAndGet();
         return CompletableFuture.completedFuture(response(200, "{\"ok\":false}"));
      });
      manager.setTelegram(token, "12345");

      assertTrue(manager.isConfigured());
      assertFalse(manager.toJson().toString().contains(token));
      assertEquals("", manager.getUrl());
      assertThrows(CompletionException.class, () -> manager.sendContent("hello").join());
      assertEquals(1, calls.get());

      WebhookManager restored = new WebhookManager(request -> CompletableFuture.completedFuture(response(200, "{\"ok\":true}")));
      restored.fromJson(manager.toJson());
      assertTrue(restored.isTelegram());
      assertFalse(restored.isConfigured());
      assertTrue(restored.getDestinationDescription().contains("re-enter"));
   }

   @Test
   void replacingDestinationStopsUnsentChunks() {
      AtomicInteger calls = new AtomicInteger();
      CompletableFuture<HttpResponse<String>> first = new CompletableFuture<>();
      WebhookManager manager = new WebhookManager(request -> {
         calls.incrementAndGet();
         return first;
      });
      manager.setUrl("https://example.test/original");
      var batch = manager.sendContent("x".repeat(2500));
      manager.setUrl("https://example.test/replacement");
      first.complete(response(204, ""));
      assertThrows(CompletionException.class, batch::join);
      assertEquals(1, calls.get());
   }

   @Test
   void privacyChangeStopsRemainingChunks() {
      var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
      var pending = new CompletableFuture<HttpResponse<String>>();
      AtomicInteger calls = new AtomicInteger();
      WebhookManager manager = new WebhookManager(request -> {
         calls.incrementAndGet();
         return pending;
      }, allowed::get);
      manager.setUrl("https://example.test/webhook");
      var batch = manager.sendContent("x".repeat(2500));
      allowed.set(false);
      pending.complete(response(204, ""));
      assertThrows(CompletionException.class, batch::join);
      assertEquals(1, calls.get());
   }

   private static HttpResponse<String> response(int status, String body) {
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.test")).GET().build();
      return new HttpResponse<>() {
         @Override public int statusCode() { return status; }
         @Override public HttpRequest request() { return request; }
         @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
         @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (name, value) -> true); }
         @Override public String body() { return body; }
         @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
         @Override public URI uri() { return request.uri(); }
         @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
      };
   }
}
