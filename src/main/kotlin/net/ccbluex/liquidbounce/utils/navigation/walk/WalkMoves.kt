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

import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

internal fun interface MoveSink {
    @Suppress("LongParameterList")
    fun accept(
        to: BlockPos,
        move: WalkMove,
        cost: Double,
        breaks: List<BlockPos>,
        places: List<BlockPos>,
        opens: List<BlockPos>,
        jump: WalkJump?,
    )
}

internal fun interface JumpFilter {
    fun accept(x: Int, y: Int, z: Int): Boolean
}

/**
 * The moves out of a feet block. Moves only look at [terrain], so a node always yields the same moves.
 *
 * A node reached by breaking or placing is not standable in [terrain]; it counts as standing on its floor. Doors the
 * terrain counts as open are opened on the way, see [NavigationTerrain.opensDoors].
 *
 * Not thread-safe: use it from one thread at a time.
 */
@Suppress("TooManyFunctions")
internal class WalkMoves(private val terrain: NavigationTerrain, private val abilities: WalkAbilities) {

    private val walk = if (abilities.canSprint) WalkCosts.SPRINT else WalkCosts.WALK
    private val canPlace = abilities.canPlace && abilities.placeableBlocks > 0
    private val maxDrop = max(abilities.safeFall, abilities.waterFall) + 1
    private val halfWidth = terrain.width / 2.0
    private val swim = WalkWater(terrain, abilities.canSprint)
    private val jumps = WalkJumps(terrain, abilities)
    private val jumpTargets = WalkJumpTargets(terrain, abilities.motion)
    private val maxJumpDrop = abilities.safeFall + if (abilities.riskyJumps) RISKY_JUMP_DROP else 1

    /**
     * Extra cost of a jump for a walker that takes no risks, so it only jumps where that saves a good while.
     */
    private val jumpPenalty = if (abilities.riskyJumps) 0.0 else WalkCosts.JUMP_PENALTY

    private val breaks = ArrayList<BlockPos>(3)
    private val places = ArrayList<BlockPos>(1)
    private val opens = ArrayList<BlockPos>(1)
    private var breakCost = 0.0
    private var jumpFilter = JumpFilter { _, _, _ -> true }

    /**
     * The node the moves start from, its feet height and whether it is standable in [terrain].
     */
    private var origin = BlockPos.ZERO
    private var originHeight = 0.0
    private var originStands = true

    /**
     * Floor factor of the node the moves start from, see [WalkCosts.floorFactor].
     */
    private var fromFloor = 1.0

    /**
     * Whether the floor of the node the moves start from allows a full jump; honey does not.
     */
    private var canJump = true

    fun isNode(x: Int, y: Int, z: Int) =
        !terrain.standHeight(x, y, z).isNaN() || isWater(x, y, z) || isClimb(x, y, z) || isCrouch(x, y, z)

    fun feet(pos: BlockPos) = Vec3(pos.x + 0.5, height(pos.x, pos.y, pos.z), pos.z + 0.5)

    /**
     * The cheapest move from [from] to [to], or null when there is none.
     */
    fun step(from: BlockPos, to: BlockPos): WalkStep? {
        var best: WalkStep? = null
        val sink = MoveSink { target, move, cost, breaks, places, opens, jump ->
            val current = best
            if (target == to && (current == null || cost < current.cost)) {
                best = WalkStep(
                    from, to, move, feet(to), breaks.toList(), places.toList(), opens.toList(), cost,
                    isSneaking(from, to, move), isUnderwater(to), jump,
                )
            }
        }
        forEach(from, sink) { x, y, z -> x == to.x && y == to.y && z == to.z }
        return best
    }

