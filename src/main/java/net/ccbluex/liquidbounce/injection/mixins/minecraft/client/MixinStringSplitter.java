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
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(StringSplitter.class)
public abstract class MixinStringSplitter {

    @Shadow
    @Final
    public StringSplitter.WidthProvider widthProvider;

    @Inject(method = "stringWidth(Lnet/minecraft/network/chat/FormattedText;)F", at = @At("HEAD"), cancellable = true)
    private void injectNameProtectWidthB(FormattedText text, CallbackInfoReturnable<Float> cir) {
        if (!ModuleNameProtect.INSTANCE.getRunning()) {
            return;
        }

        MutableFloat mutableFloat = new MutableFloat();
        text.visit((style, asString) -> {
            StringDecomposer.iterateFormatted(ModuleNameProtect.INSTANCE.replace(asString), style, (unused, stylex, codePoint) -> {
                mutableFloat.add(widthProvider.getWidth(codePoint, stylex));
                return true;
            });

            return Optional.empty();
        }, Style.EMPTY);

        cir.setReturnValue(mutableFloat.floatValue());
    }

}
