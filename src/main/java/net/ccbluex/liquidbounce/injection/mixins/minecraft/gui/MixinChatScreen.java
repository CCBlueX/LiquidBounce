package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatSendEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends MixinScreen {

    /**
     * We want to close the screen before sending the message to make sure it doesn't affect commands.
     */
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatScreen;sendMessage(Ljava/lang/String;Z)Z", shift = At.Shift.BEFORE))
    private void fixOrder(CallbackInfoReturnable<Boolean> callbackInfo) {
        this.client.setScreen(null);
    }

    /**
     * Handle user chat messages
     *
     * @param chatText chat message by client user
     * @param callbackInfo callback
     */
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void handleChatMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> callbackInfo) {
        ChatSendEvent chatSendEvent = new ChatSendEvent(chatText);

        EventManager.INSTANCE.callEvent(chatSendEvent);

        if (chatSendEvent.isCancelled()) {
            client.inGameHud.getChatHud().addToMessageHistory(chatText);
            callbackInfo.cancel();
        }
    }

}
