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

import net.ccbluex.liquidbounce.utils.entity.PlayerMotion
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity.BASE_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.BASE_VERTICAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

/** A jump between two feet blocks: sprint from [runUp] towards [aim], jump [takeoff] ticks later, steer in the air. */
class Jump(
    /** Where the run starts, at rest. */
    val runUp: Vec3,
    val takeoff: Int,

    /** False to run off the edge without jumping. */
    val jumps: Boolean,
    val airTicks: Int,

    /** How far the landing could still slide and stay on its block, or how deep it gets into a ladder caught. */
    val margin: Double,

    /** Jumps in a row on the floor of the run-up before the run, for speed. */
    val hops: Int,
    val aim: Vec3,
) {

    /** Estimated duration in ticks, the walk from [from] back to [runUp] included. */
    fun ticks(from: Vec3): Double {
        val back = from.horizontalDistanceTo(runUp)
        val backTicks = if (back > RUN_UP_TOLERANCE) back * Costs.WALK + STOP_TICKS else 0.0
        return backTicks + takeoff + airTicks + LANDING_TICKS
    }

    companion object {
        const val RUN_UP_TOLERANCE = 0.15
        private const val STOP_TICKS = 4.0
        private const val LANDING_TICKS = 2.0
    }
}

/** Finds out by simulation whether and how a jump between two feet blocks works. Not thread-safe. */
@Suppress("TooManyFunctions")
internal class Jumps(private val terrain: Terrain, private val abilities: Abilities) {

    val canSprint: Boolean
        get() = abilities.canSprint

    private val minMargin = if (abilities.riskyJumps) MIN_MARGIN else CAREFUL_MARGIN
    private val maxFall = abilities.safeFall + if (abilities.riskyJumps) RISKY_EXTRA_FALL else 0.0

    private val motion = PlayerMotion(terrain, abilities.motion)
    private val rest = motion.copy()
    private val ground = ArrayList<PlayerMotion>()
    private val pool = ArrayList<PlayerMotion>()
    private val estimates = ArrayList<Estimate>()

    private var fromX = 0.0
    private var fromY = 0.0
    private var fromZ = 0.0
    private var target = BlockPos.ZERO
    private var targetX = 0.0
    private var targetZ = 0.0
    private var targetHeight = 0.0

    /** Whether the target is a ladder or vine the jump catches rather than lands on. */
    private var catches = false
    private var dirX = 0.0
    private var dirZ = 0.0
    private var distance = 0.0
    private var aimX = 0.0
    private var aimZ = 0.0

    /** How far ahead of the middle of the block jumped from its floor reaches, which hops must land short of. */
    private var edge = Double.MAX_VALUE
    private var airTicks = 0

    /** Where [trace] puts the position after every simulated tick, or null. */
    private var path: MutableList<Vec3>? = null

    /** The best way to jump from [from], standing at [fromHeight], to [to] with the feet at [toHeight], or null. */
    fun plan(from: BlockPos, fromHeight: Double, to: BlockPos, toHeight: Double): Jump? {
        setCourse(from.x + 0.5, fromHeight, from.z + 0.5, to, toHeight)
        if (!abilities.riskyJumps && !isSafe()) {
            return null
        }

        val center = behind(0.0)
        var best = fromRest(center)
        if (!best.has(GOOD_MARGIN)) {
            // Longer run-ups are faster, but the right take-off spot can be anywhere on the way.
            val longest = runUpLength(MAX_RUN_UP)
            var back = RUN_UP_SPACING
            while (back < longest + RUN_UP_SPACING && !best.has(GOOD_MARGIN)) {
                best = better(best, fromRest(behind(minOf(back, longest))))
                back += RUN_UP_SPACING
            }
        }
        if (!best.has(minMargin) && distance <= HOP_REACH) {
            best = better(best, hopped())
        }
        for (offset in SIDE_AIMS) {
            if (best.has(minMargin)) {
                break
            }
            aimBeside(offset)
            best = better(best, fromRest(center))
        }

        return best?.takeIf { it.margin >= minMargin }
    }

    /** Like [plan], but from the run-up that lands best, tried in small steps back from the middle of [from]. */
    fun refine(from: BlockPos, fromHeight: Double, to: BlockPos, toHeight: Double): Jump? {
        setCourse(from.x + 0.5, fromHeight, from.z + 0.5, to, toHeight)
        val longest = runUpLength(MAX_RUN_UP)
        var best: Jump? = null
        var back = 0.0
        while (back <= longest) {
            best = better(best, fromRest(behind(back)))
            back += RUN_UP_STEP
        }
        return best?.takeIf { it.margin >= minMargin }
    }

