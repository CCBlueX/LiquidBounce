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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Font.class)
public abstract class MixinFont {

    @ModifyArg(
        method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"
        )
    )
    private String injectNameProtectA(String text) {
        return ModuleNameProtect.INSTANCE.replace(text);
    }

    @Redirect(
        method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FormattedCharSequence;accept(Lnet/minecraft/util/FormattedCharSink;)Z")
    )
    private boolean injectNameProtectB(FormattedCharSequence orderedText, FormattedCharSink visitor) {
        return ModuleNameProtect.INSTANCE.wrap(orderedText).accept(visitor);
    }

    @ModifyArg(method = "width(Ljava/lang/String;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/StringSplitter;stringWidth(Ljava/lang/String;)F"), index = 0)
    private @Nullable String injectNameProtectWidthA(@Nullable String text) {
        if (text != null && ModuleNameProtect.INSTANCE.getRunning()) {
            return ModuleNameProtect.INSTANCE.replace(text);
        }

        return text;
    }

    @ModifyArg(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/StringSplitter;stringWidth(Lnet/minecraft/util/FormattedCharSequence;)F"),
            index = 0)
    private FormattedCharSequence injectNameProtectWidthB(FormattedCharSequence text) {
        return ModuleNameProtect.INSTANCE.wrap(text);
    }

}
