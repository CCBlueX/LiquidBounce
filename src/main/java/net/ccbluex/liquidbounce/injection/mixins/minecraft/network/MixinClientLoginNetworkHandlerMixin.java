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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.spoofer.SpooferClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLoginNetworkHandler.class)
public class MixinClientLoginNetworkHandlerMixin {

    @ModifyExpressionValue(method = "onSuccess", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientBrandRetriever;getClientModName()Ljava/lang/String;", remap = false))
    private String getClientModName(String original) {
        return SpooferClient.INSTANCE.clientBrand(original);
    }

    /**
     * For some reason a lot of people do not know the Minecraft basics.
     * How do people not know that you need a Minecraft premium account to join a premium server?
     */
    @ModifyExpressionValue(method = "joinServerSession", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;", ordinal = 1))
    private MutableText modifySessionReason(MutableText original) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return original;
        }

        var notOfflineMode = Text.literal("Not offline mode")
                .styled(style -> style.withColor(Formatting.RED).withUnderline(true));
        var requiresValidText = Text.literal("This server requires a valid session. Possible solutions:")
                .styled(style -> style.withColor(Formatting.RED));
        var loginText = Text.literal("Login into a Minecraft premium account and try again.");
        var retryText = Text.literal("If you've already signed into a premium account,\n" +
                "reload the game or re-sign into the account.");

        return Text.empty()
                .append(notOfflineMode)
                .append("\n\n")
                .append(requiresValidText)
                .append("\n")
                .append(loginText)
                .append("\n")
                .append(retryText);
    }

}
