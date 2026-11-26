/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.modules.misc.AutoReconnectModule;
import dev.schizo.client.utils.MonotonicCountdown;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.client.UIModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {
   @Unique
   private long reconnectTimerStartNanos;
   @Unique
   private boolean reconnectTimerStarted;
   @Unique
   private boolean reconnectStarted;
   @Unique
   private ServerData reconnectServer;
   @Shadow
   @Final
   private DisconnectionDetails details;
   @Shadow
   @Final
   private LinearLayout layout;
   @Unique
   private Button reconnectButton;
   @Unique
   private Button autoReconnectButton;

   protected DisconnectedScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "init()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DisconnectedScreen;repositionElements()V", shift = Shift.BEFORE))
   private void addReconnectButtons(CallbackInfo callback) {
      if (!this.reconnectTimerStarted) {
         this.reconnectTimerStartNanos = System.nanoTime();
         this.reconnectTimerStarted = true;
      }
      AutoReconnectModule autoReconnectModule = ClientContext.getModule(AutoReconnectModule.class);
      if (autoReconnectModule == null || ClientContext.serverStateManager == null) {
         return;
      }
      this.reconnectServer = ClientContext.serverStateManager.getServerData();
      autoReconnectModule.attempts.select(this.reconnectServer == null ? null : this.reconnectServer.ip);
      Builder reconnectBuilder = Button.builder(
            Component.literal("Reconnect"),
            button -> {
               ServerData serverData = this.reconnectServer;
               if (serverData != null) {
                  ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), this.minecraft, ServerAddress.parseString(serverData.ip), serverData, true, null);
               }
            }
         )
         .width(200);
      Builder autoReconnectBuilder = Button.builder(
            Component.literal("AutoReconnect"),
            button -> {
               if (!autoReconnectModule.isToggled()) {
                  ClientContext.serverStateManager.autoLogged = false;
               }

               autoReconnectModule.toggle();
               this.reconnectTimerStartNanos = System.nanoTime();
               button.setFocused(false);
            }
         )
         .width(200);
      this.reconnectButton = reconnectBuilder.build();
      this.autoReconnectButton = autoReconnectBuilder.build();
      if (ClientContext.serverStateManager.getServerData()
         != null) {
         this.addRenderableWidget(this.reconnectButton);
         this.addRenderableWidget(this.autoReconnectButton);
      }
   }

   @Inject(method = "repositionElements", at = @At("TAIL"))
   private void repositionReconnectButtons(CallbackInfo callback) {
      if (this.reconnectButton != null) {
         int buttonX = this.width
               / (
                  2
               )
            - 100;
         this.reconnectButton
            .setPosition(
               buttonX,
               Math.min(
                     this.height
                           / 2
                        + this.layout.getHeight()
                           / 2,
                     this.height
                        - 30
                  )
                  - 1
            );
         this.autoReconnectButton
            .setPosition(
               buttonX,
               Math.min(
                     this.height
                           / 2
                        + this.layout.getHeight()
                           / 2,
                     this.height
                        - 30
                  )
                  + 20
            );
      }
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      UIModule uiModule = ClientContext.getModule(UIModule.class);
      if (uiModule != null
         && keyEvent.key() == uiModule.getKeybind().code()
         && !(this.getFocused() instanceof EditBox)) {
         uiModule.enable();
         ClientContext.getClickGui().open();
         return false;
      }
      return super.keyPressed(keyEvent);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.autoReconnectButton == null || ClientContext.getModule(AutoReconnectModule.class) == null
            || ClientContext.serverStateManager == null) {
         super.render(graphics, mouseX, mouseY, partialTick);
         return;
      }
      this.tryAutoReconnect();
      if (this.minecraft.screen != this) return;
      this.autoReconnectButton.setMessage(this.getAutoReconnectButtonMessage());
      this.autoReconnectButton.setFocused(false);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void tryAutoReconnect() {
      AutoReconnectModule module = ClientContext.getModule(AutoReconnectModule.class);
      if (module == null || !module.isToggled() || this.reconnectStarted
         || this.minecraft.screen != this || this.reconnectServer == null
         || ClientContext.serverStateManager.autoLogged
         || ClientContext.serverStateManager.getServerData() != this.reconnectServer) {
         return;
      }
      if (this.getReconnectSecondsRemaining(module) <= 0.0F
         && module.attempts.claim(module.attemptsSetting.getValue())) {
         this.reconnectStarted = true;
         ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), this.minecraft,
            ServerAddress.parseString(this.reconnectServer.ip), this.reconnectServer, true, null);
      }
   }

   private Component getAutoReconnectButtonMessage() {
      MutableComponent message = Component.literal("AutoReconnect ");
      AutoReconnectModule autoReconnectModule = ClientContext.getModule(AutoReconnectModule.class);
      if (autoReconnectModule == null || ClientContext.serverStateManager == null) {
         return message.append(ChatFormatting.RED + "OFF");
      }
      boolean showCountdown = autoReconnectModule.isToggled();
      if (autoReconnectModule.attempts.exhausted(autoReconnectModule.attemptsSetting.getValue())) {
         return message.append(ChatFormatting.RED + "Retry limit");
      }
      float remainingSeconds = this.getReconnectSecondsRemaining(
         autoReconnectModule
      );
      if (ClientContext.serverStateManager.autoLogged
         )
       {
         return message.append(ChatFormatting.RED + "AutoLogged");
      } else {
         return showCountdown ? message.append(ChatFormatting.GREEN + "%.1fs".formatted(remainingSeconds)) : message.append(ChatFormatting.RED + "OFF");
      }
   }

   private float getReconnectSecondsRemaining(AutoReconnectModule module) {
      if (module == null) {
         return 0.1F;
      }

      float delaySeconds = module.secondsSetting.getValue()
         + module.minutesSetting.getValue()
            * 60;
      double delay = module.attempts.delaySeconds(delaySeconds, module.backoffSetting.getValue());
      return MonotonicCountdown.remainingSeconds(this.reconnectTimerStartNanos, System.nanoTime(), (float)delay);
   }
}
