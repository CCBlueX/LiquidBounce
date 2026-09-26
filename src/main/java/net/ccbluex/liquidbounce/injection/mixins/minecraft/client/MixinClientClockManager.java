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

import net.ccbluex.liquidbounce.interfaces.ClientClockInstanceAddition;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Since 26.3 the overworld clock is read through {@code ClockInstance#totalTicks()} instead of the
 * former {@code ClockManager#getTotalTicks(Holder)}. Tags each clock instance with its definition so
 * {@link MixinClientClockInstance} can apply the CustomAmbience time override to the overworld clock.
 *
 * @see MixinClientClockInstance
 */
@NullMarked
@Mixin(ClientClockManager.class)
public abstract class MixinClientClockManager {

    @Inject(
        method = "lambda$getInstance$0", // computeIfAbsent
        at = @At("RETURN")
    )
    private static void liquidbounce$tagInstance(Holder<WorldClock> definition, CallbackInfoReturnable<ClientClockManager.ClientClockInstance> cir) {
        ((ClientClockInstanceAddition) cir.getReturnValue()).liquid_bounce$setDefinition(definition);
    }

}
