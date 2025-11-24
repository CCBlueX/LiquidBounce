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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.widget;

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTextFieldProtect;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(TextFieldWidget.class)
public abstract class MixinTextFieldWidget {

    @Shadow
    private BiFunction<String, Integer, OrderedText> renderTextProvider;

    @Inject(method = "<init>(Lnet/minecraft/client/font/TextRenderer;IIIILnet/minecraft/client/gui/widget/TextFieldWidget;Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void wrapRenderTextProviderAtInit(TextRenderer textRenderer, int x, int y, int width, int height,
        TextFieldWidget copyFrom, Text text, CallbackInfo ci) {
        this.renderTextProvider = ModuleTextFieldProtect.INSTANCE.getWrappedRenderTextProvider((TextFieldWidget) (Object) this, this.renderTextProvider);
    }

    @Inject(method = "setRenderTextProvider", at = @At("RETURN"))
    private void wrapRenderTextProviderAtSet(CallbackInfo ci) {
        this.renderTextProvider = ModuleTextFieldProtect.INSTANCE.getWrappedRenderTextProvider((TextFieldWidget) (Object) this, this.renderTextProvider);
    }

}