    /**
     * Passes every move out of [node] to [sink]. The lists passed are reused; copy them to keep them. Jumps are only
     * worked out to spots [isJumpWorth] accepts, as each is simulated.
     */
    fun forEach(node: BlockPos, sink: MoveSink, isJumpWorth: JumpFilter = JumpFilter { _, _, _ -> true }) {
        jumpFilter = isJumpWorth
        val x = node.x
        val y = node.y
        val z = node.z
        val water = isWater(x, y, z)
        val climb = isClimb(x, y, z)
        val stands = !terrain.standHeight(x, y, z).isNaN()
        val standing = stands || !water && !climb
        val height = height(x, y, z)
        origin = node
        originHeight = height
        originStands = stands
        if (isCrouch(x, y, z)) {
            crouchMoves(node, height, sink)
            return
        }

        val floor = floorBlock(x, z, height)
        fromFloor = if (water || climb) 1.0 else WalkCosts.floorFactor(floor)
        canJump = water || climb || floor.jumpFactor >= 1f

        for (i in 0 until 4) {
            val dx = DX[i]
            val dz = DZ[i]

            if (standing || climb) {
                traverse(node, height, standing, x + dx, z + dz, sink)
                ascend(node, height, x + dx, z + dz, sink)
            }
            if (standing) {
                fall(node, height, x + dx, z + dz, sink)
                diagonal(node, height, DIAGONAL_X[i], DIAGONAL_Z[i], sink)
            }

            swimInto(x + dx, y, z + dz, sink)
            if (water) {
                swimOut(node, x + dx, z + dz, sink)
            }
        }

        jump(node, height, sink)

        swimInto(x, y + 1, z, sink)
        swimInto(x, y - 1, z, sink)
        climb(node, sink)

        if (standing) {
            pillar(node, sink)
            downward(node, sink)
        }
    }

    private fun traverse(from: BlockPos, height: Double, standing: Boolean, tx: Int, tz: Int, sink: MoveSink) {
        val y = from.y
        begin()

        val wet = terrain.isWater(from.x, y, from.z) || terrain.isWater(tx, y, tz)
        val top = terrain.standHeight(tx, y, tz)
        when {
            !top.isNaN() -> {
                val jump = jumpCost(top - height)
                val fits = jump == 0.0 || !jump.isNaN() && hasJumpRoom(from, tx, tz)
                if (fits && canCross(from, tx, tz, max(height, top))) {
                    emit(tx, y, tz, WalkMove.TRAVERSE, walking(wet, tx, tz, top) + jump, sink)
                }
            }

            isClimb(tx, y, tz) -> emit(tx, y, tz, WalkMove.TRAVERSE, walking(wet, tx, tz, y.toDouble()), sink)
            isCrouch(tx, y, tz) -> {
                if (terrain.crouchHeight(tx, y, tz) - height <= NavigationTerrain.STEP) {
                    emit(tx, y, tz, WalkMove.TRAVERSE, WalkCosts.SNEAK, sink)
                }
            }

            else -> traverseChanging(from, height, standing, tx, tz, wet, sink)
        }
    }

    /**
     * Ticks to walk one block onto the floor at [x] [z] with the feet at [top], or to swim when [wet].
     */
    private fun walking(wet: Boolean, x: Int, z: Int, top: Double) =
        if (wet) WalkCosts.SWIM else walk * (fromFloor + WalkCosts.floorFactor(floorBlock(x, z, top))) / 2.0

    /**
     * The block below feet at [height] in column [x] [z] that sets the speed, friction and jump of the player.
     */
    private fun floorBlock(x: Int, z: Int, height: Double): Block =
        terrain.state(x, Mth.floor(height - FLOOR_PROBE), z).block

    /**
     * A traverse that breaks the way free or places the floor.
     */
    @Suppress("LongParameterList")
    private fun traverseChanging(
        from: BlockPos,
        height: Double,
        standing: Boolean,
        tx: Int,
        tz: Int,
        wet: Boolean,
        sink: MoveSink,
    ) {
        val y = from.y
        if (!clear(tx, y + 1, tz, from) || !clear(tx, y, tz, from)) {
            return
        }

        val onFloor = standing && height <= y + EPSILON
        val floor = floorOrPlace(tx, y, tz) { canPlaceAgainst(tx, y - 1, tz, from, onFloor) }
        val jump = jumpCost(floor - height)
        if (breaks.isEmpty() && places.isEmpty() || jump.isNaN() || jump > 0.0 && !hasJumpRoom(from, tx, tz)) {
            return
        }
        val cost = walking(wet, tx, tz, floor) + jump + breakCost + places.size * WalkCosts.PLACE
        emit(tx, y, tz, WalkMove.TRAVERSE, cost, sink)
    }

    /**
     * Feet height on the floor of [x] [y] [z]. Without a floor, the block below gets placed when it is replaceable
     * and [hasSupport]; NaN when it cannot.
     */
    private inline fun floorOrPlace(x: Int, y: Int, z: Int, hasSupport: () -> Boolean): Double {
        val floor = floorTop(x, y, z)
        if (!floor.isNaN() || !isPlaceable(x, y - 1, z) || !hasSupport()) {
            return floor
        }

        places += BlockPos(x, y - 1, z)
        return y.toDouble()
    }

