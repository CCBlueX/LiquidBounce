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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.ccbluex.liquidbounce.utils.math.ifNaN
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.hasRoom
import net.ccbluex.liquidbounce.utils.navigation.terrain.isOpen
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity.DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

internal fun interface MoveSink {
    /** The lists are reused; copy them to keep them. */
    fun accept(
        to: BlockPos,
        move: Move,
        cost: Double,
        breaks: List<BlockPos>,
        places: List<BlockPos>,
        opens: List<BlockPos>,
        jump: Jump?,
    )
}

internal fun interface JumpFilter {
    fun accept(x: Int, y: Int, z: Int): Boolean
}

/**
 * The moves out of a feet block. They only look at [terrain], so a node always yields the same moves. A node reached
 * by breaking or placing is not standable in [terrain]; it counts as standing on its floor. Not thread-safe.
 */
@Suppress("TooManyFunctions")
internal class Moves(private val terrain: Terrain, private val abilities: Abilities) {

    private val walk = if (abilities.canSprint) Costs.SPRINT else Costs.WALK
    private val canPlace = abilities.canPlace && abilities.placeableBlocks > 0
    private val maxDrop = max(abilities.safeFall, abilities.waterFall) + 1
    private val maxJumpDrop = abilities.safeFall + if (abilities.riskyJumps) RISKY_JUMP_DROP else 1
    private val jumpPenalty = if (abilities.riskyJumps) 0.0 else Costs.JUMP_PENALTY
    private val halfWidth = terrain.width / 2.0
    private val swim = Water(terrain, abilities.canSprint)
    private val jumps = Jumps(terrain, abilities)
    private val jumpTargets = JumpTargets(terrain, abilities.motion)

    private val breaks = ArrayList<BlockPos>(3)
    private val places = ArrayList<BlockPos>(1)
    private val opens = ArrayList<BlockPos>(1)
    private var breakCost = 0.0

    // The node the moves start from.
    private var from = BlockPos.ZERO
    private var fromHeight = 0.0
    private var fromStands = true
    private var fromFloorFactor = 1.0
    private var canJump = true
    private var isJumpWorth = EVERY_JUMP
    private lateinit var sink: MoveSink

    fun isNode(x: Int, y: Int, z: Int) = stands(x, y, z) || swims(x, y, z) || climbs(x, y, z) || crouches(x, y, z)

    fun isUnderwater(pos: BlockPos) = swims(pos.x, pos.y, pos.z) && swim.isSubmerged(pos.x, pos.y, pos.z)

    /** The cheapest move from [from] to [to], or null when there is none. */
    fun step(from: BlockPos, to: BlockPos): Step? {
        var best: Step? = null
        val sink = MoveSink { target, move, cost, breaks, places, opens, jump ->
            val current = best
            if (target == to && (current == null || cost < current.cost)) {
                val feet = Vec3(to.x + 0.5, height(to.x, to.y, to.z), to.z + 0.5)
                best = Step(
                    from, to, move, feet, breaks.toList(), places.toList(), opens.toList(), cost,
                    sneak = crouches(from.x, from.y, from.z) || crouches(to.x, to.y, to.z),
                    edge = isAtEdge(to, move), underwater = isUnderwater(to), jump = jump,
                )
            }
        }
        forEach(from, sink) { x, y, z -> x == to.x && y == to.y && z == to.z }
        return best
    }

