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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.spoofer.SpooferClient;
import net.ccbluex.liquidbounce.utils.text.PlainText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class MixinClientHandshakePacketListenerImplMixin {

    @ModifyExpressionValue(method = "handleLoginFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientBrandRetriever;getClientModName()Ljava/lang/String;", remap = false))
    private String getClientModName(String original) {
        return SpooferClient.INSTANCE.clientBrand(original);
    }

    /**
     * For some reason a lot of people do not know the Minecraft basics.
     * How do people not know that you need a Minecraft premium account to join a premium server?
     */
    @ModifyExpressionValue(method = "authenticateServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 1))
    private MutableComponent modifySessionReason(MutableComponent original) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return original;
        }

        var notOfflineMode = PlainText.of(
            "Not offline mode",
            Style.EMPTY.withColor(ChatFormatting.RED).withUnderlined(true)
        );
        var requiresValidText = PlainText.of(
            "This server requires a valid session. Possible solutions:",
            ChatFormatting.RED
        );
        var loginText = PlainText.of("Login into a Minecraft premium account and try again.");
        var retryText = PlainText.of("If you've already signed into a premium account,\n" +
                "reload the game or re-sign into the account.");

        return Component.empty()
                .append(notOfflineMode)
                .append(PlainText.NEW_LINE)
                .append(PlainText.NEW_LINE)
                .append(requiresValidText)
                .append(PlainText.NEW_LINE)
                .append(loginText)
                .append(PlainText.NEW_LINE)
                .append(retryText);
    }

}