    /**
     * Extra ticks to rise [rise] blocks between feet heights: nothing up to a step, a jump up to [MAX_JUMP],
     * NaN beyond.
     */
    private fun jumpCost(rise: Double) = when {
        rise <= NavigationTerrain.STEP -> 0.0
        canJump && rise <= MAX_JUMP -> WalkCosts.JUMP
        else -> Double.NaN
    }

    private fun hasJumpRoom(from: BlockPos, tx: Int, tz: Int) =
        terrain.isPassable(from.x, from.y + 2, from.z) && terrain.isPassable(tx, from.y + 2, tz)

    private fun diagonal(from: BlockPos, height: Double, dx: Int, dz: Int, sink: MoveSink) {
        val x = from.x
        val y = from.y
        val z = from.z
        begin()

        val top = terrain.standHeight(x + dx, y, z + dz)
        if (top.isNaN() || top - height > NavigationTerrain.STEP) {
            return
        }

        val cornersClear = terrain.isPassable(x + dx, y, z) && terrain.isPassable(x + dx, y + 1, z) &&
            terrain.isPassable(x, y, z + dz) && terrain.isPassable(x, y + 1, z + dz)
        if (!cornersClear || !canCross(from, x + dx, z + dz, max(height, top))) {
            return
        }

        val wet = terrain.isWater(x, y, z) || terrain.isWater(x + dx, y, z + dz)
        emit(x + dx, y, z + dz, WalkMove.DIAGONAL, DIAGONAL * walking(wet, x + dx, z + dz, top), sink)
    }

    private fun ascend(from: BlockPos, height: Double, tx: Int, tz: Int, sink: MoveSink) {
        val y = from.y
        val ty = y + 1
        begin()

        if (!clear(from.x, y + 2, from.z, from)) {
            return
        }

        val top = terrain.standHeight(tx, ty, tz)
        if (!top.isNaN()) {
            val jump = jumpCost(top - height)
            if (!jump.isNaN() && canCross(from, tx, tz, top)) {
                emit(tx, ty, tz, WalkMove.ASCEND, walking(false, tx, tz, top) + jump + breakCost, sink)
            }
            return
        }

        val headBreaks = breaks.size
        if (!clear(tx, ty + 1, tz, from) || !clear(tx, ty, tz, from)) {
            return
        }

        val floor = floorOrPlace(tx, ty, tz) { isSolid(tx, y - 1, tz) }
        val jump = jumpCost(floor - height)
        if (jump.isNaN() || breaks.size == headBreaks && places.isEmpty()) {
            return
        }
        val cost = walking(false, tx, tz, floor) + jump + breakCost + places.size * WalkCosts.PLACE
        emit(tx, ty, tz, WalkMove.ASCEND, cost, sink)
    }

    private fun fall(from: BlockPos, height: Double, tx: Int, tz: Int, sink: MoveSink) {
        val y = from.y
        if (!floorTop(tx, y, tz).isNaN() || terrain.isWater(tx, y, tz)) {
            return
        }

        begin()
        if (!clear(tx, y + 1, tz, from) || !clear(tx, y, tz, from)) {
            return
        }

        if (breaks.isEmpty() && !canCross(from, tx, tz, height)) {
            return
        }

        val landing = landing(tx, y - 1, tz, height) ?: return
        val bounce = WalkCosts.bounce(floorBlock(tx, tz, height - landing.drop), ceil(landing.drop).toInt())
        val cost = walk * fromFloor + fallCost(landing.drop) + bounce + breakCost
        emit(tx, landing.y, tz, WalkMove.FALL, cost, sink)
    }

    /**
     * Where a fall down column [x] [z] from block [top] with the feet at [height] ends safely: in water, on a spot to
     * stand or in a climbable block, within the allowed drop. Null when it ends anywhere else.
     */
    private fun landing(x: Int, top: Int, z: Int, height: Double): Landing? {
        for (ly in top downTo max(top - maxDrop, terrain.minY)) {
            val stand = terrain.standHeight(x, ly, z)
            return when {
                isWater(x, ly, z) -> Landing(ly, height - ly).takeIf { it.drop <= abilities.waterFall }
                !stand.isNaN() -> Landing(ly, height - stand).takeIf { it.drop <= abilities.safeFall }
                // Climbable blocks catch the fall.
                isClimb(x, ly, z) -> Landing(ly, height - ly)
                terrain.isPassable(x, ly, z) -> continue
                else -> null
            }
        }

        return null
    }