    /** Passes every move out of [node] to [sink]. Jumps, each simulated, are only worked out to spots [isJumpWorth]. */
    fun forEach(node: BlockPos, sink: MoveSink, isJumpWorth: JumpFilter = EVERY_JUMP) {
        val (x, y, z) = node
        val water = swims(x, y, z)
        val climb = climbs(x, y, z)
        from = node
        fromHeight = height(x, y, z)
        fromStands = stands(x, y, z)
        val standing = fromStands || !water && !climb
        this.sink = sink
        this.isJumpWorth = isJumpWorth
        if (crouches(x, y, z)) {
            crouchMoves()
            return
        }

        val floor = floorBlock(x, z, fromHeight)
        fromFloorFactor = if (water || climb) 1.0 else Costs.floorFactor(floor)
        // Honey does not allow a full jump.
        canJump = water || climb || floor.jumpFactor >= 1f

        for (i in DX.indices) {
            val tx = x + DX[i]
            val tz = z + DZ[i]
            if (standing || climb) {
                traverse(tx, tz, standing)
                ascend(tx, tz)
            }
            if (standing) {
                fall(tx, tz)
                diagonal(x + DIAGONAL_X[i], z + DIAGONAL_Z[i])
            }

            swimInto(tx, y, tz)
            if (water) {
                swimOut(tx, tz)
            }
        }

        parkour()
        swimInto(x, y + 1, z)
        swimInto(x, y - 1, z)
        climb()
        if (standing) {
            pillar()
            downward()
        }
    }

    private fun traverse(tx: Int, tz: Int, standing: Boolean) {
        val y = from.y
        begin()

        val wet = isWet(tx, tz)
        val top = terrain.standHeight(tx, y, tz)
        when {
            !top.isNaN() -> {
                val jump = jumpCost(top - fromHeight)
                val fits = jump == 0.0 || !jump.isNaN() && hasJumpRoom(tx, tz)
                if (fits && canCross(tx, tz, max(fromHeight, top))) {
                    emit(tx, y, tz, Move.TRAVERSE, walkCost(tx, tz, top, wet) + jump)
                }
            }

            climbs(tx, y, tz) -> emit(tx, y, tz, Move.TRAVERSE, walkCost(tx, tz, y.toDouble(), wet))
            crouches(tx, y, tz) -> {
                if (terrain.crouchHeight(tx, y, tz) - fromHeight <= Terrain.STEP) {
                    emit(tx, y, tz, Move.TRAVERSE, Costs.SNEAK)
                }
            }

            else -> traverseChanging(tx, tz, standing, wet)
        }
    }

    /** A traverse that breaks the way free or places the floor. */
    private fun traverseChanging(tx: Int, tz: Int, standing: Boolean, wet: Boolean) {
        val y = from.y
        if (!passOrBreak(tx, y + 1, tz) || !passOrBreak(tx, y, tz)) {
            return
        }

        // The walker stands on the floor of the node even if that was placed on the way.
        val onFloor = standing && fromHeight <= y + EPSILON
        val floor = floorOrPlace(tx, y, tz) { canPlaceAgainst(tx, y - 1, tz, onFloor) }
        val jump = jumpCost(floor - fromHeight)
        if (breaks.isEmpty() && places.isEmpty() || jump.isNaN() || jump > 0.0 && !hasJumpRoom(tx, tz)) {
            return
        }
        val placeCost = if (breaks.isEmpty()) abilities.bridgeTicks else Costs.PLACE
        val cost = walkCost(tx, tz, floor, wet) + jump + breakCost + places.size * placeCost
        emit(tx, y, tz, Move.TRAVERSE, cost)
    }

    private fun diagonal(tx: Int, tz: Int) {
        val (x, y, z) = from
        begin()

        val top = terrain.standHeight(tx, y, tz)
        if (top.isNaN() || top - fromHeight > Terrain.STEP) {
            return
        }
        if (!terrain.isOpen(tx, y, z) || !terrain.isOpen(x, y, tz) || !canCross(tx, tz, max(fromHeight, top))) {
            return
        }

        emit(tx, y, tz, Move.DIAGONAL, DIAGONAL * walkCost(tx, tz, top, isWet(tx, tz)))
    }

