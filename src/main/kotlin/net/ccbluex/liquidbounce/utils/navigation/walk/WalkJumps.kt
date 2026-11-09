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

import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity.BASE_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.BASE_VERTICAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.abs

/**
 * How a jump from one feet block to another is made: run from [runUp] towards [aim], sprinting, first with [hops]
 * for speed if set, jump [takeoff] ticks later (or run off the edge without jumping when [jumps] is false) and keep
 * running towards the middle of the target block in the air until landing.
 */
class WalkJump(
    /**
     * Where the run starts, at rest, on the floor of the block jumped from.
     */
    val runUp: Vec3,
    val takeoff: Int,
    val jumps: Boolean,
    val airTicks: Int,

    /**
     * How far the player could still slide, along the shorter way, and keep standing on the block landed on, or for
     * a ladder or vine caught how far inside its block the player gets; the more, the more a jump may be off.
     */
    val margin: Double,

    /**
     * Jumps on the same floor the run starts with, one right after the other, which land faster than running gets.
     */
    val hops: Int,

    /**
     * What the run on the ground heads for: the middle of the target block, or a little beside it to get around
     * something in the way.
     */
    val aim: Vec3,
) {

    /**
     * Estimated duration in ticks, the walk back to [runUp] included.
     */
    fun ticks(from: Vec3): Double {
        val back = from.horizontalDistanceTo(runUp)
        val backTicks = if (back > RUN_UP_TOLERANCE) back * WalkCosts.WALK + STOP_TICKS else 0.0
        return backTicks + takeoff + airTicks + LANDING_TICKS
    }

    companion object {
        /**
         * How close to [runUp] counts as standing there.
         */
        const val RUN_UP_TOLERANCE = 0.15
        private const val STOP_TICKS = 4.0
        private const val LANDING_TICKS = 2.0
    }
}

/**
 * Finds out by simulation whether and how a jump between two feet blocks works, see [WalkJump].
 *
 * Not thread-safe: use it from one thread at a time.
 */
@Suppress("TooManyFunctions")
internal class WalkJumps(private val terrain: NavigationTerrain, private val abilities: WalkAbilities) {

    val canSprint: Boolean
        get() = abilities.canSprint

    /**
     * Least margin of a jump; a walker that takes no risks only makes jumps with room to spare.
     */
    private val minMargin = if (abilities.riskyJumps) MIN_MARGIN else CAREFUL_MARGIN

    private val motion = PlayerMotion(terrain, abilities.motion)
    private val start = motion.copy()
    private val ground = ArrayList<PlayerMotion>()
    private val pool = ArrayList<PlayerMotion>()
    private val estimates = ArrayList<Estimate>()

    private var targetX = 0.0
    private var targetZ = 0.0
    private var aimX = 0.0
    private var aimZ = 0.0
    private var target = BlockPos.ZERO
    private var targetHeight = 0.0
    private var fromX = 0.0
    private var fromZ = 0.0
    private var dirX = 0.0
    private var dirZ = 0.0
    private var distance = 0.0
    private var airTicks = 0

    /**
     * Whether the target is a ladder or vine the jump catches rather than lands on.
     */
    private var catches = false

    /**
     * How far ahead of the middle of the block jumped from its floor reaches, which hops must land short of.
     */
    private var edge = Double.MAX_VALUE

    /**
     * The best way to jump from [from], standing at [fromHeight], to [to] with the feet at [toHeight], or null.
     */
    fun plan(from: BlockPos, fromHeight: Double, to: BlockPos, toHeight: Double): WalkJump? {
        aim(from.x + 0.5, from.z + 0.5, to, toHeight)
        if (!abilities.riskyJumps && !isSafe(fromHeight)) {
            return null
        }

        val center = Vec3(fromX, fromHeight, fromZ)
        var best = fromRest(center, hops = 0)
        if (best == null || best.margin < GOOD_MARGIN) {
            // Longer run-ups are faster, but the right take-off spot can be anywhere on the way.
            val longest = runUpLength(fromHeight, MAX_RUN_UP)
            var back = RUN_UP_SPACING
            while (back < longest + RUN_UP_SPACING && (best == null || best.margin < GOOD_MARGIN)) {
                val length = minOf(back, longest)
                best = better(best, fromRest(Vec3(fromX - dirX * length, fromHeight, fromZ - dirZ * length), hops = 0))
                back += RUN_UP_SPACING
            }
        }
        if ((best == null || best.margin < minMargin) && distance <= HOP_REACH) {
            best = better(best, hopped(fromHeight))
        }
        // Around something in the way.
        for (offset in SIDE_AIMS) {
            if (best != null && best.margin >= minMargin) {
                break
            }
            offsetAim(offset)
            best = better(best, fromRest(center, hops = 0))
        }

        return best?.takeIf { it.margin >= minMargin }
    }

