/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;


import dev.schizo.client.SchizoClient;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.LocalPlayerTickEvent;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.render.ColorUtils;
import dev.schizo.client.modules.HudWidgetModule;
import dev.schizo.client.settings.EnumSetting;
import dev.schizo.client.settings.NamedEntry;
import dev.schizo.client.settings.Setting;
import dev.schizo.client.settings.StringSetting;
import dev.schizo.client.utils.math.MathUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CryptoModule extends HudWidgetModule {
   private final Setting<String> coinSetting;
   private final Setting<FiatCurrency> fiatSetting;
   private final HttpClient httpClient;
   private final ElapsedTimer updateTimer;
   private final FloatAnimation priceChangeAnimation;
   private boolean priceIncreased;
   private String formattedPrice;
   private String coinSlug;
   private final AtomicBoolean refreshInProgress = new AtomicBoolean();
   private long refreshGeneration;

   private void schedulePriceRefresh() {
      String coinSymbol = this.coinSetting.getValue().toUpperCase(Locale.ROOT);
      if (!isCoinSymbolValid(coinSymbol)) {
         this.coinSlug = null;
         this.formattedPrice = "UNKNOWN";
         this.priceChangeAnimation.setValue(0.0F);
         return;
      }

      if (!this.refreshInProgress.compareAndSet(false, true)) {
         return;
      }

      FiatCurrency fiatCurrency = this.fiatSetting.getValue();
      String fiatSymbol = fiatCurrency.getName().toUpperCase(Locale.ROOT);
      String cachedCoinSlug = this.coinSlug;
      long generation = this.refreshGeneration;
      ExecutorService executor = ClientAccess.executorService;
      try {
         executor.submit(() -> {
            PriceFetchResult result = null;
            try {
               result = this.fetchPrice(coinSymbol, fiatSymbol, cachedCoinSlug);
            } catch (Exception ignoredException) {
            } finally {
               this.refreshInProgress.set(false);
            }

            PriceFetchResult completed = result;
            if (completed != null) {
               ClientAccess.schizoMinecraft.executeIfPossible(() -> {
                  if (this.isToggled() && this.refreshGeneration == generation) {
                     this.applyPriceResult(completed);
                  }
               });
            }
         });
      } catch (java.util.concurrent.RejectedExecutionException rejected) {
         this.refreshInProgress.set(false);
      }
   }

   private PriceFetchResult fetchPrice(String coinSymbol, String fiatSymbol, String cachedCoinSlug) throws Exception {
      if (cachedCoinSlug == null) {
         URI uri = new URI("https://api.alternative.me/v2/ticker/?convert=%s".formatted(fiatSymbol));
         JsonObject prices = this.requestPrices(uri);
         if (prices == null || prices.isEmpty()) {
            return PriceFetchResult.unknown(null);
         }

         for (String key : prices.keySet()) {
            if (!prices.get(key).isJsonObject()) {
               continue;
            }
            JsonObject coin = prices.get(key).getAsJsonObject();
            if (!coin.has("symbol") || !coin.has("website_slug") || !coin.has("quotes")) {
               continue;
            }
            if (!coinSymbol.equalsIgnoreCase(coin.get("symbol").getAsString())) {
               continue;
            }

            JsonObject quotes = coin.getAsJsonObject("quotes");
            if (!quotes.has(fiatSymbol) || !quotes.get(fiatSymbol).isJsonObject()) {
               return PriceFetchResult.unknown(null);
            }
            JsonObject quote = quotes.getAsJsonObject(fiatSymbol);
            if (!quote.has("price")) {
               return PriceFetchResult.unknown(null);
            }
            return PriceFetchResult.quote(
               quote.get("price").getAsFloat(),
               coin.get("website_slug").getAsString()
            );
         }
         return PriceFetchResult.unknown(null);
      }

      String conversionUrl = "https://api.alternative.me/v2/ticker/%s/?convert=%s";
      URI uri = new URI(conversionUrl.formatted(cachedCoinSlug, fiatSymbol));
      JsonObject prices = this.requestPrices(uri);
      if (prices == null || prices.isEmpty()) {
         return PriceFetchResult.unknown(cachedCoinSlug);
      }
      for (String key : prices.keySet()) {
         if (!prices.get(key).isJsonObject()) {
            continue;
         }
         JsonObject coin = prices.get(key).getAsJsonObject();
         if (!coin.has("quotes")) {
            continue;
         }
         JsonObject quotes = coin.getAsJsonObject("quotes");
         if (!quotes.has(fiatSymbol) || !quotes.get(fiatSymbol).isJsonObject()) {
            continue;
         }
         JsonObject quote = quotes.getAsJsonObject(fiatSymbol);
         if (quote.has("price")) {
            return PriceFetchResult.quote(quote.get("price").getAsFloat(), cachedCoinSlug);
         }
      }
      return PriceFetchResult.unknown(cachedCoinSlug);
   }

   private void applyPriceResult(PriceFetchResult result) {
      this.coinSlug = result.coinSlug();
      if (result.unknown()) {
         this.formattedPrice = "UNKNOWN";
         this.priceChangeAnimation.setValue(0.0F);
         return;
      }
      this.updateQuotedPrice(result.price());
   }

   private JsonObject requestPrices(URI uri) {
      HttpRequest request = HttpRequest.newBuilder().GET().uri(uri)
         .header("Accept", "application/json")
         .header("User-Agent", SchizoClient.USER_AGENT)
         .build();
      HttpResponse<String> response = this.httpClient.sendAsync(request, BodyHandlers.ofString())
         .orTimeout(15L, TimeUnit.SECONDS).join();
      if (response.statusCode() != 200) {
         return null;
      }

      JsonObject responseJson = ClientAccess.gson.fromJson(response.body(), JsonObject.class);
      if (responseJson == null || !responseJson.has("data") || !responseJson.has("metadata")) {
         return null;
      }
      JsonObject metadata = responseJson.getAsJsonObject("metadata");
      if (metadata.has("error") && !metadata.get("error").isJsonNull()) {
         return null;
      }
      return responseJson.getAsJsonObject("data");
   }

   private void updateQuotedPrice(float quotedPrice) {
      String priceText = "%.2f".formatted(quotedPrice);
      float roundedPrice = Float.parseFloat(priceText);
      if (!this.formattedPrice.equalsIgnoreCase("0.00")
         && !this.formattedPrice.equalsIgnoreCase("UNKNOWN")) {

         this.updatePriceChangeAnimation(roundedPrice);
      }

      this.formattedPrice = priceText;
   }

   private static boolean isCoinSymbolValid(String coinSymbol) {
      return coinSymbol != null && !coinSymbol.isEmpty() && coinSymbol.matches("[a-zA-Z]+");
   }

   private Component getDisplayText() {
      MutableComponent text = Component.empty();
      String coinSymbol = this.coinSetting.getValue();
      String displayCoinSymbol = coinSymbol.toUpperCase();
      text.append(displayCoinSymbol);
      text.append(" ");
      int priceColor;
      if (this.priceIncreased) {
         Color gainColor = Color.GREEN;
         Color neutralColor = Color.WHITE;
         FloatAnimation gainAnimation = this.priceChangeAnimation;

         Color gainBlendedColor = ColorUtils.blend(
            gainColor, neutralColor, gainAnimation.getValue()
         );
         priceColor = gainBlendedColor.hashCode();
      } else {
         Color lossColor = Color.RED;
         Color lossNeutralColor = Color.WHITE;
         FloatAnimation lossAnimation = this.priceChangeAnimation;

         Color lossBlendedColor = ColorUtils.blend(
            lossColor, lossNeutralColor, lossAnimation.getValue()
         );
         priceColor = lossBlendedColor.hashCode();
      }

      String priceText = this.formattedPrice;
      MutableComponent priceComponent = Component.literal(priceText);
      MutableComponent coloredPrice = priceComponent.withColor(priceColor);
      text.append(coloredPrice);
      return text;
   }

   @EventHandler
   private void onLocalPlayerTickEvent(LocalPlayerTickEvent ignoredEvent) {
      if (this.updateTimer.hasElapsed(5.0D, TimeUnit.SECONDS)) {
         this.updateTimer.reset();
         this.schedulePriceRefresh();
      }
   }

   @Override
   public void onDisable() {
      this.refreshGeneration++;
   }

   private void updatePriceChangeAnimation(float price) {
      String previousPriceText = this.formattedPrice;
      float parsedPreviousPrice = Float.parseFloat(previousPriceText);

      float comparisonPrice = MathUtils.floorToDecimals(parsedPreviousPrice, 2);
      float priceDifference = price - comparisonPrice;
      double absoluteDifference = Math.abs(priceDifference);

      if (absoluteDifference < 0.01D) {
         FloatAnimation unchangedAnimation = this.priceChangeAnimation;

         unchangedAnimation.setValue(0.0F);
         return;
      }

      this.priceIncreased = price > comparisonPrice;
      if (comparisonPrice != price) {
         FloatAnimation changeAnimation = this.priceChangeAnimation;

         float changeProgress = 1.0F;

         changeAnimation.setValue(changeProgress);
         FloatAnimation fadeAnimation = this.priceChangeAnimation;

         fadeAnimation.setTarget(0.0F, 1000L);
      }
   }

   public CryptoModule() {
      super("Crypto", new String[0]);

      this.coinSetting = this.add(new StringSetting("Coin", "BTC"));

      this.fiatSetting = this.add(new EnumSetting<>("Fiat", CryptoModule.FiatCurrency.USD));
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
      this.updateTimer = new ElapsedTimer();
      this.priceChangeAnimation = new FloatAnimation();

      this.formattedPrice = "0.00";
      this.coinSlug = null;
      this.setRenderer(
         new HudTextListRenderer(
            this, new HudTextRow(this::getDisplayText, () -> true)
         )
      );
      this.coinSetting.setChangeCallback(() -> {
         this.refreshGeneration++;
         this.coinSlug = null;
         this.priceChangeAnimation.setValue(0.0F);
         this.formattedPrice = "0.00";

         String coinSymbol = this.coinSetting.getValue();
         if (!isCoinSymbolValid(coinSymbol)) {
            if (!"UNKNOWN".equals(coinSymbol)) {
               this.coinSetting.setValue("UNKNOWN");
            }
         } else {
            this.updateTimer.setTime(5000L);
         }
      });
      this.fiatSetting.setChangeCallback(() -> {
         this.refreshGeneration++;
         this.priceChangeAnimation.setValue(0.0F);
         this.formattedPrice = "0.00";
         this.updateTimer.setTime(5000L);
      });
   }

   private record PriceFetchResult(float price, String coinSlug, boolean unknown) {
      static PriceFetchResult quote(float price, String coinSlug) {
         return new PriceFetchResult(price, coinSlug, false);
      }

      static PriceFetchResult unknown(String coinSlug) {
         return new PriceFetchResult(0.0F, coinSlug, true);
      }
   }

   private enum FiatCurrency implements NamedEntry {
      USD("USD"),
      EUR("EUR"),
      RUB("RUB"),
      CNY("CNY"),
      TRY("TRY"),
      JPY("JPY"),
      PLN("PLN"),
      CHF("CHF"),
      GBP("GBP"),
      CAD("CAD"),
      KRW("KRW"),
      BRL("BRL");

      private final String displayName;

      @Override
      public String getName() {
         return this.displayName;
      }

      FiatCurrency(String displayName) {
         this.displayName = displayName;
      }

   }
}
