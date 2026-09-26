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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.ModuleCustomAmbience;
import net.ccbluex.liquidbounce.interfaces.ClientClockInstanceAddition;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockInstance;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Since 26.3 the overworld clock is read through {@code ClockInstance#totalTicks()} instead of the
 * former {@code ClockManager#getTotalTicks(Holder)}. Clock instances do not know their own definition,
 * so it is tagged by {@link MixinClientClockManager} upon creation.
 *
 * @see MixinClientClockManager
 */
@NullMarked
@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public abstract class MixinClientClockInstance implements ClockInstance, ClientClockInstanceAddition {

    @Unique
    private @Nullable Holder<WorldClock> liquidbounce$definition;

    @Unique
    @Override
    public void liquid_bounce$setDefinition(Holder<WorldClock> definition) {
        this.liquidbounce$definition = definition;
    }

    @ModifyReturnValue(method = "totalTicks", at = @At("RETURN"))
    private long injectOverrideClockTime(long original) {
        var definition = this.liquidbounce$definition;
        if (definition == null || !definition.is(WorldClocks.OVERWORLD)) {
            return original;
        }

        return ModuleCustomAmbience.getWorldClockTime(original);
    }

}