    /**
     * Jumps from the edge of a gap to spots out of reach of the other moves, each simulated by [WalkJumps].
     */
    private fun jump(from: BlockPos, height: Double, sink: MoveSink) {
        if (!abilities.canParkour || !originStands || terrain.isWater(from.x, from.y, from.z)) {
            return
        }

        begin()
        val feet = Vec3(from.x + 0.5, height, from.z + 0.5)
        // Drops to the ground below a course would use up the jumps worked out before the next block of it.
        var planned = 0
        var drops = 0
        jumpTargets.forEach(from.x, from.y, from.z, height, maxJumpDrop) { x, y, z, top ->
            val drop = top < height - DROP_HEIGHT
            if (drop && drops >= MAX_JUMPS) {
                return@forEach planned < MAX_JUMPS
            }
            val plan = if (jumpFilter.accept(x, y, z)) jumps.plan(from, height, BlockPos(x, y, z), top) else null
            if (plan != null) {
                emit(x, y, z, WalkMove.PARKOUR, plan.ticks(feet) + jumpPenalty, sink, plan)
                if (drop) drops++ else planned++
            }
            planned < MAX_JUMPS
        }
    }

    private fun pillar(from: BlockPos, sink: MoveSink) {
        val x = from.x
        val y = from.y
        val z = from.z
        if (!canPlace || !canJump || !terrain.isPassable(x, y, z) || !terrain.state(x, y, z).canBeReplaced()) {
            return
        }

        begin()
        if (!clear(x, y + 2, z, from)) {
            return
        }

        places += BlockPos(x, y, z)
        emit(x, y + 1, z, WalkMove.PILLAR, WalkCosts.JUMP + WalkCosts.PLACE + breakCost, sink)
    }

    private fun downward(from: BlockPos, sink: MoveSink) {
        val x = from.x
        val y = from.y
        val z = from.z
        if (terrain.isPassable(x, y - 1, z)) {
            return
        }

        begin()
        val ticks = breakTicks(x, y - 1, z, from, floor = true)
        if (ticks.isNaN() || floorTop(x, y - 1, z).isNaN()) {
            return
        }

        breaks += BlockPos(x, y - 1, z)
        emit(x, y - 1, z, WalkMove.DOWNWARD, ticks + WalkCosts.BREAK_OVERHEAD + WalkCosts.fall(1), sink)
    }

    /**
     * Swims into the water block [tx] [ty] [tz]; sprinting along under water, which only a swimmer coming from water
     * can do, also where the swimming pose is needed.
     */
    private fun swimInto(tx: Int, ty: Int, tz: Int, sink: MoveSink) {
        if (!isWater(tx, ty, tz) || swim.isLow(tx, ty, tz) && !isWater(origin.x, origin.y, origin.z)) {
            return
        }

        begin()
        val sprints = abilities.canSprint && ty == origin.y && swim.isSubmerged(tx, ty, tz)
        emit(tx, ty, tz, WalkMove.SWIM, if (sprints) WalkCosts.SPRINT_SWIM else WalkCosts.SWIM, sink)
    }

    private fun swimOut(from: BlockPos, tx: Int, tz: Int, sink: MoveSink) {
        val y = from.y
        begin()
        if (!terrain.standHeight(tx, y, tz).isNaN()) {
            emit(tx, y, tz, WalkMove.SWIM, WalkCosts.SWIM, sink)
        } else if (!terrain.standHeight(tx, y + 1, tz).isNaN() && terrain.isPassable(from.x, y + 2, from.z)) {
            emit(tx, y + 1, tz, WalkMove.SWIM, WalkCosts.SWIM + WalkCosts.JUMP, sink)
        }
    }

    private fun climb(from: BlockPos, sink: MoveSink) {
        val x = from.x
        val y = from.y
        val z = from.z
        begin()

        if (terrain.isClimbable(x, y, z) && (isClimb(x, y + 1, z) || !terrain.standHeight(x, y + 1, z).isNaN())) {
            emit(x, y + 1, z, WalkMove.CLIMB_UP, WalkCosts.CLIMB_UP, sink)
        }
        if (isClimb(x, y - 1, z)) {
            emit(x, y - 1, z, WalkMove.CLIMB_DOWN, WalkCosts.CLIMB_DOWN, sink)
        }
    }

