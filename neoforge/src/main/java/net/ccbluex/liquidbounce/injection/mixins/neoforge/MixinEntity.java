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
package net.ccbluex.liquidbounce.injection.mixins.neoforge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slime.NoSlowSlime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Loader-specific companion of {@code minecraft.entity.MixinEntity}.
 * <p>
 * NeoForge moves the body of {@code restituteMovementAfterCollisions} (with the
 * {@code Math#max} slime-bounce expression) into an added overload with a
 * leading {@code BlockPos} parameter, so the hook targets that overload. The
 * {@code BlockState} is captured by type because the recompiled NeoForge jar
 * does not preserve local variable names.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @ModifyExpressionValue(
        method = "restituteMovementAfterCollisions(Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;ZZLnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D", remap = false)
    )
    private double hookSlimeBounce(double original, @Local(argsOnly = true) BlockState effectState) {
        if (NoSlowSlime.INSTANCE.getRunning() && effectState.getBlock() instanceof SlimeBlock) {
            return 0.0;
        }

        return original;
    }

}
