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
package net.ccbluex.liquidbounce.common;

import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleGhostHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TweakedMethods {

    public static BlockHitResult tweakedRaycast(BlockGetter blockView, ClipContext context) {
        if (ModuleGhostHand.INSTANCE.getRunning()) {
            var returned = BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (contextx, pos) -> {
                BlockState blockState = blockView.getBlockState(pos);

                if (!ModuleGhostHand.INSTANCE.getTargetedBlocks().contains(blockState.getBlock()))
                    return null;

                VoxelShape voxelShape = contextx.getBlockShape(blockState, blockView, pos);

                return blockView.clipWithInteractionOverride(contextx.getFrom(), contextx.getTo(), pos, voxelShape, blockState);
            }, (contextx) -> null);

            if (returned != null)
                return returned;
        }

        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (contextx, pos) -> {
            BlockState blockState = blockView.getBlockState(pos);
            FluidState fluidState = blockView.getFluidState(pos);
            Vec3 vec3d = contextx.getFrom();
            Vec3 vec3d2 = contextx.getTo();
            VoxelShape voxelShape = contextx.getBlockShape(blockState, blockView, pos);
            BlockHitResult
                blockHitResult = blockView.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);
            VoxelShape voxelShape2 = contextx.getFluidShape(fluidState, blockView, pos);
            BlockHitResult blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : contextx.getFrom().distanceToSqr(blockHitResult.getLocation());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : contextx.getFrom().distanceToSqr(blockHitResult2.getLocation());
            return d <= e ? blockHitResult : blockHitResult2;
        }, contextx -> {
            Vec3 vec3d = contextx.getFrom().subtract(contextx.getTo());
            return BlockHitResult.miss(contextx.getTo(), Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(contextx.getTo()));
        });
    }

}