    private fun ascend(tx: Int, tz: Int) {
        val y = from.y
        val ty = y + 1
        begin()

        if (!passOrBreak(from.x, y + 2, from.z)) {
            return
        }

        val top = terrain.standHeight(tx, ty, tz)
        if (!top.isNaN()) {
            val jump = jumpCost(top - fromHeight)
            if (!jump.isNaN() && canCross(tx, tz, top)) {
                emit(tx, ty, tz, Move.ASCEND, walkCost(tx, tz, top) + jump + breakCost)
            }
            return
        }

        val headBreaks = breaks.size
        if (!passOrBreak(tx, ty + 1, tz) || !passOrBreak(tx, ty, tz)) {
            return
        }

        val floor = floorOrPlace(tx, ty, tz) { isSolid(tx, y - 1, tz) }
        val jump = jumpCost(floor - fromHeight)
        if (jump.isNaN() || breaks.size == headBreaks && places.isEmpty()) {
            return
        }
        val cost = walkCost(tx, tz, floor) + jump + breakCost + places.size * Costs.PLACE
        emit(tx, ty, tz, Move.ASCEND, cost)
    }

    private fun fall(tx: Int, tz: Int) {
        val y = from.y
        if (!floorTop(tx, y, tz).isNaN() || terrain.isWater(tx, y, tz)) {
            return
        }

        begin()
        if (!passOrBreak(tx, y + 1, tz) || !passOrBreak(tx, y, tz)) {
            return
        }
        if (breaks.isEmpty() && !canCross(tx, tz, fromHeight)) {
            return
        }

        val landing = landing(tx, y - 1, tz, fromHeight) ?: return
        val blocks = ceil(landing.drop).toInt()
        val bounce = Costs.bounce(floorBlock(tx, tz, fromHeight - landing.drop), blocks)
        val cost = walk * fromFloorFactor + Costs.fall(blocks) + bounce + breakCost
        emit(tx, landing.y, tz, Move.FALL, cost)
    }

    /** Where a fall down column [x] [z] from block [top] with the feet at [height] ends, null unless safely. */
    private fun landing(x: Int, top: Int, z: Int, height: Double): Landing? {
        for (y in top downTo max(top - maxDrop, terrain.minY)) {
            val stand = terrain.standHeight(x, y, z)
            return when {
                swims(x, y, z) -> Landing(y, height - y).takeIf { it.drop <= abilities.waterFall }
                !stand.isNaN() -> Landing(y, height - stand).takeIf { it.drop <= abilities.safeFall }
                climbs(x, y, z) -> Landing(y, height - y)
                terrain.isPassable(x, y, z) -> continue
                else -> null
            }
        }

        return null
    }

    /** Jumps over a gap or dip to spots out of reach of the other moves, each simulated by [Jumps]. */
    private fun parkour() {
        val (x, y, z) = from
        if (!abilities.canParkour || !fromStands || terrain.isWater(x, y, z)) {
            return
        }

        begin()
        val feet = Vec3(x + 0.5, fromHeight, z + 0.5)
        // Drops to the ground below a course would use up the jumps worked out before the next block of it.
        var planned = 0
        var drops = 0
        jumpTargets.forEach(x, y, z, fromHeight, maxJumpDrop) { tx, ty, tz, top ->
            val drop = top < fromHeight - DROP_HEIGHT
            if (drop && drops >= MAX_JUMPS || !isJumpWorth.accept(tx, ty, tz)) {
                return@forEach planned < MAX_JUMPS
            }
            val plan = jumps.plan(from, fromHeight, BlockPos(tx, ty, tz), top)
            if (plan != null) {
                emit(tx, ty, tz, Move.PARKOUR, plan.ticks(feet) + jumpPenalty, plan)
                if (drop) drops++ else planned++
            }
            planned < MAX_JUMPS
        }
    }

    private fun pillar() {
        val (x, y, z) = from
        if (!canJump || !terrain.isPassable(x, y, z) || !isPlaceable(x, y, z)) {
            return
        }

        begin()
        if (!passOrBreak(x, y + 2, z)) {
            return
        }

        places += BlockPos(x, y, z)
        emit(x, y + 1, z, Move.PILLAR, Costs.JUMP + Costs.PLACE + breakCost)
    }

    private fun downward() {
        val (x, y, z) = from
        if (terrain.isPassable(x, y - 1, z)) {
            return
        }

        begin()
        val ticks = breakTicks(x, y - 1, z, floor = true)
        if (ticks.isNaN() || floorTop(x, y - 1, z).isNaN()) {
            return
        }

        breaks += BlockPos(x, y - 1, z)
        emit(x, y - 1, z, Move.DOWNWARD, ticks + Costs.BREAK_OVERHEAD + Costs.fall(1))
    }