    /**
     * The jump from [from], standing at [fromHeight], to [to] with the feet at [toHeight] that lands best from rest
     * anywhere on the way back from the middle of [from], tried in small steps; or null.
     */
    fun refine(from: BlockPos, fromHeight: Double, to: BlockPos, toHeight: Double): WalkJump? {
        aim(from.x + 0.5, from.z + 0.5, to, toHeight)
        val longest = runUpLength(fromHeight, MAX_RUN_UP)
        var best: WalkJump? = null
        var back = 0.0
        while (back <= longest) {
            best = better(best, fromRest(Vec3(fromX - dirX * back, fromHeight, fromZ - dirZ * back), hops = 0))
            back += RUN_UP_STEP
        }
        return best?.takeIf { it.margin >= minMargin }
    }

    /**
     * A jump out of reach of a run made after hops along a runway, started so far back that the last hop lands
     * right before the edge and the jump keeps its speed.
     */
    private fun hopped(fromHeight: Double): WalkJump? {
        val runway = runUpLength(fromHeight, MAX_HOP_RUN_UP)
        edge = edgeLength(fromHeight)
        for (hops in 1..MAX_HOPS) {
            val travel = hopTravel(fromHeight, runway, hops)
            val back = travel + HOP_LEAD - edge
            if (travel.isNaN() || back > runway) {
                return null
            }

            val jump = fromRest(Vec3(fromX - dirX * back, fromHeight, fromZ - dirZ * back), hops)
            if (jump != null && jump.margin >= minMargin) {
                return jump
            }
        }
        return null
    }

    /**
     * How far [hops] from rest at the back of the [runway] carry the player along it; NaN when one does not land on
     * the same floor.
     */
    private fun hopTravel(height: Double, runway: Double, hops: Int): Double {
        motion.reset(Vec3(fromX - dirX * runway, height, fromZ - dirZ * runway), PlayerMotion.RESTING, onGround = true)
        val startX = motion.x
        val startZ = motion.z
        if (hop(hops, height) < 0) {
            return Double.NaN
        }
        return (motion.x - startX) * dirX + (motion.z - startZ) * dirZ
    }

    /**
     * Makes [motion] jump [hops] times in a row towards the target; the ticks it took, or -1 when one did not land
     * at [height] on the runway, before the [edge] of the block jumped from.
     */
    private fun hop(hops: Int, height: Double): Int {
        var ticks = 0
        repeat(hops) {
            var air = 0
            do {
                motion.tick(yawToTarget(motion.x, motion.z), 1, 0, jump = air == 0, sprint = abilities.canSprint)
                air++
            } while (!motion.onGround && air < MAX_AIR_TICKS)
            val progress = (motion.x - fromX) * dirX + (motion.z - fromZ) * dirZ
            if (!motion.onGround || abs(motion.y - height) > LANDING_PROBE || progress > edge) {
                return -1
            }
            ticks += air
        }
        return ticks
    }

    /**
     * How far ahead of the middle of the block jumped from, towards the target, the floor still holds the player.
     */
    private fun edgeLength(height: Double): Double {
        var length = 0.0
        while (length + RUN_UP_STEP <= MAX_EDGE) {
            val feet = Vec3(fromX + dirX * (length + RUN_UP_STEP), height, fromZ + dirZ * (length + RUN_UP_STEP))
            if (!terrain.isSupported(feet)) {
                break
            }
            length += RUN_UP_STEP
        }
        return length
    }

