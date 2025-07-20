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
package net.ccbluex.liquidbounce.common;

import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleGhostHand;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

public class TweakedMethods {

    public static BlockHitResult tweakedRaycast(BlockView blockView, RaycastContext context) {
        if (ModuleGhostHand.INSTANCE.getRunning()) {
            var returned = (BlockHitResult) BlockView.raycast(context.getStart(), context.getEnd(), context, (contextx, pos) -> {
                BlockState blockState = blockView.getBlockState(pos);

                if (!ModuleGhostHand.INSTANCE.getTargetedBlocks().contains(blockState.getBlock()))
                    return null;

                VoxelShape voxelShape = contextx.getBlockShape(blockState, blockView, pos);

                return blockView.raycastBlock(contextx.getStart(), contextx.getEnd(), pos, voxelShape, blockState);
            }, (contextx) -> null);

            if (returned != null)
                return returned;
        }

        return BlockView.raycast(context.getStart(), context.getEnd(), context, (contextx, pos) -> {
            BlockState blockState = blockView.getBlockState(pos);
            FluidState fluidState = blockView.getFluidState(pos);
            Vec3d vec3d = contextx.getStart();
            Vec3d vec3d2 = contextx.getEnd();
            VoxelShape voxelShape = contextx.getBlockShape(blockState, blockView, pos);
            BlockHitResult blockHitResult = blockView.raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);
            VoxelShape voxelShape2 = contextx.getFluidShape(fluidState, blockView, pos);
            BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : contextx.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : contextx.getStart().squaredDistanceTo(blockHitResult2.getPos());
            return d <= e ? blockHitResult : blockHitResult2;
        }, contextx -> {
            Vec3d vec3d = contextx.getStart().subtract(contextx.getEnd());
            return BlockHitResult.createMissed(contextx.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(contextx.getEnd()));
        });
    }

}
