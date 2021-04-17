/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.BlockVelocityMultiplierEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinBlock {

    /**
     * Hook velocity multiplier event
     */
    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void hookVelocityMultiplier(CallbackInfoReturnable<Float> callback) {
        final BlockVelocityMultiplierEvent multiplierEvent = new BlockVelocityMultiplierEvent((Block) (Object) this, callback.getReturnValue());
        EventManager.INSTANCE.callEvent(multiplierEvent);
        callback.setReturnValue(multiplierEvent.getMultiplier());
    }

}
