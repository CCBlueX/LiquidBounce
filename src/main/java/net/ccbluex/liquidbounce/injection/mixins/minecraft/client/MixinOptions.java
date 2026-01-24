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

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class MixinOptions {

    /**
     * Hook GUI scale adjustment
     * <p>
     * This is used to set the default GUI scale to 2X on AUTO because the default is TOO HUGE.
     * On WQHD and HD displays, the default GUI scale is way too big. 4K might be fine, but
     * the majority of players are not using 4K displays.
     */
    @Inject(method = "guiScale", at = @At("RETURN"))
    private void injectGuiScale(CallbackInfoReturnable<OptionInstance<Integer>> cir) {
        if (cir.getReturnValue().get() == 0) {
            cir.getReturnValue().set(2);
        }
    }

}