    /** The jump onto [to] from where [state], a player on the ground, is now, moving this tick with [yaw]. */
    fun fromHere(state: PlayerMotion, yaw: Float, to: BlockPos, toHeight: Double): Jump? {
        setCourse(state.x, state.y, state.z, to, toHeight)
        val here = state.position
        var best = attempt(state, here, yaw, hops = 0, validations = Int.MAX_VALUE)
        for (offset in SIDE_AIMS) {
            aimBeside(offset)
            best = better(best, attempt(state, here, yaw, hops = 0, validations = Int.MAX_VALUE))
        }
        return best
    }

    /**
     * The keys for this tick, moving with [yaw], that let [state], a player in the air, land best on [to]. With
     * [keepSpeed], the first keys in the order tried, forward first, that land well enough, as the landing is a
     * take-off.
     */
    fun airKeys(
        state: PlayerMotion,
        yaw: Float,
        to: BlockPos,
        toHeight: Double,
        keepSpeed: Boolean = false,
    ): AirKeys? {
        setCourse(state.x, state.y, state.z, to, toHeight)
        var best: AirKeys? = null
        for (forward in 1 downTo -1) {
            for (strafe in STRAFES) {
                motion.copyFrom(state)
                motion.tick(yaw, forward, strafe, jump = false, sprint = abilities.canSprint)
                val margin = land()
                if (keepSpeed && margin >= minMargin) {
                    return AirKeys(forward, strafe, margin)
                }
                if (!margin.isNaN() && (best == null || margin > best.margin + KEY_PREFERENCE)) {
                    best = AirKeys(forward, strafe, margin)
                }
            }
        }
        return best
    }

    /**
     * Replays [jump] onto [to], with the feet at [toHeight], from [state], which moves this tick with [yaw] or towards
     * the aim when null: the positions tick for tick into [path], [state] first. The index of the last position on
     * the ground before the jump, or -1 when [state] is in the air already or the replay fails.
     */
    @Suppress("LongParameterList")
    fun trace(
        state: PlayerMotion,
        yaw: Float?,
        to: BlockPos,
        toHeight: Double,
        jump: Jump,
        path: MutableList<Vec3>,
    ): Int {
        setCourse(state.x, state.y, state.z, to, toHeight)
        aimX = jump.aim.x
        aimZ = jump.aim.z
        motion.copyFrom(state)
        path.clear()
        path += motion.position
        this.path = path
        try {
            var takeoff = -1
            if (motion.onGround) {
                val hopTicks = hop(jump.hops)
                if (hopTicks < 0) {
                    return -1
                }
                // Running off the edge, the last tick on the ground is the one that leaves it.
                val groundTicks = jump.takeoff - hopTicks - if (jump.jumps) 0 else 1
                for (tick in 0 until groundTicks) {
                    run(yawAt(tick, yaw))
                }
                takeoff = path.lastIndex
                run(yawAt(groundTicks, yaw), jump = jump.jumps)
            }
            land()
            return takeoff
        } finally {
            this.path = null
        }
    }

    class AirKeys(val forward: Int, val strafe: Int, val margin: Double)

    private fun setCourse(fromX: Double, fromY: Double, fromZ: Double, to: BlockPos, toHeight: Double) {
        this.fromX = fromX
        this.fromY = fromY
        this.fromZ = fromZ
        target = to
        targetX = to.x + 0.5
        targetZ = to.z + 0.5
        targetHeight = toHeight
        catches = terrain.isClimbable(to.x, to.y, to.z)
        aimX = targetX
        aimZ = targetZ
        edge = Double.MAX_VALUE
        dirX = targetX - fromX
        dirZ = targetZ - fromZ
        distance = Mth.length(dirX, dirZ)
        if (distance > 1.0E-6) {
            dirX /= distance
            dirZ /= distance
        }
    }

    /** Aims the run [offset] blocks to the left of the middle of the target, to get around something in the way. */
    private fun aimBeside(offset: Double) {
        aimX = targetX - dirZ * offset
        aimZ = targetZ + dirX * offset
    }

    /** The spot [distance] blocks behind where the jump starts, away from the target; ahead of it when negative. */
    private fun behind(distance: Double) = Vec3(fromX - dirX * distance, fromY, fromZ - dirZ * distance)

    private fun runUpLength(max: Double): Double {
        var length = 0.0
        while (length + RUN_UP_STEP <= max && canStandAt(behind(length + RUN_UP_STEP))) {
            length += RUN_UP_STEP
        }
        return length
    }

