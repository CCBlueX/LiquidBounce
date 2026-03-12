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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoInterpolation;
import net.minecraft.world.entity.InterpolationHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InterpolationHandler.class)
public abstract class MixinInterpolationHandler {

    @ModifyExpressionValue(
        method = "interpolateTo",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/InterpolationHandler;interpolationSteps:I",
            opcode = Opcodes.GETFIELD
        )
    )
    private int hookInterpolationSteps(int original) {
        if (ModuleNoInterpolation.INSTANCE.getRunning()) {
            return Math.max(original - ModuleNoInterpolation.INSTANCE.getValue(), 0);
        }
        return original;
    }

}
