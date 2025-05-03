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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ChatSendEvent;
import net.ccbluex.liquidbounce.event.events.NotificationEvent;
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat;
import net.ccbluex.liquidbounce.interfaces.ChatHudAddition;
import net.ccbluex.liquidbounce.utils.client.ClientChat;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.CharacterVisitor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void hookMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!(ModuleBetterChat.INSTANCE.getRunning() && ModuleBetterChat.Copy.INSTANCE.getRunning())) {
            return;
        }

        int[] activeMessage = getActiveMessage((int)mouseX, (int)mouseY);

        if (activeMessage == null) {
            return;
        }

        ChatHud chatHud = this.client.inGameHud.getChatHud();
        MixinChatHudAccessor accessor = (MixinChatHudAccessor) chatHud;

        var visibleMessages = accessor.getVisibleMessages();
        var messageParts = new kotlin.collections.ArrayDeque<ChatHudLine.Visible>();
        messageParts.add(visibleMessages.get(activeMessage[3]));

        for (int index = activeMessage[3] + 1; index < visibleMessages.size(); index++) {
            if (visibleMessages.get(index).endOfEntry())
                break;

            messageParts.addFirst(visibleMessages.get(index));
        }

        if (messageParts.isEmpty())
            return;

        copyMessage(messageParts, button);
    }

    @Unique
    private void copyMessage(List<ChatHudLine.Visible> messageParts, int button) {
        final StringBuilder builder = new StringBuilder();

        CharacterVisitor visitor = (index, style, codePoint) -> {
            builder.append((char) codePoint);
            return true;
        };

        for (ChatHudLine.Visible line : messageParts) {
            line.content().accept(visitor);
        }

        if (isPressed(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT) && button == GLFW.GLFW_MOUSE_BUTTON_1) {
            client.keyboard.setClipboard(builder.toString());

            if (ModuleBetterChat.Copy.INSTANCE.getNotification()) {
                ClientChat.notification(
                        "ChatCopy",
                        "The line is copied",
                        NotificationEvent.Severity.SUCCESS
                );
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (client.currentScreen instanceof ChatScreen chat) {
                ((MixinChatScreenAccessor) chat).getChatField().setText(builder.toString());
            }
        }
    }

    @Unique
    private boolean isPressed(int... keys) {
        for (int key : keys) {
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), key) == GLFW.GLFW_PRESS) {
                return true;
            }
        }

        return false;
    }

    // [0] - y,
    // [1] - width,
    // [2] - height,
    // [3] - (message) index
    @Unique
    private int @Nullable [] getActiveMessage(int mouseX, int mouseY) {
        ChatHud chatHud = this.client.inGameHud.getChatHud();
        MixinChatHudAccessor accessor = (MixinChatHudAccessor) chatHud;
        ChatHudAddition addition = (ChatHudAddition) chatHud;

        float chatScale = (float) chatHud.getChatScale();
        int chatLineY = (int) accessor.invokeToChatLineY(mouseY);
        int messageIndex = accessor.invokeGetMessageIndex(0, chatLineY);
        int buttonX = (int) (chatHud.getWidth() + 14 * chatScale);

        if (messageIndex == -1 || mouseX > buttonX + 14 * chatScale)
            return null;

        int chatY = addition.liquidbounce_getChatY();

        int buttonSize = (int) (9 * chatScale);
        int lineHeight = accessor.invokeGetLineHeight();
        int scaledButtonY = chatY - (chatLineY + 1) * lineHeight + (int) Math.ceil((lineHeight - 9) / 2.0);
        float buttonY = scaledButtonY * chatScale;

        boolean hovering = mouseX >= 0 && mouseX <= buttonX && mouseY >= buttonY && mouseY <= buttonY + buttonSize;

        if (hovering) {
            return new int[]{(int) buttonY, buttonX, buttonSize, messageIndex};
        } else {
            return null;
        }
    }
}

