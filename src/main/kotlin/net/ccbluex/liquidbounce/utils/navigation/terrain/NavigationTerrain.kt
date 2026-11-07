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

package net.ccbluex.liquidbounce.utils.navigation.terrain

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs

enum class DestinationCheck {
    VALID,
    OUTSIDE_WORLD,
    OUTSIDE_BORDER,
    NOT_LOADED,
    NOT_STANDABLE,
}

/**
 * Block lookups for route searches, cached per block until [invalidate] or [clear]. Unknown blocks and hazards
 * count as solid.
 *
 * Not thread-safe: use it from one thread at a time.
 */
@Suppress("TooManyFunctions")
class NavigationTerrain(
    val world: NavigationWorld,
    val width: Double = PLAYER_WIDTH,
    val height: Double = PLAYER_HEIGHT,
) {

    private val halfWidth = width / 2.0
    private val states = Long2ObjectOpenHashMap<BlockState>()
    private val shapes = Long2ObjectOpenHashMap<VoxelShape>()
    private val stands = Long2DoubleOpenHashMap()

    val minY: Int get() = world.minY

    val maxY: Int get() = world.maxY

    fun box(feet: Vec3): AABB = box(feet.x, feet.y, feet.z)

    fun box(x: Double, y: Double, z: Double) =
        AABB(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth)

    /**
     * Whether the block at [x] [y] [z] is loaded, copied and inside the world border.
     */
    fun isKnown(x: Int, y: Int, z: Int) = world.isKnown(x, y, z) && world.isInsideBorder(x + 0.5, z + 0.5)

    fun state(x: Int, y: Int, z: Int): BlockState {
        val key = BlockPos.asLong(x, y, z)
        states.get(key)?.let { return it }

        val state = if (isKnown(x, y, z)) world.getBlockState(BlockPos(x, y, z)) else VOID_AIR
        states.put(key, state)
        return state
    }

    /**
     * Collision shape of the block at [x] [y] [z], relative to the block. Unknown blocks and hazards are full blocks.
     */
    fun shape(x: Int, y: Int, z: Int): VoxelShape {
        val key = BlockPos.asLong(x, y, z)
        shapes.get(key)?.let { return it }

        val state = state(x, y, z)
        val shape = when {
            !isKnown(x, y, z) || isHazard(state) -> Shapes.block()
            else -> state.getCollisionShape(world, BlockPos(x, y, z))
        }

        shapes.put(key, shape)
        return shape
    }

    fun isPassable(x: Int, y: Int, z: Int) = shape(x, y, z).isEmpty

    fun isHazard(x: Int, y: Int, z: Int) = isKnown(x, y, z) && isHazard(state(x, y, z))

    fun isWater(x: Int, y: Int, z: Int) = state(x, y, z).fluidState.type.isSame(Fluids.WATER)

    fun isClimbable(x: Int, y: Int, z: Int) = state(x, y, z).block in CLIMBABLE

    /**
     * Height the feet rest at when standing in block [x] [y] [z], always within that block, or NaN when nothing
     * carries the player there or the body does not fit. Partial blocks such as slabs, farmland, carpets and
     * fences are taken into account, so the block of a standing player is `BlockPos.containing(feet)`.
     */
    fun standHeight(x: Int, y: Int, z: Int): Double {
        val key = BlockPos.asLong(x, y, z)
        if (stands.containsKey(key)) {
            return stands.get(key)
        }

        val height = computeStandHeight(x, y, z)
        stands.put(key, height)
        return height
    }

    fun isStandable(feet: BlockPos) = !standHeight(feet.x, feet.y, feet.z).isNaN()

    /**
     * Where the feet rest when standing in [node], or null when the player cannot stand there.
     */
    fun feet(node: BlockPos): Vec3? {
        val height = standHeight(node.x, node.y, node.z)
        return if (height.isNaN()) null else Vec3(node.x + 0.5, height, node.z + 0.5)
    }

    fun check(feet: BlockPos): DestinationCheck = when {
        feet.y < minY || feet.y > maxY -> DestinationCheck.OUTSIDE_WORLD
        !world.isInsideBorder(feet.x + 0.5, feet.z + 0.5) -> DestinationCheck.OUTSIDE_BORDER
        !world.isKnown(feet.x, feet.y, feet.z) -> DestinationCheck.NOT_LOADED
        !isStandable(feet) -> DestinationCheck.NOT_STANDABLE
        else -> DestinationCheck.VALID
    }

    fun isClear(box: AABB): Boolean {
        val minX = Mth.floor(box.minX + EPSILON)
        val maxX = Mth.floor(box.maxX - EPSILON)
        // One extra layer below for blocks taller than a block, like fences.
        val minY = Mth.floor(box.minY + EPSILON) - 1
        val maxY = Mth.floor(box.maxY - EPSILON)
        val minZ = Mth.floor(box.minZ + EPSILON)
        val maxZ = Mth.floor(box.maxZ - EPSILON)

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val shape = shape(x, y, z)
                    if (!shape.isEmpty && shape.intersects(x, y, z, box)) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * Whether the server moves a player from [from] to [to] without hitting anything. The server moves
     * one axis after the other, so the corners it passes are checked too.
     */
    fun sweep(from: Vec3, to: Vec3): Boolean {
        val movement = to.subtract(from)
        val box = box(from)
        val colliders = colliders(box.expandTowards(movement))
        if (colliders.isEmpty()) {
            return true
        }

        var moved = Vec3.ZERO
        for (axis in Direction.axisStepOrder(movement)) {
            val axisMovement = movement.get(axis)
            if (axisMovement == 0.0) {
                continue
            }

            val collision = Shapes.collide(axis, box.move(moved), colliders, axisMovement)
            if (abs(collision - axisMovement) > EPSILON) {
                return false
            }
            moved = moved.with(axis, collision)
        }

        return true
    }

    /**
     * Whether something is right below the feet, which is what the server calls on ground.
     */
    fun isSupported(feet: Vec3): Boolean {
        val box = box(feet)
        return !isClear(AABB(box.minX, feet.y - SUPPORT_PROBE, box.minZ, box.maxX, feet.y, box.maxZ))
    }

    /**
     * Drops the cached data of [pos], which became [state], and the blocks whose standing height depends on it; true
     * if any was cached. A change that leaves the block's collision alike, as a growing crop does, is ignored.
     */
    fun invalidate(pos: BlockPos, state: BlockState): Boolean {
        val key = pos.asLong()
        val old = states.get(key)
        if (old != null && isAlike(old, state, pos)) {
            return false
        }

        var cached = states.remove(key) != null
        cached = shapes.remove(key) != null || cached
        // The floor, the body and the layer below a tall floor block of a standing spot are within two blocks.
        for (dy in -STAND_REACH..STAND_REACH) {
            val standKey = BlockPos.asLong(pos.x, pos.y + dy, pos.z)
            if (stands.containsKey(standKey)) {
                stands.remove(standKey)
                cached = true
            }
        }

        return cached
    }

    private fun isAlike(old: BlockState, new: BlockState, pos: BlockPos): Boolean {
        if (old === new) {
            return true
        }
        if (old.block !== new.block || !old.fluidState.type.isSame(new.fluidState.type) ||
            isHazard(old) != isHazard(new)) {
            return false
        }

        val oldShape = shapes.get(pos.asLong()) ?: old.getCollisionShape(world, pos)
        val newShape = if (isHazard(new)) Shapes.block() else new.getCollisionShape(world, pos)
        return oldShape === newShape || !Shapes.joinIsNotEmpty(oldShape, newShape, BooleanOp.NOT_SAME)
    }

    fun clear() {
        states.clear()
        shapes.clear()
        stands.clear()
    }

    private fun computeStandHeight(x: Int, y: Int, z: Int): Double {
        if (y > maxY || !isKnown(x, y, z) || !isKnown(x, y - 1, z)) {
            return Double.NaN
        }

        val below = shape(x, y - 1, z)
        val at = shape(x, y, z)
        if (at === FULL || below.isEmpty && at.isEmpty || isHazard(x, y - 1, z) || isHazard(x, y, z)) {
            return Double.NaN
        }

        val top = if (below === FULL && at.isEmpty) y.toDouble() else partialTop(below, at, y)
        return if (top.isNaN() || !isClear(box(x + 0.5, top, z + 0.5))) Double.NaN else top
    }

    private fun partialTop(below: VoxelShape, at: VoxelShape, y: Int): Double {
        var top = Double.NaN
        for (box in below.toAabbs()) {
            val boxTop = y - 1 + box.maxY
            if (box.overlapsFootprint() && boxTop >= y && boxTop < y + 1 && !(boxTop <= top)) {
                top = boxTop
            }
        }

        for (box in at.toAabbs()) {
            if (!box.overlapsFootprint()) {
                continue
            }

            val boxTop = y + box.maxY
            if (boxTop >= y + 1) {
                return Double.NaN
            }
            if (!(boxTop <= top)) {
                top = boxTop
            }
        }

        return top
    }

    private fun AABB.overlapsFootprint() =
        minX < 0.5 + halfWidth && maxX > 0.5 - halfWidth && minZ < 0.5 + halfWidth && maxZ > 0.5 - halfWidth

    private fun colliders(region: AABB): List<VoxelShape> {
        val minX = Mth.floor(region.minX + EPSILON)
        val maxX = Mth.floor(region.maxX - EPSILON)
        val minY = Mth.floor(region.minY + EPSILON) - 1
        val maxY = Mth.floor(region.maxY - EPSILON)
        val minZ = Mth.floor(region.minZ + EPSILON)
        val maxZ = Mth.floor(region.maxZ - EPSILON)

        val colliders = ArrayList<VoxelShape>()
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val shape = shape(x, y, z)
                    if (!shape.isEmpty) {
                        colliders.add(shape.move(x.toDouble(), y.toDouble(), z.toDouble()))
                    }
                }
            }
        }

        return colliders
    }

    companion object {
        const val PLAYER_WIDTH = 0.6
        const val PLAYER_HEIGHT = 1.8

        /**
         * Highest ledge the player walks onto without jumping.
         */
        const val STEP = 0.6

        private const val STAND_REACH = 2
        private const val EPSILON = 1.0E-7
        private const val SUPPORT_PROBE = 1.0E-3

        private val VOID_AIR: BlockState = Blocks.VOID_AIR.defaultBlockState()
        private val FULL: VoxelShape = Shapes.block()

        private val HAZARDS = setOf(
            Blocks.COBWEB,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.POWDER_SNOW,
            Blocks.WITHER_ROSE,
            Blocks.MAGMA_BLOCK,
            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,
            Blocks.CACTUS,
        )

        private val CLIMBABLE: Set<Block> = setOf(
            Blocks.LADDER,
            Blocks.VINE,
            Blocks.WEEPING_VINES,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
        )

        fun isHazard(state: BlockState) =
            state.fluidState.type.isSame(Fluids.LAVA) || state.block is BaseFireBlock || state.block in HAZARDS

        private fun VoxelShape.intersects(x: Int, y: Int, z: Int, box: AABB): Boolean {
            if (this === Shapes.block()) {
                return box.intersects(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
            }

            return toAabbs().any {
                box.intersects(it.minX + x, it.minY + y, it.minZ + z, it.maxX + x, it.maxY + y, it.maxZ + z)
            }
        }
    }

}
