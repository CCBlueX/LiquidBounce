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
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.ArrayListDeque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

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
            minecraft.gui.hud.getChat().addRecentChat(chatText);
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void hookMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!(ModuleBetterChat.INSTANCE.getRunning() && ModuleBetterChat.Copy.INSTANCE.getRunning())) {
            return;
        }

        var activeMessage = getActiveMessage(click);

        if (activeMessage.isEmpty()) {
            return;
        }

        var chatHud = (MixinChatComponentAccessor) this.minecraft.gui.hud.getChat();

        var visibleMessages = chatHud.getTrimmedMessages();
        var messageBounds = ModuleBetterChat.resolveMessageBounds(visibleMessages, activeMessage.getAsInt());
        var messageParts = new ArrayListDeque<GuiMessage.Line>(messageBounds.getEndInclusive() - messageBounds.getStart() + 1);
        for (int index = messageBounds.getEndInclusive(); index >= messageBounds.getStart(); index--) {
            messageParts.addLast(visibleMessages.get(index));
        }

        if (messageParts.isEmpty())
            return;

        ModuleBetterChat.Copy.copyMessage(messageParts, click.button());
    }

    @Unique
    private OptionalInt getActiveMessage(MouseButtonEvent click) {
        var chatHud = (MixinChatComponentAccessor) this.minecraft.gui.hud.getChat();
        var visibleMessages = chatHud.getTrimmedMessages();
        if (visibleMessages.isEmpty()) {
            return OptionalInt.empty();
        }

        double chatScale = chatHud.invokeGetScale();
        if (chatScale <= 0.0) {
            return OptionalInt.empty();
        }

        int chatWidth = (int) Math.ceil(chatHud.invokeGetWidth() / chatScale);
        double localMouseX = click.x() / chatScale - 4.0;
        if (localMouseX < 0.0 || localMouseX > chatWidth) {
            return OptionalInt.empty();
        }

        int lineHeight = chatHud.invokeGetLineHeight();
        if (lineHeight <= 0) {
            return OptionalInt.empty();
        }

        int guiHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int chatBottom = (int) Math.floor((guiHeight - 40) / chatScale);
        double localMouseY = chatBottom - click.y() / chatScale;
        if (localMouseY < 0.0) {
            return OptionalInt.empty();
        }

        int lineIndex = (int) Math.floor(localMouseY / lineHeight);
        int visibleLineCount = Math.min(chatHud.invokeGetLinesPerPage(), visibleMessages.size() - chatHud.getChatScrollbarPos());
        if (lineIndex < 0 || lineIndex >= visibleLineCount) {
            return OptionalInt.empty();
        }

        int messageIndex = lineIndex + chatHud.getChatScrollbarPos();
        return messageIndex >= 0 && messageIndex < visibleMessages.size() ? OptionalInt.of(messageIndex) : OptionalInt.empty();
    }
}
