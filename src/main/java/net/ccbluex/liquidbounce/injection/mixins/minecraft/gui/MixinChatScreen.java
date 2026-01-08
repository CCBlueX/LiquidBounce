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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatSendEvent;
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ArrayListDeque;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends MixinScreen {

    /**
     * Handle user chat messages
     *
     * @param chatText chat message by client user
     */
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void handleChatMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (EventManager.INSTANCE.callEvent(new ChatSendEvent(chatText)).isCancelled()) {
            minecraft.gui.getChat().addRecentChat(chatText);
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void hookMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!(ModuleBetterChat.INSTANCE.getRunning() && ModuleBetterChat.Copy.INSTANCE.getRunning())) {
            return;
        }

        int[] activeMessage = getActiveMessage(click);

        if (activeMessage == null) {
            return;
        }

        var chatHud = (MixinChatComponentAccessor) this.minecraft.gui.getChat();

        var visibleMessages = chatHud.getTrimmedMessages();
        var messageParts = new ArrayListDeque<GuiMessage.Line>();
        messageParts.add(visibleMessages.get(activeMessage[3]));

        for (int index = activeMessage[3] + 1; index < visibleMessages.size(); index++) {
            if (visibleMessages.get(index).endOfEntry())
                break;

            messageParts.addFirst(visibleMessages.get(index));
        }

        if (messageParts.isEmpty())
            return;

        ModuleBetterChat.Copy.copyMessage(messageParts, click.button());
    }

    // [0] - y,
    // [1] - width,
    // [2] - height,
    // [3] - (message) index
    @Unique
    private int @Nullable [] getActiveMessage(MouseButtonEvent click) {
        return null;
//        var chatHud = (MixinChatHudAccessor & ChatHudAddition) this.client.inGameHud.getChatHud();
//
//        float chatScale = (float) chatHud.getChatScale();
//        int chatLineY = 0; // (int) chatHud.invokeToChatLineY(mouseY); FIXME(1.21.11)
//        int messageIndex = -1; // chatHud.invokeGetMessageIndex(0, chatLineY);
//        int buttonX = (int) (chatHud.getWidth() + 14 * chatScale);
//
//        if (messageIndex == -1 || click.x() > buttonX + 14 * chatScale)
//            return null;
//
//        int chatY = chatHud.liquidbounce_getChatY();
//
//        int buttonSize = (int) (9 * chatScale);
//        int lineHeight = chatHud.invokeGetLineHeight();
//        int scaledButtonY = chatY - (chatLineY + 1) * lineHeight + (int) Math.ceil((lineHeight - 9) / 2.0);
//        float buttonY = scaledButtonY * chatScale;
//
//        boolean hovering = click.x() >= 0 && click.x() <= buttonX && click.y() >= buttonY && click.y() <= buttonY + buttonSize;
//
//        if (hovering) {
//            return new int[]{(int) buttonY, buttonX, buttonSize, messageIndex};
//        } else {
//            return null;
//        }
    }
}

