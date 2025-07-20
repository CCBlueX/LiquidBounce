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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint;
import net.ccbluex.liquidbounce.interfaces.InputAddition;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Input.class)
public abstract class MixinInput implements InputAddition {

    @Shadow
    public float movementForward;

    @Shadow
    public float movementSideways;

    @Unique
    protected PlayerInput initial = PlayerInput.DEFAULT;

    @Unique
    protected PlayerInput untransformed = PlayerInput.DEFAULT;

    @ModifyReturnValue(method = "hasForwardMovement", at = @At("RETURN"))
    private boolean hookOmnidirectionalSprint(boolean original) {
        // Allow omnidirectional sprinting
        if (ModuleSprint.INSTANCE.getShouldSprintOmnidirectional()) {
            return Math.abs(movementForward) > 1.0E-5F || Math.abs(movementSideways) > 1.0E-5F;
        }

        return original;
    }

    @Override
    public PlayerInput liquid_bounce$getInitial() {
        return initial;
    }

    @Override
    public PlayerInput liquid_bounce$getUntransformed() {
        return untransformed;
    }

}
