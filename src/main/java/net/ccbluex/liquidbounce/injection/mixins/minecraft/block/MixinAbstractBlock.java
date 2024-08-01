/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBlock.class)
public class MixinAbstractBlock {

    /**
     * Hook collision shape event
     *
     * @param original voxel shape
     * @param state block state
     * @param world block world
     * @param pos block position
     * @param context shape context
     * @return possibly modified voxel shape
     */
    @ModifyReturnValue(method = "getCollisionShape", at = @At("RETURN"))
    private VoxelShape hookCollisionShape(VoxelShape original, BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (pos == null) {
            return original;
        }

        final BlockShapeEvent shapeEvent = EventManager.INSTANCE.callEvent(new BlockShapeEvent(state, pos, original));
        return shapeEvent.getShape();
    }

}
