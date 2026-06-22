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

import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.function.Function;

@Mixin(SignText.class)
public abstract class MixinSignText {

    @Unique
    private static final FormattedCharSequence[] LIQUIDBOUNCE$EMPTY = new FormattedCharSequence[SignText.LINES];

    static {
        Arrays.fill(LIQUIDBOUNCE$EMPTY, FormattedCharSequence.EMPTY);
    }

    @Inject(method = "getRenderMessages", at = @At("HEAD"), cancellable = true)
    private void injectNoSignRender(boolean filtered, Function<Component, FormattedCharSequence> messageOrderer, CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        if (!ModuleAntiBlind.canRender(DoRender.SIGN_TEXT)) {
            cir.setReturnValue(LIQUIDBOUNCE$EMPTY);
        }
    }

}
