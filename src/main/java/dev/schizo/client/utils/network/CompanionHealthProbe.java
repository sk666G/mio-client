package dev.schizo.client.utils.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Authenticated companion health probe. Never reads or alters Minecraft traffic. */
public final class CompanionHealthProbe {
   private static final Duration TIMEOUT = Duration.ofSeconds(4);
   private final URI endpoint;
   private final String token;
   private final HttpClient client;

   public CompanionHealthProbe(String endpoint, String token) {
      this.endpoint = validateEndpoint(endpoint);
      if (token == null || !token.matches("[A-Za-z0-9_-]{32,256}")) {
         throw new IllegalArgumentException("Invalid companion token");
      }
      this.token = token;
      this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT)
         .followRedirects(HttpClient.Redirect.NEVER).build();
   }

   static URI validateEndpoint(String value) {
      if (value == null || value.length() > 2048) throw new IllegalArgumentException("Missing companion URL");
      URI uri = URI.create(value);
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
         || uri.getRawUserInfo() != null || uri.getRawQuery() != null
         || uri.getRawFragment() != null || !"/health".equals(uri.getRawPath())
         || uri.getPort() == 0 || uri.getPort() > 65535) {
         throw new IllegalArgumentException("Expected HTTPS companion /health URL");
      }
      return uri;
   }

   public CompletableFuture<Result> sample() {
      HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(TIMEOUT)
         .header("Authorization", "Bearer " + token)
         .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
      long start = System.nanoTime();
      return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
         .thenApply(response -> classify(response.statusCode(), System.nanoTime() - start));
   }

   static Result classify(int status, long nanos) {
      if (status == 204) return new Result("Reachable", Math.max(0, TimeUnit.NANOSECONDS.toMillis(nanos)));
      if (status == 401 || status == 403) return new Result("Authentication failed", -1);
      return new Result("Unexpected HTTP " + status, -1);
   }

   public void shutdown() {
      client.shutdownNow();
   }

   public record Result(String status, long millis) {}
}
