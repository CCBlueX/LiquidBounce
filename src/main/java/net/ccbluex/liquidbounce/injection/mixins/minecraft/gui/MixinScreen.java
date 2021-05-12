/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.event.ChatSendEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @Shadow @Nullable
    protected MinecraftClient client;

    @Shadow public abstract void sendMessage(String message);

    /**
     * Handle user chat messages
     *
     * @param message chat message by client user
     * @param callbackInfo callback
     */
    @Inject(method = "sendMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void handleChatMessage(String message, CallbackInfo callbackInfo) {
        ChatSendEvent chatSendEvent = new ChatSendEvent(message);

        EventManager.INSTANCE.callEvent(chatSendEvent);

        if (chatSendEvent.isCancelled()) {
            client.inGameHud.getChatHud().addToMessageHistory(message);
            callbackInfo.cancel();
        }
    }

}
