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

package net.ccbluex.liquidbounce.injection.mixins.lithium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeperVoxelShape;
import net.ccbluex.liquidbounce.common.ShapeFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@SuppressWarnings("rawtypes")
@Pseudo
@Mixin(ChunkAwareBlockCollisionSweeperVoxelShape.class)
public abstract class MixinChunkAwareBlockCollisionSweeperVoxelShape extends ChunkAwareBlockCollisionSweeper {

    public MixinChunkAwareBlockCollisionSweeperVoxelShape(
        Level world,
        @Nullable Entity entity,
        AABB box,
        boolean hideLastCollision
    ) {
        super(world, entity, box, hideLastCollision);
    }

    /**
     * Hook collision shape event
     *
     * @param original voxel shape
     * @return possibly modified voxel shape
     */
    @ModifyExpressionValue(method = "computeNext()Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
    ))
    private VoxelShape hookCollisionShape(VoxelShape original, @Local(name = "state") BlockState blockState) {
        if (this.pos == null || ShapeFlag.noShapeChange) {
            return original;
        }

        final BlockShapeEvent shapeEvent = EventManager.INSTANCE.callEvent(new BlockShapeEvent(blockState, this.pos, original));
        return shapeEvent.getShape();
    }

}
