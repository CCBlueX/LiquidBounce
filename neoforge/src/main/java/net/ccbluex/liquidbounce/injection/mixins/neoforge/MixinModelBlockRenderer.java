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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Loader-specific companion of {@code minecraft.render.MixinModelBlockRenderer}.
 * <p>
 * NeoForge patches an extra self-position {@code BlockPos} into the
 * {@code shouldRenderFace} overload that the block tesselation path calls, so
 * the hook targets that 5-argument overload explicitly (the vanilla-shaped
 * 4-argument overload is an off-path convenience method).
 */
@NullMarked
@Mixin(ModelBlockRenderer.class)
public abstract class MixinModelBlockRenderer {

    @ModifyReturnValue(
        method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("RETURN")
    )
    private boolean injectXRayDrawSide(boolean original, BlockAndTintGetter level, BlockPos pos, BlockState state,
            Direction direction, BlockPos neighborPos) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning()) {
            return original;
        }

        return module.modifyDrawSide(state, level, neighborPos.relative(direction.getOpposite()), direction, original);
    }

}