    /**
     * Level moves out of a gap only a crouching player fits, where there is no room to jump.
     */
    private fun crouchMoves(from: BlockPos, height: Double, sink: MoveSink) {
        begin()
        for (i in 0 until 4) {
            val tx = from.x + DX[i]
            val tz = from.z + DZ[i]
            val top = terrain.standHeight(tx, from.y, tz).takeUnless { it.isNaN() }
                ?: terrain.crouchHeight(tx, from.y, tz)
            if (!top.isNaN() && top - height <= NavigationTerrain.STEP) {
                emit(tx, from.y, tz, WalkMove.TRAVERSE, WalkCosts.SNEAK, sink)
            }
        }
    }

    /**
     * Whether the move has to be made sneaking: into, through or out of a gap only a crouching player fits, or
     * walking level past a drop the player does not survive.
     */
    private fun isSneaking(from: BlockPos, to: BlockPos, move: WalkMove): Boolean {
        if (isCrouch(from.x, from.y, from.z) || isCrouch(to.x, to.y, to.z)) {
            return true
        }
        if (move != WalkMove.TRAVERSE && move != WalkMove.DIAGONAL ||
            isWater(from.x, from.y, from.z) || isWater(to.x, to.y, to.z)) {
            return false
        }

        val fromHeight = height(from.x, from.y, from.z)
        val toHeight = height(to.x, to.y, to.z)
        // Sneaking stops at every ledge higher than a step, the ones the move goes down included.
        if (abs(toHeight - fromHeight) > NavigationTerrain.STEP) {
            return false
        }

        return isNearDrop(from.x, from.y, from.z, fromHeight) || isNearDrop(to.x, to.y, to.z, toHeight)
    }

    /**
     * Whether a player standing in [x] [y] [z] with the feet at [height] can walk off a side into a fall it does not
     * survive.
     */
    private fun isNearDrop(x: Int, y: Int, z: Int, height: Double): Boolean {
        for (i in 0 until 4) {
            val nx = x + DX[i]
            val nz = z + DZ[i]
            val open = terrain.isPassable(nx, y, nz) && terrain.isPassable(nx, y + 1, nz) &&
                floorTop(nx, y, nz).isNaN() && !terrain.isWater(nx, y, nz)
            if (open && landing(nx, y - 1, nz, height) == null) {
                return true
            }
        }
        return false
    }

    /**
     * Whether [pos] is in water with no air for the head.
     */
    fun isUnderwater(pos: BlockPos) =
        isWater(pos.x, pos.y, pos.z) && swim.isSubmerged(pos.x, pos.y, pos.z)

    private fun begin() {
        breaks.clear()
        places.clear()
        breakCost = 0.0
    }

    @Suppress("LongParameterList")
    private fun emit(x: Int, y: Int, z: Int, move: WalkMove, cost: Double, sink: MoveSink, jump: WalkJump? = null) {
        opens.clear()
        if (terrain.opensDoors) {
            val from = terrain.box(origin.x + 0.5, originHeight, origin.z + 0.5)
            terrain.closedIn(from.minmax(terrain.box(x + 0.5, height(x, y, z), z + 0.5)), from, opens)
        }

        sink.accept(BlockPos(x, y, z), move, cost + opens.size * WalkCosts.OPEN, breaks, places, opens, jump)
    }

    /**
     * Whether the body gets over the edge between [from] and the block next to it at [tx] [tz] with the feet at
     * [height]. Thin blocks on the edge, such as closed doors, are in the way. Moves out of a node the terrain does
     * not have yet, as it is reached by breaking or placing, are not checked.
     */
    private fun canCross(from: BlockPos, tx: Int, tz: Int, height: Double) = !originStands ||
        terrain.isClear(terrain.box((from.x + tx) / 2.0 + 0.5, height, (from.z + tz) / 2.0 + 0.5))

    /**
     * Whether the cell is passable or can be broken; a broken cell is added to the move.
     */
    private fun clear(x: Int, y: Int, z: Int, from: BlockPos): Boolean {
        if (terrain.isPassable(x, y, z)) {
            return true
        }

        val ticks = breakTicks(x, y, z, from, floor = false)
        if (ticks.isNaN()) {
            return false
        }

        breaks += BlockPos(x, y, z)
        breakCost += ticks + WalkCosts.BREAK_OVERHEAD
        return true
    }