    /** Swims into the water block; the swimming pose, which needs sprinting, only goes on from water. */
    private fun swimInto(tx: Int, ty: Int, tz: Int) {
        if (!swims(tx, ty, tz) || swim.isLow(tx, ty, tz) && !swims(from.x, from.y, from.z)) {
            return
        }

        begin()
        val sprints = abilities.canSprint && ty == from.y && swim.isSubmerged(tx, ty, tz)
        emit(tx, ty, tz, Move.SWIM, if (sprints) Costs.SPRINT_SWIM else Costs.SWIM)
    }

    private fun swimOut(tx: Int, tz: Int) {
        val y = from.y
        begin()
        if (stands(tx, y, tz)) {
            emit(tx, y, tz, Move.SWIM, Costs.SWIM)
        } else if (stands(tx, y + 1, tz) && terrain.isPassable(from.x, y + 2, from.z)) {
            emit(tx, y + 1, tz, Move.SWIM, Costs.SWIM + Costs.JUMP)
        }
    }

    private fun climb() {
        val (x, y, z) = from
        if (!terrain.isClimbable(x, y, z)) {
            return
        }

        begin()
        val canClimbUp = abilities.jumpClimbs || terrain.climbSide(x, y, z) != null
        if (canClimbUp && (climbs(x, y + 1, z) || stands(x, y + 1, z))) {
            emit(x, y + 1, z, Move.CLIMB_UP, Costs.CLIMB_UP)
        }
        if (climbs(x, y - 1, z)) {
            emit(x, y - 1, z, Move.CLIMB_DOWN, Costs.CLIMB_DOWN)
        } else if (!fromStands && terrain.isPassable(x, y - 1, z)) {
            // Off the bottom of a ladder or vine that ends above the ground.
            val landing = landing(x, y - 1, z, fromHeight) ?: return
            val blocks = ceil(landing.drop).toInt()
            val bounce = Costs.bounce(floorBlock(x, z, fromHeight - landing.drop), blocks)
            emit(x, landing.y, z, Move.FALL, Costs.fall(blocks) + bounce)
        }
    }

    /** Level moves out of a gap only a crouching player fits, where there is no room to jump. */
    private fun crouchMoves() {
        val y = from.y
        begin()
        for (i in DX.indices) {
            val tx = from.x + DX[i]
            val tz = from.z + DZ[i]
            val top = terrain.standHeight(tx, y, tz).ifNaN { terrain.crouchHeight(tx, y, tz) }
            if (!top.isNaN() && top - fromHeight <= Terrain.STEP) {
                emit(tx, y, tz, Move.TRAVERSE, Costs.SNEAK)
            }
        }
    }

    /** Whether the move to [to] goes level past a deadly drop. */
    private fun isAtEdge(to: BlockPos, move: Move): Boolean {
        val (x, y, z) = from
        val level = move == Move.TRAVERSE || move == Move.DIAGONAL
        if (!level || swims(x, y, z) || swims(to.x, to.y, to.z)) {
            return false
        }

        val toHeight = height(to.x, to.y, to.z)
        // Safe walk stops at every ledge higher than a step, the ones the move goes down included.
        if (abs(toHeight - fromHeight) > Terrain.STEP) {
            return false
        }

        return isNearDrop(x, y, z, fromHeight) || isNearDrop(to.x, to.y, to.z, toHeight)
    }

    /** Whether a player in [x] [y] [z] with the feet at [height] can walk off a side into a deadly fall. */
    private fun isNearDrop(x: Int, y: Int, z: Int, height: Double): Boolean {
        for (i in DX.indices) {
            val nx = x + DX[i]
            val nz = z + DZ[i]
            // Servers may never send the empty chunks of the void, so an unloaded side counts as one.
            if (!terrain.isKnown(nx, y, nz)) {
                return true
            }
            val open = terrain.isOpen(nx, y, nz) && floorTop(nx, y, nz).isNaN() && !terrain.isWater(nx, y, nz)
            if (open && landing(nx, y - 1, nz, height) == null) {
                return true
            }
        }
        return false
    }

