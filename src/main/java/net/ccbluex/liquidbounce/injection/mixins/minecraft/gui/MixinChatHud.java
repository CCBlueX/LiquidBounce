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
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat;
import net.ccbluex.liquidbounce.interfaces.ChatHudAddition;
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements ChatHudAddition {

    @Mutable
    @Shadow
    @Final
    public List<ChatHudLine> messages;

    @Mutable
    @Shadow
    @Final
    public List<ChatHudLine.Visible> visibleMessages;

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow
    private int scrolledLines;

    @Shadow
    private boolean hasUnreadNewMessages;

    @Shadow
    public abstract void scroll(int scroll);

    @Shadow
    public abstract int getWidth();

    @Unique
    private int chatY = -1;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void hookNewArrayList2(MinecraftClient client, CallbackInfo ci) {
        messages = new kotlin.collections.ArrayDeque<>(50);
        // ArrayDeque for addFirst operations
        visibleMessages = new kotlin.collections.ArrayDeque<>(50);
    }

    /**
     * Spoofs the message size to be empty to avoid deletion.
     */
    @Redirect(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
    public int hookGetSize2(List<ChatHudLine.Visible> list) {
        var betterChat = ModuleBetterChat.INSTANCE;
        if (betterChat.getRunning() && betterChat.getInfiniteLength()) {
            return -1;
        }

        return list.size();
    }

    /**
     * Cancels the message clearing.
     */
    @Inject(method = "clear", at = @At(value = "HEAD"), cancellable = true)
    public void hookClear(boolean clearHistory, CallbackInfo ci) {
        var betterChat = ModuleBetterChat.INSTANCE;
        if (betterChat.getRunning() && betterChat.getAntiClear() && !betterChat.getAntiChatClearPaused()) {
            ci.cancel();
        }
    }

    /**
     * Modifies {@link ChatHud#addVisibleMessage(ChatHudLine)} so, that the id is
     * forwarded and if {@link ModuleBetterChat} is enabled, older lines won't be removed.
     */
    @Inject(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;isChatFocused()Z", shift = At.Shift.BEFORE), cancellable = true)
    public void hookAddVisibleMessage(ChatHudLine message, CallbackInfo ci, @Local List<OrderedText> list) {
        var focused = isChatFocused();
        var removable = ChatMessageAddition.class.cast(message);
        //noinspection DataFlowIssue
        var id = removable.liquid_bounce$getId();

        for(int j = 0; j < list.size(); ++j) {
            OrderedText orderedText = list.get(j);
            if (focused && scrolledLines > 0) {
                hasUnreadNewMessages = true;
                scroll(1);
            }

            boolean last = j == list.size() - 1;
            //noinspection DataFlowIssue
            ChatHudLine.Visible visible = new ChatHudLine.Visible(message.creationTick(), orderedText, message.indicator(), last);
            ChatMessageAddition.class.cast(visible).liquid_bounce$setId(id);
            visibleMessages.addFirst(visible);
        }

        var betterChat = ModuleBetterChat.INSTANCE;
        if (!betterChat.getRunning() || !betterChat.getInfiniteLength()) {
            if (visibleMessages.size() > 100) {
                visibleMessages.subList(100, visibleMessages.size()).clear();
            }
        }

        ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;getLineHeight()I", ordinal = 0))
    public void hookStoreChatY(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci, @Local(ordinal = 7) int m) {
        this.chatY = m;
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void modifyArgs(
            Args args,
            @Local(ordinal = 1, argsOnly = true) int mouseX,
            @Local(ordinal = 2, argsOnly = true) int mouseY
    ) {
        if(!(ModuleBetterChat.INSTANCE.getRunning() && ModuleBetterChat.Copy.INSTANCE.getRunning() && ModuleBetterChat.Copy.INSTANCE.getHighlight())) {
            return;
        }

        var hovering = mouseX >= 0 && mouseX <= ((int) args.get(2)) -4 &&
                mouseY >= ((int)args.get(1)+1) && mouseY <= ((int)args.get(3));

        if (hovering) {
            args.set(4, 140 << 24);
        }
    }

    @Override
    public int liquidbounce_getChatY() {
        return chatY;
    }
}

