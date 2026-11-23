package dev.schizo.client.commands;

import dev.schizo.client.profiling.EventProfiler;
import dev.schizo.client.utils.chat.ChatUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public final class ProfilerCommand extends Command {
   private static final int RESULT_LIMIT = 10;

   public ProfilerCommand() {
      super("profiler");
      this.setAliases("prof");
   }

   @Override
   public void exec(LiteralArgumentBuilder commandBuilder) {
      EventProfiler profiler = EventProfiler.GLOBAL;
      commandBuilder.then(literal("start").executes(context -> {
         profiler.start();
         send("Profiler enabled.");
         return 1;
      }));
      commandBuilder.then(literal("stop").executes(context -> {
         profiler.stop();
         send("Profiler stopped.");
         return 1;
      }));
      commandBuilder.then(literal("reset").executes(context -> {
         profiler.reset();
         send("Profiler statistics reset.");
         return 1;
      }));
      commandBuilder.then(literal("callbacks").executes(context -> {
         send("Slowest event callbacks (inclusive):");
         List<EventProfiler.CallbackSnapshot> results = profiler.topCallbacks(RESULT_LIMIT);
         if (results.isEmpty()) send("No callback samples yet.");
         for (int i = 0; i < results.size(); i++) {
            EventProfiler.CallbackSnapshot row = results.get(i);
            send(format(i + 1, row.label(), row.invocationCount(), row.totalNanos(),
               row.averageNanos(), row.maximumNanos()));
         }
         return 1;
      }));
      commandBuilder.then(literal("modules").executes(context -> {
         send("Slowest modules by event callback time (inclusive):");
         List<EventProfiler.ModuleSnapshot> results = profiler.topModules(RESULT_LIMIT);
         if (results.isEmpty()) send("No module samples yet.");
         for (int i = 0; i < results.size(); i++) {
            EventProfiler.ModuleSnapshot row = results.get(i);
            send(format(i + 1, row.moduleName(), row.invocationCount(), row.totalNanos(),
               row.averageNanos(), row.maximumNanos()));
         }
         return 1;
      }));
      commandBuilder.then(literal("hud").executes(context -> {
         send("Slowest direct HUD widget renders:");
         List<EventProfiler.HudSnapshot> results = profiler.topHudWidgets(RESULT_LIMIT);
         if (results.isEmpty()) send("No HUD render samples yet.");
         for (int i = 0; i < results.size(); i++) {
            EventProfiler.HudSnapshot row = results.get(i);
            send(format(i + 1, row.widgetName(), row.invocationCount(), row.totalNanos(),
               row.averageNanos(), row.maximumNanos()));
         }
         return 1;
      }));
   }

   private static String format(int rank, String name, long count, long total, long average, long maximum) {
      return String.format(Locale.ROOT, "%d. %s | calls %d | total %s ms | avg %s ms | max %s ms",
         rank, name, count, milliseconds(total), milliseconds(average), milliseconds(maximum));
   }

   private static String milliseconds(long nanos) {
      return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0);
   }

   private static void send(String message) {
      ChatUtils.printChat(Component.literal(message), ChatUtils.DEFAULT_SIGNATURE);
   }
}
