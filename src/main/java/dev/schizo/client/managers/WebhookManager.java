/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.network.HttpRequests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.schizo.client.settings.JsonConfigurable;
import java.net.URI;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebhookManager implements JsonConfigurable {
   private static final Logger LOGGER = LoggerFactory.getLogger(WebhookManager.class);
   private static final Pattern TELEGRAM_PATH = Pattern.compile("/bot([0-9]+:[A-Za-z0-9_-]+)/sendMessage", Pattern.CASE_INSENSITIVE);
   private static final Pattern BOT_TOKEN = Pattern.compile("[0-9]+:[A-Za-z0-9_-]+");
   private static final Pattern CHAT_ID = Pattern.compile("-?[0-9]+|@[A-Za-z0-9_]+");
   private static final Pattern ANSI_COLOR = Pattern.compile("\\x1B\\[[0-9;]*m");
   private volatile Destination destination = new Destination(null, "", null, false);
   private final RequestSender requestSender;

   @FunctionalInterface
   interface RequestSender {
      CompletableFuture<java.net.http.HttpResponse<String>> send(HttpRequest request);
   }

   private record Destination(URI uri, String chatId, String telegramToken, boolean telegram) {
   }

   public WebhookManager() {
      this(HttpRequests::sendAsync);
   }

   WebhookManager(RequestSender requestSender) {
      this.requestSender = Objects.requireNonNull(requestSender);
   }

   @Override
   public void fromJson(JsonElement json) {
      JsonObject object = json.getAsJsonObject();
      Destination target;
      if (object.has("type") && "telegram".equalsIgnoreCase(object.get("type").getAsString())) {
         String chatId = object.has("telegramChatId") ? object.get("telegramChatId").getAsString() : "";
         if (!chatId.isEmpty()) validateChatId(chatId);
         target = new Destination(null, chatId, null, true);
      } else {
         target = parseUrl(object.has("url") && !object.get("url").isJsonNull() ? object.get("url").getAsString() : "");
         if (target.telegram() && object.has("telegramChatId")) {
            String chatId = object.get("telegramChatId").getAsString();
            if (!chatId.isEmpty()) validateChatId(chatId);
            target = new Destination(null, chatId, target.telegramToken(), true);
         }
      }
      this.destination = target;
   }

   @Override
   public String getConfigName() {
      return "webhook.json";
   }

   @Override
   public JsonElement toJson() {
      Destination target = this.destination;
      JsonObject json = new JsonObject();
      json.addProperty("url", target.telegram() || target.uri() == null ? "" : target.uri().toString());
      if (target.telegram()) {
         json.addProperty("type", "telegram");
         json.addProperty("telegramChatId", target.chatId());
      }
      return json;
   }

   public boolean isConfigured() {
      Destination target = this.destination;
      return target.telegram()
         ? target.telegramToken() != null && !target.chatId().isBlank()
         : target.uri() != null;
   }

   public boolean isTelegram() {
      return this.destination.telegram();
   }

   public String getTelegramChatId() {
      return this.destination.chatId();
   }

   public void setTelegramChatId(String chatId) {
      Destination target = this.destination;
      if (!target.telegram()) {
         throw new IllegalArgumentException("Chat ID is not supported for current webhook");
      }
      validateChatId(chatId);
      this.destination = new Destination(null, chatId, target.telegramToken(), true);
   }

   public String getDestinationDescription() {
      Destination target = this.destination;
      if (target.telegram()) {
         if (target.telegramToken() == null) return "Telegram (token must be re-entered after restart)";
         return target.chatId().isBlank() ? "Telegram (chat ID missing)" : "Telegram (configured for this session)";
      }
      if (target.uri() == null) return "No notification destination configured";
      return "Webhook (" + target.uri().getHost() + ")";
   }

   public void setTelegram(String botToken, String chatId) {
      if (botToken == null || !BOT_TOKEN.matcher(botToken).matches()) {
         throw new IllegalArgumentException("Invalid Telegram bot token");
      }
      validateChatId(chatId);
      this.destination = new Destination(null, chatId, botToken, true);
   }

   public void setUrl(String url) {
      this.destination = parseUrl(url);
   }

   private static Destination parseUrl(String url) {
      if (url == null || url.isBlank()) return new Destination(null, "", null, false);
      try {
         URI uri = URI.create(url.trim());
         if ((!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException();
         }
         if ("api.telegram.org".equalsIgnoreCase(uri.getHost())) {
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getPort() != -1 && uri.getPort() != 443) {
               throw new IllegalArgumentException();
            }
            java.util.regex.Matcher path = TELEGRAM_PATH.matcher(uri.getPath());
            if (!path.matches()) throw new IllegalArgumentException();
            String chatId = "";
            String query = uri.getRawQuery();
            if (query != null) {
               for (String parameter : query.split("&")) {
                  String[] pair = parameter.split("=", 2);
                  if (pair.length == 2 && URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals("chat_id")) {
                     chatId = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                     validateChatId(chatId);
                     break;
                  }
               }
            }
            return new Destination(null, chatId, path.group(1), true);
         }
         return new Destination(uri, "", null, false);
      } catch (IllegalArgumentException invalid) {
         throw new IllegalArgumentException("Invalid webhook URL");
      }
   }

   public String getUrl() {
      Destination target = this.destination;
      return target.telegram() || target.uri() == null ? "" : target.uri().toString();
   }

   private static void validateChatId(String chatId) {
      if (chatId == null || !CHAT_ID.matcher(chatId).matches()) {
         throw new IllegalArgumentException("Invalid Telegram chat ID");
      }
   }

   List<HttpRequest> createRequests(String content) {
      Destination target = this.destination;
      if ((target.telegram() && (target.telegramToken() == null || target.chatId().isBlank()))
         || (!target.telegram() && target.uri() == null) || content == null || content.isEmpty()) return List.of();
      String text = target.telegram() ? ANSI_COLOR.matcher(content).replaceAll("") : content;
      if (text.isEmpty()) return List.of();
      int limit = target.telegram() ? 4096 : 2000;
      List<HttpRequest> requests = new ArrayList<>();
      for (int start = 0; start < text.length();) {
         int end = Math.min(text.length(), start + limit);
         if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1)) && Character.isLowSurrogate(text.charAt(end))) end--;
         JsonObject payload = new JsonObject();
         if (target.telegram()) {
            payload.addProperty("chat_id", target.chatId());
            payload.addProperty("text", text.substring(start, end));
         } else {
            payload.addProperty("content", text.substring(start, end));
         }
         URI destinationUri = target.telegram()
            ? URI.create("https://api.telegram.org/bot" + target.telegramToken() + "/sendMessage")
            : target.uri();
         requests.add(HttpRequest.newBuilder(destinationUri)
            .timeout(java.time.Duration.ofSeconds(30))
            .header("User-Agent", "Schizo")
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(BodyPublishers.ofString(payload.toString())).build());
         start = end;
      }
      return List.copyOf(requests);
   }

   public CompletableFuture<Void> sendContent(String content) {
      List<HttpRequest> requests = this.createRequests(content);
      CompletableFuture<Void> delivery = CompletableFuture.completedFuture(null);
      for (HttpRequest request : requests) {
         delivery = delivery.thenCompose(ignored -> this.sendWithRetry(request, 0));
      }
      return delivery.exceptionallyCompose(failure -> {
         Throwable cause = failure;
         while (cause.getCause() != null
            && (cause instanceof java.util.concurrent.CompletionException
               || cause instanceof java.util.concurrent.ExecutionException)) {
            cause = cause.getCause();
         }
         LOGGER.warn("Webhook notification failed ({})", cause.getClass().getSimpleName());
         return CompletableFuture.failedFuture(cause);
      });
   }

   private CompletableFuture<Void> sendWithRetry(HttpRequest request, int attempt) {
      CompletableFuture<java.net.http.HttpResponse<String>> responseFuture;
      try {
         responseFuture = this.requestSender.send(request);
      } catch (RuntimeException failure) {
         responseFuture = CompletableFuture.failedFuture(failure);
      }
      CompletableFuture<Void> checked = responseFuture.thenCompose(response -> {
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return CompletableFuture.failedFuture(new HttpStatusException(response.statusCode()));
         }
         if ("api.telegram.org".equalsIgnoreCase(request.uri().getHost())) {
            try {
               JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
               if (!result.has("ok") || !result.get("ok").getAsBoolean()) {
                  return CompletableFuture.failedFuture(new PermanentWebhookException("Telegram rejected the notification"));
               }
            } catch (RuntimeException invalidResponse) {
               return CompletableFuture.failedFuture(new PermanentWebhookException("Invalid Telegram response", invalidResponse));
            }
         }
         return CompletableFuture.completedFuture(null);
      });
      return checked.exceptionallyCompose(failure -> {
         Throwable cause = unwrapCompletionFailure(failure);
         if (attempt >= 2 || !isRetryableFailure(cause)) {
            return CompletableFuture.failedFuture(cause);
         }
         long delayMillis = attempt == 0 ? 250L : 750L;
         return CompletableFuture.runAsync(() -> { },
            CompletableFuture.delayedExecutor(
               delayMillis, TimeUnit.MILLISECONDS, ClientAccess.executorService
            ))
            .thenCompose(ignored -> this.sendWithRetry(request, attempt + 1));
      });
   }

   private static Throwable unwrapCompletionFailure(Throwable failure) {
      Throwable cause = failure;
      while (cause.getCause() != null
         && (cause instanceof java.util.concurrent.CompletionException
            || cause instanceof java.util.concurrent.ExecutionException)) {
         cause = cause.getCause();
      }
      return cause;
   }

   private static boolean isRetryableFailure(Throwable failure) {
      if (failure instanceof PermanentWebhookException) {
         return false;
      }
      if (failure instanceof HttpStatusException statusFailure) {
         int status = statusFailure.statusCode();
         return status == 408 || status == 425 || status == 429 || status >= 500 && status <= 599;
      }
      return failure instanceof IOException;
   }

   private static final class PermanentWebhookException extends IOException {
      private PermanentWebhookException(String message) {
         super(message);
      }

      private PermanentWebhookException(String message, Throwable cause) {
         super(message, cause);
      }
   }

   private static final class HttpStatusException extends IOException {
      private final int statusCode;

      private HttpStatusException(int statusCode) {
         super("HTTP " + statusCode);
         this.statusCode = statusCode;
      }

      private int statusCode() {
         return this.statusCode;
      }
   }

}
