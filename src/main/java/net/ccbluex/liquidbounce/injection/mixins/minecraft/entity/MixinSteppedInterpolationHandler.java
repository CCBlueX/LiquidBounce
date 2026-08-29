/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoInterpolation;
import net.minecraft.world.entity.SteppedInterpolationHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SteppedInterpolationHandler.class)
public abstract class MixinSteppedInterpolationHandler {
    @ModifyExpressionValue(
        method = "startInterpolating",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/SteppedInterpolationHandler;interpolationSteps:I", opcode = Opcodes.GETFIELD)
    )
    private int hookInterpolationSteps(int original) {
        return ModuleNoInterpolation.reduceInterpolationSteps(original);
    }
}
