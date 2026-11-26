package dev.schizo.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.gui.screens.ExpeditionScreen;

public final class ExpeditionCommand extends Command {
   public ExpeditionCommand() {
      super("expedition");
      this.setAliases("travel", "discoveries");
   }

   @Override
   public void exec(LiteralArgumentBuilder root) {
      root.executes(context -> {
         ClientAccess.schizoMinecraft.execute(() -> ClientAccess.schizoMinecraft.setScreen(new ExpeditionScreen()));
         return 1;
      });
   }
}
