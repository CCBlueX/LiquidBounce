/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.network.message;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;

@SuppressWarnings("CancellableInjectionUsage")
@Mixin(MessageHandler.class)
public class MixinMessageHandler {

    @Shadow
    private long lastProcessTime;

    @Inject(method = "method_45745", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/message/MessageType$Parameters;applyChatDecoration(Lnet/minecraft/text/Text;)Lnet/minecraft/text/Text;", shift = At.Shift.BEFORE), cancellable = true)
    private void injectDisguisedChatLambda(MessageType.Parameters parameters, Text text, Instant instant, CallbackInfoReturnable<Boolean> cir) {
        var result = liquid_bounce$emitChatEvent(parameters, text, ChatReceiveEvent.ChatType.DISGUISED_CHAT_MESSAGE);
        if (result) {
            lastProcessTime = Util.getMeasuringTimeMs();
            cir.cancel();
        }
    }

    @Inject(method = "processChatMessageInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true)
    private void injectChatMessage1(MessageType.Parameters parameters, SignedMessage message, Text decorated, GameProfile sender, boolean onlyShowSecureChat, Instant receptionTimestamp, CallbackInfoReturnable<Boolean> cir) {
        var result = liquid_bounce$emitChatEvent(null, decorated, ChatReceiveEvent.ChatType.CHAT_MESSAGE);
        if (result) {
            lastProcessTime = Util.getMeasuringTimeMs();
            cir.cancel();
        }
    }

    @Inject(method = "processChatMessageInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    private void injectChatMessage2(MessageType.Parameters parameters, SignedMessage message, Text decorated, GameProfile sender, boolean onlyShowSecureChat, Instant receptionTimestamp, CallbackInfoReturnable<Boolean> cir) {
        var result = liquid_bounce$emitChatEvent(parameters, decorated, ChatReceiveEvent.ChatType.CHAT_MESSAGE);
        if (result) {
            lastProcessTime = Util.getMeasuringTimeMs();
            cir.cancel();
        }
    }

    @Inject(method = "onGameMessage", at = @At(value = "HEAD"), cancellable = true)
    private void injectGameMessage(Text message, boolean overlay, CallbackInfo ci) {
        var result = liquid_bounce$emitChatEvent(null, message, overlay ? ChatReceiveEvent.ChatType.DISGUISED_CHAT_MESSAGE : ChatReceiveEvent.ChatType.GAME_MESSAGE);
        if (result) {
            lastProcessTime = Util.getMeasuringTimeMs();
            ci.cancel();
        }
    }

    @Unique
    private boolean liquid_bounce$emitChatEvent(MessageType.Parameters parameters, Text text, ChatReceiveEvent.ChatType type) {
        var event = new ChatReceiveEvent(text.getString(), text, type, inputText -> {
            if (parameters != null) {
                return parameters.applyChatDecoration(text);
            } else {
                return text;
            }
        });
        EventManager.INSTANCE.callEvent(event);
        return event.isCancelled();
    }

}