    private fun begin() {
        breaks.clear()
        places.clear()
        breakCost = 0.0
    }

    private fun emit(x: Int, y: Int, z: Int, move: Move, cost: Double, jump: Jump? = null) {
        opens.clear()
        if (terrain.opensDoors) {
            val body = terrain.box(from.x + 0.5, fromHeight, from.z + 0.5)
            terrain.closedIn(body.minmax(terrain.box(x + 0.5, height(x, y, z), z + 0.5)), body, opens)
        }

        sink.accept(BlockPos(x, y, z), move, cost + opens.size * Costs.OPEN, breaks, places, opens, jump)
    }

    /** Ticks to walk one block onto the floor at [x] [z] with the feet at [top], or to swim it when [wet]. */
    private fun walkCost(x: Int, z: Int, top: Double, wet: Boolean = false) =
        if (wet) Costs.SWIM else walk * (fromFloorFactor + Costs.floorFactor(floorBlock(x, z, top))) / 2.0

    private fun isWet(tx: Int, tz: Int) = terrain.isWater(from.x, from.y, from.z) || terrain.isWater(tx, from.y, tz)

    /** The block below feet at [height] in column [x] [z] that sets the speed, friction and jump of the player. */
    private fun floorBlock(x: Int, z: Int, height: Double): Block =
        terrain.state(x, Mth.floor(height - DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5), z).block

    /** Extra ticks to rise [rise] blocks: nothing up to a step, a jump up to [MAX_JUMP], NaN beyond. */
    private fun jumpCost(rise: Double) = when {
        rise <= Terrain.STEP -> 0.0
        canJump && rise <= MAX_JUMP -> Costs.JUMP
        else -> Double.NaN
    }

    private fun hasJumpRoom(tx: Int, tz: Int) =
        terrain.isPassable(from.x, from.y + 2, from.z) && terrain.isPassable(tx, from.y + 2, tz)

    /** Whether no thin block on the edge to [tx] [tz], such as a closed door, is in the way of a body at [height]. */
    private fun canCross(tx: Int, tz: Int, height: Double) = !fromStands ||
        terrain.isClear(terrain.box((from.x + tx) / 2.0 + 0.5, height, (from.z + tz) / 2.0 + 0.5))

    private fun passOrBreak(x: Int, y: Int, z: Int): Boolean {
        if (terrain.isPassable(x, y, z)) {
            return true
        }

        val ticks = breakTicks(x, y, z)
        if (ticks.isNaN()) {
            return false
        }

        breaks += BlockPos(x, y, z)
        breakCost += ticks + Costs.BREAK_OVERHEAD
        return true
    }

    /** Ticks to break the cell; NaN when it must not be broken. Only [floor] allows the block the walker stands on. */
    private fun breakTicks(x: Int, y: Int, z: Int, floor: Boolean = false): Double {
        val stoodOn = x == from.x && z == from.z && y == from.y - 1
        if (!abilities.canBreak || !isHarmless(x, y, z) || stoodOn && !floor || !isDry(x, y, z)) {
            return Double.NaN
        }

        val state = terrain.state(x, y, z)
        if (state.isAir || terrain.state(x, y + 1, z).block is FallingBlock) {
            return Double.NaN
        }
        val ticks = abilities.breakTicks.applyAsDouble(state)
        return if (ticks.isFinite()) ticks else Double.NaN
    }

    private fun isDry(x: Int, y: Int, z: Int): Boolean {
        if (!terrain.state(x, y, z).fluidState.isEmpty || !terrain.state(x, y + 1, z).fluidState.isEmpty) {
            return false
        }
        for (i in DX.indices) {
            if (!terrain.state(x + DX[i], y, z + DZ[i]).fluidState.isEmpty) {
                return false
            }
        }
        return true
    }