    private fun better(a: WalkJump?, b: WalkJump?) = if (a == null || b != null && b.margin > a.margin) b else a

    /**
     * The jump onto [to] from where [state], a player on the ground, is now, moving this tick with [yaw]; null
     * when no take-off in reach lands on it.
     */
    fun fromHere(state: PlayerMotion, yaw: Float, to: BlockPos, toHeight: Double): WalkJump? {
        // Once per tick, so every promising take-off can be simulated in full.
        aim(state.x, state.z, to, toHeight)
        var best = attempt(state, state.position, yaw, hops = 0, validations = Int.MAX_VALUE)
        for (offset in SIDE_AIMS) {
            aim(state.x, state.z, to, toHeight)
            offsetAim(offset)
            best = better(best, attempt(state, state.position, yaw, hops = 0, validations = Int.MAX_VALUE))
        }
        return best
    }

    /**
     * The keys for this tick, moving with [yaw], that let [state], a player in the air, land best on [to]; null
     * when no keys land on it.
     */
    fun airKeys(state: PlayerMotion, yaw: Float, to: BlockPos, toHeight: Double): AirKeys? {
        aim(state.x, state.z, to, toHeight)
        var best: AirKeys? = null
        for (forward in 1 downTo -1) {
            for (strafe in STRAFES) {
                motion.copyFrom(state)
                motion.tick(yaw, forward, strafe, jump = false, sprint = abilities.canSprint)
                val margin = land(state.y)
                if (!margin.isNaN() && (best == null || margin > best.margin + KEY_PREFERENCE)) {
                    best = AirKeys(forward, strafe, margin)
                }
            }
        }
        return best
    }

    class AirKeys(val forward: Int, val strafe: Int, val margin: Double)

    private fun aim(fromX: Double, fromZ: Double, to: BlockPos, toHeight: Double) {
        this.fromX = fromX
        this.fromZ = fromZ
        targetX = to.x + 0.5
        targetZ = to.z + 0.5
        aimX = targetX
        aimZ = targetZ
        target = to
        targetHeight = toHeight
        catches = terrain.isClimbable(to.x, to.y, to.z)
        edge = Double.MAX_VALUE
        dirX = targetX - fromX
        dirZ = targetZ - fromZ
        distance = Mth.length(dirX, dirZ)
        if (distance > 1.0E-6) {
            dirX /= distance
            dirZ /= distance
        }
    }

    /**
     * How far back from the middle of the block jumped from, away from the target, the player can stand on the same
     * floor to run up.
     */
    private fun runUpLength(height: Double, max: Double): Double {
        var length = 0.0
        while (length + RUN_UP_STEP <= max) {
            val x = fromX - dirX * (length + RUN_UP_STEP)
            val z = fromZ - dirZ * (length + RUN_UP_STEP)
            val feet = Vec3(x, height, z)
            if (!terrain.isSupported(feet) || !terrain.isClear(terrain.box(feet))) {
                break
            }
            length += RUN_UP_STEP
        }
        return length
    }

    private fun fromRest(runUp: Vec3, hops: Int): WalkJump? {
        start.reset(runUp, PlayerMotion.RESTING, onGround = true)
        return attempt(start, runUp, null, hops, VALIDATED_TAKEOFFS)
    }

