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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.BlockSlipperinessMultiplierEvent;
import net.ccbluex.liquidbounce.event.events.BlockVelocityMultiplierEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public class MixinBlock {

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private static boolean injectXRay(boolean original, BlockState state, BlockState otherState, Direction side) {
        var xRay = ModuleXRay.INSTANCE;
        if (xRay.getRunning()) {
            return xRay.shouldRender(state, otherState, side);
        }

        return original;
    }

    /**
     * Hook velocity multiplier event
     *
     * @return
     */
    @ModifyReturnValue(method = "getVelocityMultiplier", at = @At("RETURN"))
    private float hookVelocityMultiplier(float original) {
        final var multiplierEvent = EventManager.INSTANCE.callEvent(new BlockVelocityMultiplierEvent((Block) (Object) this, original));
        return multiplierEvent.getMultiplier();
    }

    /**
     * Hook slipperiness multiplier event
     *
     * @return
     */
    @ModifyReturnValue(method = "getSlipperiness", at = @At("RETURN"))
    private float hookSlipperinessMultiplier(float original) {
        final var slipperinessEvent = EventManager.INSTANCE.callEvent(new BlockSlipperinessMultiplierEvent((Block) (Object) this, original));
        return slipperinessEvent.getSlipperiness();
    }
}