    private fun canStandAt(feet: Vec3) = terrain.isSupported(feet) && terrain.isClear(terrain.box(feet))

    private fun edgeLength(): Double {
        var length = 0.0
        while (length + RUN_UP_STEP <= MAX_EDGE && terrain.isSupported(behind(-(length + RUN_UP_STEP)))) {
            length += RUN_UP_STEP
        }
        return length
    }

    /** A jump out of reach of a run: hops along a runway first, the last landing right before the edge. */
    private fun hopped(): Jump? {
        val runway = runUpLength(MAX_HOP_RUN_UP)
        edge = edgeLength()
        for (hops in 1..MAX_HOPS) {
            val travel = hopTravel(runway, hops)
            val back = travel + HOP_LEAD - edge
            if (travel.isNaN() || back > runway) {
                return null
            }

            val jump = fromRest(behind(back), hops)
            if (jump.has(minMargin)) {
                return jump
            }
        }
        return null
    }

    /** How far [hops] from rest at the back of the [runway] get along it; NaN when one leaves its floor. */
    private fun hopTravel(runway: Double, hops: Int): Double {
        motion.reset(behind(runway), PlayerMotion.RESTING, onGround = true)
        val startX = motion.x
        val startZ = motion.z
        if (hop(hops) < 0) {
            return Double.NaN
        }
        return (motion.x - startX) * dirX + (motion.z - startZ) * dirZ
    }

    /** Makes [motion] hop [hops] times in a row; the ticks it took, or -1 when a hop leaves the floor before [edge]. */
    private fun hop(hops: Int): Int {
        var ticks = 0
        repeat(hops) {
            var air = 0
            do {
                run(yawToTarget(), jump = air == 0)
                air++
            } while (!motion.onGround && air < MAX_AIR_TICKS)
            val progress = (motion.x - fromX) * dirX + (motion.z - fromZ) * dirZ
            if (!motion.onGround || abs(motion.y - fromY) > LANDING_PROBE || progress > edge) {
                return -1
            }
            ticks += air
        }
        return ticks
    }

    private fun fromRest(runUp: Vec3, hops: Int = 0): Jump? {
        rest.reset(runUp, PlayerMotion.RESTING, onGround = true)
        return attempt(rest, runUp, null, hops, VALIDATED_TAKEOFFS)
    }

    /** Runs on from [state] after [hops] and simulates running off the edge and the [validations] best take-offs. */
    private fun attempt(state: PlayerMotion, runUp: Vec3, firstYaw: Float?, hops: Int, validations: Int): Jump? {
        pool += ground
        ground.clear()
        motion.copyFrom(state)
        val hopTicks = hop(hops)
        if (hopTicks < 0) {
            return null
        }

        var best: Jump? = null
        // Off a bouncing slime block, leaving the ground without a jump goes up as well.
        if (runToEdge(firstYaw) && (targetHeight < motion.y || motion.motionY > 0.0)) {
            best = fly(runUp, hopTicks + ground.size, jumps = false, hops)
        }

        estimateTakeoffs(firstYaw)
        for (i in 0 until min(estimates.size, validations)) {
            val estimate = estimates[i]
            motion.copyFrom(estimate.state)
            val jump = fly(runUp, hopTicks + estimate.takeoff, jumps = true, hops) ?: continue
            best = better(best, jump)
            if (jump.margin >= GOOD_MARGIN) {
                break
            }
        }
        estimates.forEach { pool += it.state }
        return best
    }

    /** Runs [motion] on, with a snapshot before every tick in [ground]; whether it ran off an edge. */
    private fun runToEdge(firstYaw: Float?): Boolean {
        for (tick in 0 until MAX_GROUND_TICKS) {
            ground += snapshot()
            run(yawAt(tick, firstYaw))
            // Grazing a wall, the run slides on along it; only a wall it stops at ends it.
            if (motion.horizontalCollision && motion.horizontalSpeed < STOPPED_SPEED) {
                return false
            }
            if (!motion.onGround) {
                return true
            }
        }
        return false
    }

    private fun estimateTakeoffs(firstYaw: Float?) {
        estimates.clear()
        for (takeoff in ground.indices) {
            motion.copyFrom(ground[takeoff])
            run(yawAt(takeoff, firstYaw), jump = true)
            val margin = if (motion.onGround) Double.NaN else estimate()
            if (!margin.isNaN()) {
                estimates += Estimate(takeoff, margin, snapshot())
            }
        }
        estimates.sortByDescending(Estimate::margin)
    }

    private fun run(yaw: Float, jump: Boolean = false) {
        motion.tick(yaw, 1, 0, jump, abilities.canSprint)
        path?.add(motion.position)
    }

