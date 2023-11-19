/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.BlockSlipperinessMultiplierEvent;
import net.ccbluex.liquidbounce.event.events.BlockVelocityMultiplierEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Block.class)
public class MixinBlock {

    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private static void injectXRay(BlockState state, BlockView world, BlockPos pos, Direction side, BlockPos blockPos, CallbackInfoReturnable<Boolean> callback) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getEnabled()) {
            return;
        }

        Set<Block> blocks = module.getBlocks();
        callback.setReturnValue(blocks.contains(state.getBlock()));
        callback.cancel();
    }

    /**
     * Hook velocity multiplier event
     */
    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void hookVelocityMultiplier(CallbackInfoReturnable<Float> callback) {
        final BlockVelocityMultiplierEvent multiplierEvent = new BlockVelocityMultiplierEvent((Block) (Object) this, callback.getReturnValue());
        EventManager.INSTANCE.callEvent(multiplierEvent);
        callback.setReturnValue(multiplierEvent.getMultiplier());
    }

    /**
     * Hook slipperiness multiplier event
     */
    @Inject(method = "getSlipperiness", at = @At("RETURN"), cancellable = true)
    private void hookSlipperinessMultiplier(CallbackInfoReturnable<Float> cir) {
        final BlockSlipperinessMultiplierEvent slipperinessEvent = new BlockSlipperinessMultiplierEvent((Block) (Object) this, cir.getReturnValue());
        EventManager.INSTANCE.callEvent(slipperinessEvent);
        cir.setReturnValue(slipperinessEvent.getSlipperiness());
    }
}
