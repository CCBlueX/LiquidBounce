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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.NativeImage;
import net.ccbluex.liquidbounce.utils.client.ClientChat;
import net.ccbluex.liquidbounce.utils.text.PlainText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static net.ccbluex.liquidbounce.lang.LanguageKt.translation;
import static net.ccbluex.liquidbounce.utils.io.ClipboardUtilsKt.clipboardCopyAction;

@NullMarked
@Mixin(Screenshot.class)
public abstract class MixinScreenshot {

    // Matches the success callback lambda inside Screenshot.grab (the one building the
    // "screenshot.success" clickable file name). The failure path ("screenshot.failure")
    // has no withStyle call, so this injection cannot accidentally apply to it.
    // NOTE: the synthetic lambda name "lambda$grab$3" breaks on Minecraft upgrades if the
    // lambdas in grab are reordered - double-check Screenshot.grab before bumping versions.
    @ModifyExpressionValue(
        method = "lambda$grab$3",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Ljava/util/function/UnaryOperator;)Lnet/minecraft/network/chat/MutableComponent;")
    )
    private static MutableComponent appendCopyAction(MutableComponent original, @Local(argsOnly = true, name = "image") NativeImage image) {
        var copyAction = clipboardCopyAction(image);
        return Component.empty()
            .append(original)
            .append(PlainText.SPACE)
            .append(PlainText.of("[", ChatFormatting.GRAY))
            .append(
                ClientChat.onClickRun(
                    translation("liquidbounce.tooltip.clickToCopy").withStyle(ChatFormatting.GRAY),
                    copyAction
                )
            )
            .append(PlainText.of("]", ChatFormatting.GRAY));
    }

}
