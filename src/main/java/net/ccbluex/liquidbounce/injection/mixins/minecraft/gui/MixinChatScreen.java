/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatSendEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends MixinScreen {

    /**
     * We want to close the screen before sending the message to make sure it doesn't affect commands.
     */
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatScreen;sendMessage(Ljava/lang/String;Z)V", shift = At.Shift.BEFORE))
    private void fixOrder(CallbackInfoReturnable<Boolean> callbackInfo) {
        this.client.setScreen(null);
    }

    /**
     * Handle user chat messages
     *
     * @param chatText chat message by client user
     */
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void handleChatMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        ChatSendEvent chatSendEvent = new ChatSendEvent(chatText);

        EventManager.INSTANCE.callEvent(chatSendEvent);

        if (chatSendEvent.isCancelled()) {
            client.inGameHud.getChatHud().addToMessageHistory(chatText);
            ci.cancel();
        }
    }

}