    /**
     * Runs from [state] towards the target, after [hops] that have to land on the same floor, estimates for every
     * tick on the ground how jumping then would land and simulates up to [validations] of the most promising
     * take-offs, and running off the edge, in full. The first tick moves with [firstYaw] when given, every other one
     * aims at the target.
     */
    @Suppress("CognitiveComplexMethod", "LongParameterList")
    private fun attempt(state: PlayerMotion, runUp: Vec3, firstYaw: Float?, hops: Int, validations: Int): WalkJump? {
        recycle()
        motion.copyFrom(state)
        val hopTicks = hop(hops, runUp.y)
        if (hopTicks < 0) {
            return null
        }

        var ranOff: PlayerMotion? = null
        for (tick in 0 until MAX_GROUND_TICKS) {
            ground += snapshot(motion)
            motion.tick(yawAt(tick, firstYaw), 1, 0, jump = false, sprint = abilities.canSprint)
            // Grazing a wall, the run slides on along it; only a wall it stops at ends it.
            if (motion.horizontalCollision && motion.horizontalSpeed < STOPPED_SPEED) {
                break
            }
            if (!motion.onGround) {
                ranOff = snapshot(motion)
                break
            }
        }

        var best: WalkJump? = null
        if (ranOff != null && targetHeight < ranOff.y) {
            best = jump(ranOff, runUp, hopTicks + ground.size, jumps = false, hops)
            pool += ranOff
        }

        estimates.clear()
        for (takeoff in ground.indices) {
            motion.copyFrom(ground[takeoff])
            motion.tick(yawAt(takeoff, firstYaw), 1, 0, jump = true, sprint = abilities.canSprint)
            val margin = if (motion.onGround) Double.NaN else estimate(motion)
            if (!margin.isNaN()) {
                estimates += Estimate(takeoff, margin, snapshot(motion))
            }
        }

        estimates.sortByDescending(Estimate::margin)
        for (i in 0 until min(estimates.size, validations)) {
            val estimate = estimates[i]
            val jump = jump(estimate.state, runUp, hopTicks + estimate.takeoff, jumps = true, hops) ?: continue
            best = better(best, jump)
            if (jump.margin >= GOOD_MARGIN) {
                break
            }
        }
        estimates.forEach { pool += it.state }

        return best
    }

    private fun yawAt(tick: Int, firstYaw: Float?) =
        if (tick == 0 && firstYaw != null) firstYaw else yawTo(aimX, aimZ, motion.x, motion.z)