    /**
     * Ticks to break the cell, NaN when it must not be broken. [floor] allows the block below [from].
     */
    @Suppress("ReturnCount")
    private fun breakTicks(x: Int, y: Int, z: Int, from: BlockPos, floor: Boolean): Double {
        if (!abilities.canBreak || !terrain.isKnown(x, y, z) || terrain.isHazard(x, y, z)) {
            return Double.NaN
        }
        if (!floor && x == from.x && z == from.z && y == from.y - 1) {
            return Double.NaN
        }

        val state = terrain.state(x, y, z)
        val above = terrain.state(x, y + 1, z)
        if (state.isAir || !state.fluidState.isEmpty || !above.fluidState.isEmpty || above.block is FallingBlock) {
            return Double.NaN
        }
        for (i in 0 until 4) {
            if (!terrain.state(x + DX[i], y, z + DZ[i]).fluidState.isEmpty) {
                return Double.NaN
            }
        }

        val ticks = abilities.breakTicks(state)
        return if (ticks.isFinite()) ticks else Double.NaN
    }

    private fun isPlaceable(x: Int, y: Int, z: Int) =
        canPlace && terrain.isKnown(x, y, z) && !terrain.isHazard(x, y, z) && terrain.state(x, y, z).canBeReplaced()

    /**
     * Whether a block placed at [x] [y] [z] has a side to be placed against. The floor of [from] counts when
     * [onFloor], since the walker stands on it even if it was placed on the way.
     */
    private fun canPlaceAgainst(x: Int, y: Int, z: Int, from: BlockPos, onFloor: Boolean): Boolean {
        if (isSolid(x, y - 1, z)) {
            return true
        }

        for (i in 0 until 4) {
            val nx = x + DX[i]
            val nz = z + DZ[i]
            if (onFloor && nx == from.x && nz == from.z && y == from.y - 1 || isSolid(nx, y, nz)) {
                return true
            }
        }
        return false
    }

    private fun isSolid(x: Int, y: Int, z: Int) =
        terrain.isKnown(x, y, z) && !terrain.isHazard(x, y, z) && !terrain.shape(x, y, z).isEmpty

    private fun isWater(x: Int, y: Int, z: Int) = swim.fits(x, y, z)

    private fun isClimb(x: Int, y: Int, z: Int) = terrain.isClimbable(x, y, z) && isBodyClear(x, y, z)

    private fun isCrouch(x: Int, y: Int, z: Int) =
        terrain.standHeight(x, y, z).isNaN() && !terrain.crouchHeight(x, y, z).isNaN() &&
            !terrain.isWater(x, y, z) && !terrain.isClimbable(x, y, z)

    private fun isBodyClear(x: Int, y: Int, z: Int) = terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5))

    /**
     * Feet height in [x] [y] [z]; a node that is not standable in the terrain stands on its floor or a placed block.
     */
    private fun height(x: Int, y: Int, z: Int): Double {
        val stand = terrain.standHeight(x, y, z)
        if (!stand.isNaN()) {
            return stand
        }
        if (isWater(x, y, z) || isClimb(x, y, z)) {
            return y.toDouble()
        }
        val crouch = terrain.crouchHeight(x, y, z)
        if (!crouch.isNaN()) {
            return crouch
        }

        val floor = floorTop(x, y, z)
        return if (floor.isNaN()) y.toDouble() else floor
    }

    /**
     * Top of the block below [x] [y] [z] the feet would rest on, ignoring the feet block; NaN without one.
     */
    private fun floorTop(x: Int, y: Int, z: Int): Double {
        if (!terrain.isKnown(x, y - 1, z) || terrain.isHazard(x, y - 1, z)) {
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

    private fun fallCost(drop: Double) = WalkCosts.fall(ceil(drop).toInt())

    private class Landing(val y: Int, val drop: Double)

    private companion object {
        const val MAX_JUMP = 1.25
        const val EPSILON = 1.0E-6

        /**
         * How far below the feet the floor that moves the player is looked up, as vanilla does.
         */
        const val FLOOR_PROBE = 0.500001
        val DIAGONAL = Mth.SQRT_OF_TWO

        val DX = intArrayOf(1, -1, 0, 0)
        val DZ = intArrayOf(0, 0, 1, -1)
        val DIAGONAL_X = intArrayOf(1, 1, -1, -1)
        val DIAGONAL_Z = intArrayOf(1, -1, 1, -1)

        /**
         * Jumps tried from one spot, nearest first.
         */
        const val MAX_JUMPS = 24

        /**
         * How much lower a jump has to land to count as a drop, the floor beside a slab included.
         */
        const val DROP_HEIGHT = 0.25
        const val RISKY_JUMP_DROP = 3
    }

}