    /** Feet height on the floor of [x] [y] [z]; without one the block below is placed if [hasSupport]. NaN if not. */
    private inline fun floorOrPlace(x: Int, y: Int, z: Int, hasSupport: () -> Boolean): Double {
        val floor = floorTop(x, y, z)
        if (!floor.isNaN() || !isPlaceable(x, y - 1, z) || !hasSupport()) {
            return floor
        }

        places += BlockPos(x, y - 1, z)
        return y.toDouble()
    }

    /** Whether a block placed at [x] [y] [z] has a side to place it against; the floor stood on counts if [onFloor]. */
    private fun canPlaceAgainst(x: Int, y: Int, z: Int, onFloor: Boolean): Boolean {
        if (isSolid(x, y - 1, z)) {
            return true
        }

        for (i in DX.indices) {
            val nx = x + DX[i]
            val nz = z + DZ[i]
            if (onFloor && nx == from.x && nz == from.z && y == from.y - 1 || isSolid(nx, y, nz)) {
                return true
            }
        }
        return false
    }

    private fun isHarmless(x: Int, y: Int, z: Int) = terrain.isKnown(x, y, z) && !terrain.isHazard(x, y, z)

    private fun isSolid(x: Int, y: Int, z: Int) = isHarmless(x, y, z) && !terrain.isPassable(x, y, z)

    private fun isPlaceable(x: Int, y: Int, z: Int) =
        canPlace && isHarmless(x, y, z) && terrain.state(x, y, z).canBeReplaced()

    private fun stands(x: Int, y: Int, z: Int) = !terrain.standHeight(x, y, z).isNaN()

    private fun swims(x: Int, y: Int, z: Int) = swim.fits(x, y, z)

    private fun climbs(x: Int, y: Int, z: Int) = terrain.isClimbable(x, y, z) && terrain.hasRoom(x, y, z)

    private fun crouches(x: Int, y: Int, z: Int) = !stands(x, y, z) && !terrain.crouchHeight(x, y, z).isNaN() &&
        !terrain.isWater(x, y, z) && !terrain.isClimbable(x, y, z)

    /** Feet height in [x] [y] [z]; a node the terrain does not have yet stands on its floor or a placed block. */
    private fun height(x: Int, y: Int, z: Int): Double {
        val stand = terrain.standHeight(x, y, z)
        return when {
            !stand.isNaN() -> stand
            swims(x, y, z) || climbs(x, y, z) -> y.toDouble()
            else -> terrain.crouchHeight(x, y, z).ifNaN { floorTop(x, y, z).ifNaN { y.toDouble() } }
        }
    }

    /** Top of the block below [x] [y] [z] the feet would rest on, whatever is in the feet block; NaN without one. */
    private fun floorTop(x: Int, y: Int, z: Int): Double {
        if (!isHarmless(x, y - 1, z)) {
            return Double.NaN
        }

        var top = Double.NaN
        for (box in terrain.shape(x, y - 1, z).toAabbs()) {
            val boxTop = y - 1 + box.maxY
            val inFootprint = box.minX < 0.5 + halfWidth && box.maxX > 0.5 - halfWidth &&
                box.minZ < 0.5 + halfWidth && box.maxZ > 0.5 - halfWidth
            if (inFootprint && boxTop >= y && boxTop < y + 1 && !(boxTop <= top)) {
                top = boxTop
            }
        }
        return top
    }

    private class Landing(val y: Int, val drop: Double)

    private companion object {
        const val MAX_JUMP = 1.25
        const val EPSILON = 1.0E-6

        val DIAGONAL = Mth.SQRT_OF_TWO

        val DX = intArrayOf(1, -1, 0, 0)
        val DZ = intArrayOf(0, 0, 1, -1)
        val DIAGONAL_X = intArrayOf(1, 1, -1, -1)
        val DIAGONAL_Z = intArrayOf(1, -1, 1, -1)

        /** Jumps tried from one spot, nearest first. */
        const val MAX_JUMPS = 24

        /** How much lower a jump has to land to count as a drop, the floor beside a slab included. */
        const val DROP_HEIGHT = 0.25
        const val RISKY_JUMP_DROP = 3

        val EVERY_JUMP = JumpFilter { _, _, _ -> true }
    }

}
