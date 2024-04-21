/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSignRender;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.function.Function;

@Mixin(SignText.class)
public class MixinSignText {

    @Inject(method = "getOrderedMessages", at = @At("HEAD"), cancellable = true)
    private void injectNoSignRender(boolean filtered, Function<Text, OrderedText> messageOrderer, CallbackInfoReturnable<OrderedText[]> cir) {
        if (ModuleNoSignRender.INSTANCE.getEnabled()) {
            var signText = new OrderedText[4];
            Arrays.fill(signText, OrderedText.EMPTY);
            cir.setReturnValue(signText);
        }
    }

}
