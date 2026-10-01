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

import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect;
import net.ccbluex.liquidbounce.utils.text.TextExtensionsKt;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringSplitter.class)
public abstract class MixinStringSplitter {

    // TODO: only `stringWidth` measures the replaced text. `StringSplitter.splitLines` and `headByWidth`
    //  still lay out the original one, so a replacement longer than the name it replaces can overflow
    //  the width these methods were measured for.

    @Shadow
    @Final
    public StringSplitter.WidthProvider widthProvider;

    @Inject(method = "stringWidth(Lnet/minecraft/network/chat/FormattedText;)F", at = @At("HEAD"), cancellable = true)
    private void injectNameProtectWidthB(FormattedText text, CallbackInfoReturnable<Float> cir) {
        if (!ModuleNameProtect.INSTANCE.getRunning()) {
            return;
        }

        // Measure through the same path rendering uses, so bypassed names and names spanning
        // multiple style parts are measured exactly like they are drawn.
        FormattedCharSequence replaced = ModuleNameProtect.INSTANCE.wrap(TextExtensionsKt.asFormattedCharSequence(text));

        MutableFloat mutableFloat = new MutableFloat();
        replaced.accept((_, style, codePoint) -> {
            mutableFloat.add(widthProvider.getWidth(codePoint, style));
            return true;
        });

        cir.setReturnValue(mutableFloat.floatValue());
    }

}
