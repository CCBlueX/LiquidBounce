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
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
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
 * With [opensDoors], closed doors, fence gates and trapdoors a player opens by hand count as open; [closedIn] tells
 * which a move has to open. A closed trapdoor stays closed when it is a floor: it counts as open only with a ladder
 * or vine below it or without room to stand on it.
 *
 * Not thread-safe: use it from one thread at a time.
 */
@Suppress("TooManyFunctions")
class NavigationTerrain(
    val world: NavigationWorld,
    val width: Double = PLAYER_WIDTH,
    val height: Double = PLAYER_HEIGHT,
    val opensDoors: Boolean = false,
) {

    private val halfWidth = width / 2.0
    private val states = Long2ObjectOpenHashMap<BlockState>()
    private val shapes = Long2ObjectOpenHashMap<VoxelShape>()
    private val stands = Long2DoubleOpenHashMap()
    private val crouchStands = Long2DoubleOpenHashMap()

    /**
     * Collision shapes of the closed blocks that count as open.
     */
    private val closed = Long2ObjectOpenHashMap<VoxelShape>()

    val minY: Int get() = world.minY

    val maxY: Int get() = world.maxY

    fun box(feet: Vec3): AABB = box(feet.x, feet.y, feet.z)

    fun box(x: Double, y: Double, z: Double, height: Double = this.height) =
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
        val pos = BlockPos(x, y, z)
        val shape = when {
            !isKnown(x, y, z) || isHazard(state) -> Shapes.block()
            countsOpen(state, x, y, z) -> {
                closed.put(key, state.getCollisionShape(world, pos))
                state.setValue(BlockStateProperties.OPEN, true).getCollisionShape(world, pos)
            }

            else -> state.getCollisionShape(world, pos)
        }

        shapes.put(key, shape)
        return shape
    }

    /**
     * Adds the closed doors, fence gates and trapdoors that count as open and that a body moving through [box] from
     * [from] would collide with to [opens] unless already there, a door by its lower half. Blocks [from] already
     * collides with must have been opened before.
     */
    fun closedIn(box: AABB, from: AABB, opens: MutableList<BlockPos>) {
        if (!opensDoors) {
            return
        }

        for (x in Mth.floor(box.minX + EPSILON)..Mth.floor(box.maxX - EPSILON)) {
            for (z in Mth.floor(box.minZ + EPSILON)..Mth.floor(box.maxZ - EPSILON)) {
                for (y in Mth.floor(box.minY + EPSILON) - 1..Mth.floor(box.maxY - EPSILON)) {
                    val closedShape = closedShape(x, y, z)
                    if (closedShape != null && closedShape.intersects(x, y, z, box) &&
                        !closedShape.intersects(x, y, z, from)) {
                        addOpen(x, y, z, opens)
                    }
                }
            }
        }
    }

    private fun closedShape(x: Int, y: Int, z: Int): VoxelShape? {
        shape(x, y, z)
        return closed.get(BlockPos.asLong(x, y, z))
    }

    private fun addOpen(x: Int, y: Int, z: Int, opens: MutableList<BlockPos>) {
        val upper = state(x, y, z).getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
            .map { it == DoubleBlockHalf.UPPER }.orElse(false)
        val pos = BlockPos(x, if (upper) y - 1 else y, z)
        if (pos !in opens) {
            opens += pos
        }
    }

    private fun countsOpen(state: BlockState, x: Int, y: Int, z: Int): Boolean {
        if (!opensDoors || state.getOptionalValue(BlockStateProperties.OPEN).orElse(true)) {
            return false
        }

        return when (val block = state.block) {
            is DoorBlock -> block.type().canOpenByHand()
            is FenceGateBlock -> true
            is TrapDoorBlock -> block.type.canOpenByHand() && !isFloor(x, y, z)
            else -> false
        }
    }

    private fun isFloor(x: Int, y: Int, z: Int) = state(x, y - 1, z).block !in CLIMBABLE &&
        isEmpty(x, y + 1, z) && isEmpty(x, y + 2, z)

    private fun isEmpty(x: Int, y: Int, z: Int) =
        state(x, y, z).getCollisionShape(world, BlockPos(x, y, z)).isEmpty

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
     * Like [standHeight] for a crouching player, who also fits under a ceiling [CROUCH_HEIGHT] above the feet.
     */
    fun crouchHeight(x: Int, y: Int, z: Int): Double {
        val key = BlockPos.asLong(x, y, z)
        if (crouchStands.containsKey(key)) {
            return crouchStands.get(key)
        }

        val height = computeStandHeight(x, y, z, minOf(height, CROUCH_HEIGHT))
        crouchStands.put(key, height)
        return height
    }

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
            if (state.getOptionalValue(BlockStateProperties.OPEN).orElse(false)) {
                // An opened door keeps the shape it counted with, but no longer needs opening.
                closed.remove(key)
            }
            return false
        }

        var cached = states.remove(key) != null
        cached = drop(pos.x, pos.y, pos.z) || cached
        if (opensDoors) {
            // Whether a trapdoor counts as open depends on the blocks below and above it.
            for (dy in TRAPDOOR_NEIGHBOURS) {
                if (states.get(BlockPos.asLong(pos.x, pos.y + dy, pos.z))?.block is TrapDoorBlock) {
                    cached = drop(pos.x, pos.y + dy, pos.z) || cached
                }
            }
        }

        return cached
    }

    /**
     * Drops the shape of [x] [y] [z] and the standing heights that depend on it; true if any was cached.
     */
    private fun drop(x: Int, y: Int, z: Int): Boolean {
        val key = BlockPos.asLong(x, y, z)
        var cached = shapes.remove(key) != null
        closed.remove(key)
        // The floor, the body and the layer below a tall floor block of a standing spot are within two blocks.
        for (dy in -STAND_REACH..STAND_REACH) {
            val standKey = BlockPos.asLong(x, y + dy, z)
            if (stands.containsKey(standKey)) {
                stands.remove(standKey)
                cached = true
            }
            if (crouchStands.containsKey(standKey)) {
                crouchStands.remove(standKey)
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
        closed.clear()
        crouchStands.clear()
    }

    private fun computeStandHeight(x: Int, y: Int, z: Int, bodyHeight: Double = height): Double {
        if (y > maxY || !isKnown(x, y, z) || !isKnown(x, y - 1, z)) {
            return Double.NaN
        }

        val below = shape(x, y - 1, z)
        val at = shape(x, y, z)
        if (at === FULL || below.isEmpty && at.isEmpty || isHazard(x, y - 1, z) || isHazard(x, y, z)) {
            return Double.NaN
        }

        val top = if (below === FULL && at.isEmpty) y.toDouble() else partialTop(below, at, y)
        return if (top.isNaN() || !isClear(box(x + 0.5, top, z + 0.5, bodyHeight))) Double.NaN else top
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
         * Height of a crouching player.
         */
        const val CROUCH_HEIGHT = 1.5

        /**
         * Highest ledge the player walks onto without jumping.
         */
        const val STEP = 0.6

        private const val STAND_REACH = 2
        private const val EPSILON = 1.0E-7
        private const val SUPPORT_PROBE = 1.0E-3

        /**
         * Offsets from a block to the trapdoors whose counting as open depends on it, see [isFloor].
         */
        private val TRAPDOOR_NEIGHBOURS = intArrayOf(-2, -1, 1)

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