    /**
     * Margin of the landing on the target a jump from [state], just off the ground, makes when nothing else is in
     * the way; NaN when it misses. Cheap enough to try every take-off.
     */
    @Suppress("CognitiveComplexMethod")
    private fun estimate(state: PlayerMotion): Double {
        var x = state.x
        var y = state.y
        var z = state.z
        var motionX = state.motionX
        var motionY = state.motionY
        var motionZ = state.motionZ
        val acceleration = if (state.sprinting) SPRINT_AIR_ACCELERATION else AIR_ACCELERATION
        val minX = target.x - PlayerMotion.HALF_WIDTH
        val maxX = target.x + 1.0 + PlayerMotion.HALF_WIDTH
        val minZ = target.z - PlayerMotion.HALF_WIDTH
        val maxZ = target.z + 1.0 + PlayerMotion.HALF_WIDTH

        for (tick in 0 until MAX_AIR_TICKS) {
            val toX = targetX - x
            val toZ = targetZ - z
            val length = Mth.length(toX, toZ)
            if (length > 1.0E-4) {
                val push = when {
                    catches || !overshoots(x, y, z, motionX, motionY, motionZ) -> acceleration
                    else -> -AIR_ACCELERATION
                }
                motionX += toX / length * push
                motionZ += toZ / length * push
            }

            val over = x > minX && x < maxX && z > minZ && z < maxZ
            val nextY = y + motionY
            if (!catches && motionY < 0.0 && nextY <= targetHeight && y >= targetHeight - LANDING_PROBE) {
                return if (over) margin(x + motionX, z + motionZ) else Double.NaN
            }
            y = nextY

            x += motionX
            z += motionZ
            if (catches && isInTarget(x, y, z)) {
                return margin(x, z)
            }
            // Rising against the side, the player slides up it; only sinking into it misses.
            val hitsSide = motionY <= 0.0 && y < targetHeight && x > minX && x < maxX && z > minZ && z < maxZ
            if (hitsSide || y < targetHeight - MAX_BELOW) {
                return Double.NaN
            }

            motionX *= BASE_HORIZONTAL_AIR_DRAG
            motionZ *= BASE_HORIZONTAL_AIR_DRAG
            motionY = (motionY - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
        }
        return Double.NaN
    }

    private fun margin(x: Double, z: Double) =
        minOf(x - target.x, target.x + 1.0 - x, z - target.z, target.z + 1.0 - z)

    /**
     * Jumping from [start], already off the ground, onto the target, or null when it does not land there.
     */
    private fun jump(start: PlayerMotion, runUp: Vec3, takeoff: Int, jumps: Boolean, hops: Int): WalkJump? {
        motion.copyFrom(start)
        val margin = land(runUp.y)
        return if (margin.isNaN()) {
            null
        } else {
            WalkJump(runUp, takeoff, jumps, airTicks, margin, hops, Vec3(aimX, targetHeight, aimZ))
        }
    }

    /**
     * Keeps [motion] running towards the target in the air until it lands; the margin of the landing on the target,
     * right on touching down or after a few careful steps on, whichever is more; NaN when it lands anywhere else,
     * too hard, or not at all. The ticks in the air go to [airTicks].
     */
    private fun land(fromHeight: Double): Double {
        val floor = min(targetHeight, fromHeight) - MAX_BELOW
        airTicks = 1
        while (!motion.onGround && motion.y > floor && airTicks < MAX_AIR_TICKS) {
            if (catches && isInTarget(motion.x, motion.y, motion.z)) {
                return margin(motion.x, motion.z)
            }
            val back = !catches && overshoots(motion)
            motion.tick(
                yawToTarget(motion.x, motion.z), if (back) -1 else 1, 0, jump = false,
                sprint = abilities.canSprint && !back,
            )
            airTicks++
        }

        if (catches || !motion.onGround || motion.landingFall > maxFall()) {
            return Double.NaN
        }
        val touchdown = footing()
        // A landing on the lip of a block is followed by a few careful steps onto it, as into a hole between walls.
        repeat(SETTLE_TICKS) {
            motion.tick(yawToTarget(motion.x, motion.z), 1, 0, jump = false, sprint = false, sneak = true)
        }
        val settled = footing()
        return if (touchdown.isNaN() || settled > touchdown) settled else touchdown
    }

    /**
     * How far [motion] could still slide and keep standing on the target, NaN when it does not stand on it.
     */
    private fun footing() = when {
        !motion.onGround || abs(motion.y - targetHeight) > HEIGHT_TOLERANCE -> Double.NaN
        motion.standsOn(target.x, target.y, target.z) -> motion.footing(target.x, target.y, target.z)
        motion.standsOn(target.x, target.y - 1, target.z) -> motion.footing(target.x, target.y - 1, target.z)
        else -> Double.NaN
    }

    private fun overshoots(motion: PlayerMotion) =
        overshoots(motion.x, motion.y, motion.z, motion.motionX, motion.motionY, motion.motionZ)

    /**
     * Whether pushing on towards the middle of the target would carry a player at [x] [y] [z], moving [motionX]
     * [motionY] [motionZ], past it before it comes down to the target's height; then it holds back instead.
     */
    @Suppress("LongParameterList")
    private fun overshoots(
        x: Double,
        y: Double,
        z: Double,
        motionX: Double,
        motionY: Double,
        motionZ: Double,
    ): Boolean {
        val toX = targetX - x
        val toZ = targetZ - z
        val remaining = Mth.length(toX, toZ)
        if (remaining < 1.0E-4) {
            return false
        }

        var speed = (motionX * toX + motionZ * toZ) / remaining

        var height = y
        var vertical = motionY
        var travel = 0.0
        for (tick in 0 until MAX_AIR_TICKS) {
            speed += SPRINT_AIR_ACCELERATION
            travel += speed
            height += vertical
            if (travel > remaining + BRAKE_SLACK) {
                return true
            }
            if (vertical < 0.0 && height <= targetHeight) {
                return false
            }
            speed *= BASE_HORIZONTAL_AIR_DRAG
            vertical = (vertical - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
        }
        return false
    }

    private fun isInTarget(x: Double, y: Double, z: Double) =
        Mth.floor(x) == target.x && Mth.floor(y) == target.y && Mth.floor(z) == target.z

    private fun maxFall() =
        if (abilities.riskyJumps) abilities.safeFall + RISKY_EXTRA_FALL else abilities.safeFall.toDouble()

    private class Estimate(val takeoff: Int, val margin: Double, val state: PlayerMotion)

    /**
     * Whether every column between the jump's start and target catches a fall the walker survives.
     */
    private fun isSafe(height: Double): Boolean {
        var travelled = HALF_BLOCK
        while (travelled < distance - HALF_BLOCK) {
            val x = Mth.floor(fromX + dirX * travelled)
            val z = Mth.floor(fromZ + dirZ * travelled)
            if (!catchesFall(x, Mth.floor(height), z, height)) {
                return false
            }
            travelled += SAFETY_STEP
        }
        return true
    }

    private fun catchesFall(x: Int, top: Int, z: Int, height: Double): Boolean {
        for (y in top downTo maxOf(top - abilities.safeFall - 1, terrain.minY)) {
            val stand = terrain.standHeight(x, y, z)
            when {
                terrain.isWater(x, y, z) -> return true
                !stand.isNaN() -> return height - stand <= abilities.safeFall
                !terrain.isPassable(x, y, z) -> return true
            }
        }
        return false
    }

    private fun yawToTarget(x: Double, z: Double) = yawTo(targetX, targetZ, x, z)

    private fun yawTo(toX: Double, toZ: Double, x: Double, z: Double): Float =
        (atan2(toZ - z, toX - x) * Mth.RAD_TO_DEG - QUARTER_TURN).toFloat()

    /**
     * Moves the aim of the run [offset] blocks to the left of the middle of the target, across the jump.
     */
    private fun offsetAim(offset: Double) {
        aimX = targetX - dirZ * offset
        aimZ = targetZ + dirX * offset
    }

    private fun snapshot(source: PlayerMotion): PlayerMotion =
        if (pool.isEmpty()) source.copy() else pool.removeLast().also { it.copyFrom(source) }

    private fun recycle() {
        pool += ground
        ground.clear()
    }

    private companion object {
        const val MAX_RUN_UP = 1.6
        const val RUN_UP_SPACING = 0.3

        /**
         * Hops: at most this many in a row, on runways up to this long, for jumps up to [HOP_REACH] apart; the last
         * lands [HOP_LEAD] blocks before the edge.
         */
        const val MAX_HOPS = 2
        const val MAX_HOP_RUN_UP = 8.0
        const val HOP_REACH = 6.2
        const val HOP_LEAD = 0.3

        /**
         * Farthest ahead of the middle of a block its floor is looked for.
         */
        const val MAX_EDGE = 1.5
        const val RUN_UP_STEP = 0.1
        const val MAX_GROUND_TICKS = 16
        const val MAX_AIR_TICKS = 60
        const val MAX_BELOW = 4.0
        const val LANDING_PROBE = 1.0E-4
        const val HEIGHT_TOLERANCE = 1.0E-3
        const val SETTLE_TICKS = 3
        const val STOPPED_SPEED = 0.01

        /**
         * How far past the middle of the target a landing may go before the air control holds back; braking for the
         * exact middle lands short.
         */
        const val BRAKE_SLACK = 0.25
        const val HALF_BLOCK = 0.5
        const val SAFETY_STEP = 0.5
        const val QUARTER_TURN = 90.0
        const val RISKY_EXTRA_FALL = 4.5
        const val VALIDATED_TAKEOFFS = 4

        /**
         * How far beside the middle of the target, across the jump, runs may aim when straight at it does not work.
         */
        val SIDE_AIMS = doubleArrayOf(0.3, -0.3)

        /**
         * How much better a margin must be for other keys than the ones tried first: forward, no strafe.
         */
        const val KEY_PREFERENCE = 0.02
        val STRAFES = intArrayOf(0, 1, -1)
        const val AIR_ACCELERATION = 0.02f * 0.98f
        const val SPRINT_AIR_ACCELERATION = 0.025999999f * 0.98f

        /**
         * Least margin of a jump, see [WalkJump.margin].
         */
        const val MIN_MARGIN = 0.15
        const val CAREFUL_MARGIN = 0.35

        /**
         * A margin that makes a run-up from further back not worth it.
         */
        const val GOOD_MARGIN = 0.45
    }

}
