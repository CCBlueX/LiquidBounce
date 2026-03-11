/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.network.message;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
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
@Mixin(ChatListener.class)
public abstract class MixinChatListener {

    @Shadow
    private long previousMessageTime;

    @Inject(method = "method_45745", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ChatType$Bound;decorate(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;", shift = At.Shift.BEFORE), cancellable = true)
    private void injectDisguisedChatLambda(ChatType.Bound parameters, Component text, Instant instant, CallbackInfoReturnable<Boolean> cir) {
        var result = liquid_bounce$emitChatEvent(parameters, text, ChatReceiveEvent.ChatType.DISGUISED_CHAT_MESSAGE);
        if (result) {
            previousMessageTime = Util.getMillis();
            cir.cancel();
        }
    }

    @Inject(method = "showMessageToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true)
    private void injectChatMessage1(ChatType.Bound parameters, PlayerChatMessage message, Component decorated, GameProfile sender, boolean onlyShowSecureChat, Instant receptionTimestamp, CallbackInfoReturnable<Boolean> cir) {
        var result = liquid_bounce$emitChatEvent(null, decorated, ChatReceiveEvent.ChatType.CHAT_MESSAGE);
        if (result) {
            previousMessageTime = Util.getMillis();
            cir.cancel();
        }
    }

    @Inject(method = "showMessageToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    private void injectChatMessage2(ChatType.Bound parameters, PlayerChatMessage message, Component decorated, GameProfile sender, boolean onlyShowSecureChat, Instant receptionTimestamp, CallbackInfoReturnable<Boolean> cir) {
        var result = liquid_bounce$emitChatEvent(parameters, decorated, ChatReceiveEvent.ChatType.CHAT_MESSAGE);
        if (result) {
            previousMessageTime = Util.getMillis();
            cir.cancel();
        }
    }

    @Inject(method = "handleSystemMessage", at = @At(value = "HEAD"), cancellable = true)
    private void injectGameMessage(Component message, boolean overlay, CallbackInfo ci) {
        var result = liquid_bounce$emitChatEvent(null, message, overlay ? ChatReceiveEvent.ChatType.DISGUISED_CHAT_MESSAGE : ChatReceiveEvent.ChatType.GAME_MESSAGE);
        if (result) {
            previousMessageTime = Util.getMillis();
            ci.cancel();
        }
    }

    @Unique
    private boolean liquid_bounce$emitChatEvent(ChatType.Bound parameters, Component text, ChatReceiveEvent.ChatType type) {
        var event = new ChatReceiveEvent(text.getString(), text, type, inputText -> {
            if (parameters != null) {
                return parameters.decorate(text);
            } else {
                return text;
            }
        });
        EventManager.INSTANCE.callEvent(event);
        return event.isCancelled();
    }

}
