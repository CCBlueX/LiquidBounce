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

import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slime.NoSlowSlime;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public class MixinSlimeBlock {

    @Inject(method = "bounce", at = @At("HEAD"), cancellable = true)
    private void hookBounce(Entity entity, CallbackInfo ci) {
        if (NoSlowSlime.INSTANCE.getRunning()) {
            if (entity.getVelocity().y == -0.0784000015258789 || entity.getVelocity().y == -0.001567998535156222) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
    private void hookStep(World world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (NoSlowSlime.INSTANCE.getRunning()) {
            ci.cancel();
        }
    }
}