    private fun yawAt(tick: Int, firstYaw: Float?) =
        if (tick == 0 && firstYaw != null) firstYaw else yawTo(aimX, aimZ)

    private fun yawToTarget() = yawTo(targetX, targetZ)

    private fun yawTo(x: Double, z: Double): Float =
        (atan2(z - motion.z, x - motion.x) * Mth.RAD_TO_DEG - QUARTER_TURN).toFloat()

    /** Margin of the landing [motion], just off the ground, makes with nothing in the way; NaN when it misses. */
    @Suppress("CognitiveComplexMethod")
    private fun estimate(): Double {
        var x = motion.x
        var y = motion.y
        var z = motion.z
        var motionX = motion.motionX
        var motionY = motion.motionY
        var motionZ = motion.motionZ
        val acceleration = if (motion.sprinting) SPRINT_AIR_ACCELERATION else AIR_ACCELERATION

        repeat(MAX_AIR_TICKS) {
            val toX = targetX - x
            val toZ = targetZ - z
            val length = Mth.length(toX, toZ)
            if (length > 1.0E-4) {
                val speed = (motionX * toX + motionZ * toZ) / length
                val push = if (catches || !overshoots(length, speed, y, motionY)) acceleration else -AIR_ACCELERATION
                motionX += toX / length * push
                motionZ += toZ / length * push
            }

            val nextY = y + motionY
            if (!catches && motionY < 0.0 && nextY <= targetHeight && y >= targetHeight - LANDING_PROBE) {
                return if (isOver(x, z)) margin(x + motionX, z + motionZ) else Double.NaN
            }
            x += motionX
            y = nextY
            z += motionZ
            if (catches && isInTarget(x, y, z)) {
                return margin(x, z)
            }
            // Rising against the side, the player slides up it; only sinking into it misses.
            val hitsSide = motionY <= 0.0 && y < targetHeight && isOver(x, z)
            if (hitsSide || y < targetHeight - MAX_BELOW) {
                return Double.NaN
            }

            motionX *= BASE_HORIZONTAL_AIR_DRAG
            motionY = (motionY - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
            motionZ *= BASE_HORIZONTAL_AIR_DRAG
        }
        return Double.NaN
    }

    private fun isOver(x: Double, z: Double) =
        x > target.x - PlayerMotion.HALF_WIDTH && x < target.x + 1.0 + PlayerMotion.HALF_WIDTH &&
            z > target.z - PlayerMotion.HALF_WIDTH && z < target.z + 1.0 + PlayerMotion.HALF_WIDTH

    private fun isInTarget(x: Double, y: Double, z: Double) =
        Mth.floor(x) == target.x && Mth.floor(y) == target.y && Mth.floor(z) == target.z

    private fun margin(x: Double, z: Double) =
        minOf(x - target.x, target.x + 1.0 - x, z - target.z, target.z + 1.0 - z)

    private fun fly(runUp: Vec3, takeoff: Int, jumps: Boolean, hops: Int): Jump? {
        val margin = land()
        if (margin.isNaN()) {
            return null
        }
        return Jump(runUp, takeoff, jumps, airTicks, margin, hops, Vec3(aimX, targetHeight, aimZ))
    }

    /** Flies [motion] on to the ground; the margin of its landing on the target, NaN for any other end. */
    private fun land(): Double {
        val floor = min(targetHeight, fromY) - MAX_BELOW
        airTicks = 1
        while (!motion.onGround && motion.y > floor && airTicks < MAX_AIR_TICKS) {
            if (catches && isInTarget(motion.x, motion.y, motion.z)) {
                return margin(motion.x, motion.z)
            }
            val back = !catches && overshoots()
            motion.tick(yawToTarget(), if (back) -1 else 1, 0, jump = false, sprint = abilities.canSprint && !back)
            path?.add(motion.position)
            airTicks++
        }

        if (catches || !motion.onGround || motion.landingFall > maxFall) {
            return Double.NaN
        }
        val touchdown = footing()
        // A landing on the lip of a block is followed by a few careful steps onto it, as into a hole between walls.
        repeat(SETTLE_TICKS) {
            motion.tick(yawToTarget(), 1, 0, jump = false, sprint = false, sneak = true)
        }
        val settled = footing()
        return if (touchdown.isNaN() || settled > touchdown) settled else touchdown
    }

    private fun footing() = when {
        !motion.onGround || abs(motion.y - targetHeight) > HEIGHT_TOLERANCE -> Double.NaN
        motion.standsOn(target.x, target.y, target.z) -> motion.footing(target.x, target.y, target.z)
        motion.standsOn(target.x, target.y - 1, target.z) -> motion.footing(target.x, target.y - 1, target.z)
        else -> Double.NaN
    }

    private fun overshoots(): Boolean {
        val toX = targetX - motion.x
        val toZ = targetZ - motion.z
        val remaining = Mth.length(toX, toZ)
        if (remaining < 1.0E-4) {
            return false
        }
        val speed = (motion.motionX * toX + motion.motionZ * toZ) / remaining
        return overshoots(remaining, speed, motion.y, motion.motionY)
    }

    /** Whether pushing on at [speed] carries a player past the target, [remaining] blocks away, before it is down. */
    private fun overshoots(remaining: Double, speed: Double, height: Double, motionY: Double): Boolean {
        var forward = speed
        var y = height
        var vertical = motionY
        var travel = 0.0
        repeat(MAX_AIR_TICKS) {
            forward += SPRINT_AIR_ACCELERATION
            travel += forward
            y += vertical
            if (travel > remaining + BRAKE_SLACK) {
                return true
            }
            if (vertical < 0.0 && y <= targetHeight) {
                return false
            }
            forward *= BASE_HORIZONTAL_AIR_DRAG
            vertical = (vertical - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
        }
        return false
    }

    /** Whether every column between the start and the target catches a fall the walker survives. */
    private fun isSafe(): Boolean {
        var travelled = HALF_BLOCK
        while (travelled < distance - HALF_BLOCK) {
            val x = Mth.floor(fromX + dirX * travelled)
            val z = Mth.floor(fromZ + dirZ * travelled)
            if (!catchesFall(x, Mth.floor(fromY), z)) {
                return false
            }
            travelled += SAFETY_STEP
        }
        return true
    }

    private fun catchesFall(x: Int, top: Int, z: Int): Boolean {
        for (y in top downTo maxOf(top - abilities.safeFall - 1, terrain.minY)) {
            val stand = terrain.standHeight(x, y, z)
            when {
                terrain.isWater(x, y, z) -> return true
                !stand.isNaN() -> return fromY - stand <= abilities.safeFall
                !terrain.isPassable(x, y, z) -> return true
            }
        }
        return false
    }

    private fun snapshot(): PlayerMotion =
        if (pool.isEmpty()) motion.copy() else pool.removeLast().also { it.copyFrom(motion) }

    private fun better(a: Jump?, b: Jump?) = if (a == null || b != null && b.margin > a.margin) b else a

    private fun Jump?.has(margin: Double) = this != null && this.margin >= margin

    private class Estimate(val takeoff: Int, val margin: Double, val state: PlayerMotion)

    private companion object {
        const val MAX_RUN_UP = 1.6
        const val RUN_UP_SPACING = 0.3
        const val RUN_UP_STEP = 0.1

        const val MAX_HOPS = 2
        const val MAX_HOP_RUN_UP = 8.0
        const val HOP_REACH = 6.2

        /** How far before the edge the last hop lands. */
        const val HOP_LEAD = 0.3
        const val MAX_EDGE = 1.5
        const val MAX_GROUND_TICKS = 16
        const val MAX_AIR_TICKS = 60
        const val MAX_BELOW = 4.0
        const val LANDING_PROBE = 1.0E-4
        const val HEIGHT_TOLERANCE = 1.0E-3
        const val SETTLE_TICKS = 3
        const val STOPPED_SPEED = 0.01

        /** How far past the middle of the target a landing may go before the air control holds back. */
        const val BRAKE_SLACK = 0.25
        const val HALF_BLOCK = 0.5
        const val SAFETY_STEP = 0.5
        const val QUARTER_TURN = 90.0
        const val RISKY_EXTRA_FALL = 4.5
        const val VALIDATED_TAKEOFFS = 4

        val SIDE_AIMS = doubleArrayOf(0.3, -0.3)

        /** How much better a margin must be for other keys than the ones tried first: forward, no strafe. */
        const val KEY_PREFERENCE = 0.02
        val STRAFES = intArrayOf(0, 1, -1)
        const val AIR_ACCELERATION = PlayerMotion.AIR_SPEED * PlayerMotion.INPUT_FRICTION
        const val SPRINT_AIR_ACCELERATION = PlayerMotion.SPRINT_AIR_SPEED * PlayerMotion.INPUT_FRICTION

        const val MIN_MARGIN = 0.15
        const val CAREFUL_MARGIN = 0.35

        /** A margin that makes a run-up from further back not worth it. */
        const val GOOD_MARGIN = 0.45
    }

}
