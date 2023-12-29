package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.network.message;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;

@Mixin(MessageHandler.class)
public class MixinMessageHandler {

    @Inject(method = "method_45745", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;)V"))
    private void injectDisguisedChatLambda(MessageType.Parameters parameters, Text text, Instant instant, CallbackInfoReturnable<Boolean> cir) {
        emitChatEvent(text, ChatReceiveEvent.ChatType.DISGUISED_CHAT_MESSAGE);
    }

    @Inject(method = "processChatMessageInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"))
    private void injectChatMessage(MessageType.Parameters params, SignedMessage message, Text decorated, GameProfile sender, boolean onlyShowSecureChat, Instant receptionTimestamp, CallbackInfoReturnable<Boolean> cir) {
        emitChatEvent(decorated, ChatReceiveEvent.ChatType.CHAT_MESSAGE);
    }

    @Inject(method = "onGameMessage", at = @At(value = "HEAD"))
    private void injectGameMessage(Text message, boolean overlay, CallbackInfo ci) {
        emitChatEvent(message, ChatReceiveEvent.ChatType.GAME_MESSAGE);
    }

    private void emitChatEvent(Text text, ChatReceiveEvent.ChatType type) {
        EventManager.INSTANCE.callEvent(new ChatReceiveEvent(text.getString(), text, type));
    }
}
