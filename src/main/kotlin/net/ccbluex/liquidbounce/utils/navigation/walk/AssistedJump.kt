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
import net.ccbluex.liquidbounce.utils.math.yaw
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * The jump across a gap a player running with some heading goes for, worked out by simulation every tick: the block
 * it lands best on, the tick to jump at and the keys in the air. The player only holds the keys towards it.
 */
internal class AssistedJump {

    /**
     * The block jumped to, with the feet at [feet], and the jump onto it. A jump that does not work from where the
     * player is has a [step], which [Leap] makes from its run-up. A landing that [boosts] bounces the player back up
     * higher than a jump, so it is the next take-off and the speed is kept onto it. One it [settles] on is landed on
     * sneaking, which stops a bounce.
     */
    class Target(
        val pos: BlockPos,
        val feet: Double,
        val jump: Jump,
        val step: Step? = null,
        val boosts: Boolean = false,
        val settles: Boolean = false,
    ) {
        /** Where to turn to: the jump was simulated running there, which can be beside the middle of the block. */
        val aim: Vec3
            get() = jump.aim

        fun isLanded(position: Vec3, onGround: Boolean) = onGround && abs(position.y - feet) < FEET_PROBE &&
            abs(position.x - pos.x - HALF_BLOCK) < HALF_BLOCK + PlayerMotion.HALF_WIDTH &&
            abs(position.z - pos.z - HALF_BLOCK) < HALF_BLOCK + PlayerMotion.HALF_WIDTH
    }

    var target: Target? = null
        private set

    /** The way to [target], tick for tick, as [trace] predicted it: from its run-up when it has one. */
    val path = ArrayList<Vec3>()

    /** The index in [path] of the last position on the ground before the jump, or -1 when it is not on the way. */
    var takeoff = -1
        private set

    private var leap = Leap()

    /** Whether an assisted jump is in the air. */
    private var airborne = false
    private var ticksSinceLanding = BRAKE_TICKS

    /**
     * The yaw a player at [position], moving by [motion], turns to for the jump, or NaN to hold the one it has: close
     * to the aim, where the way there turns around once it is passed, past it in the air, and down on the target until
     * the next tick picks the jump after it.
     */
    fun aimYaw(position: Vec3, motion: Vec3, onGround: Boolean): Float {
        val target = target ?: return Float.NaN
        val delta = target.aim.subtract(position)
        val passed = !onGround && delta.x * motion.x + delta.z * motion.z < 0.0
        val near = delta.horizontalDistanceSqr() < MIN_AIM_DISTANCE * MIN_AIM_DISTANCE
        return if (near || passed || target.isLanded(position, onGround)) Float.NaN else delta.yaw
    }

    /**
     * The keys for [state], a player moving with [yaw] who holds [keys] towards [heading], that make the jump it
     * goes for; null when there is nothing to help with.
     */
    @Suppress("LongParameterList")
    fun tick(
        state: PlayerMotion,
        yaw: Float,
        heading: Float,
        keys: LeapInput,
        maxAngle: Float,
        jumps: Jumps,
        targets: JumpTargets,
    ): LeapInput? {
        if (!state.onGround) {
            return steerInAir(state, yaw, jumps)
        }

        if (airborne) {
            airborne = false
            ticksSinceLanding = 0
        } else if (ticksSinceLanding < BRAKE_TICKS) {
            ticksSinceLanding++
        }

        val target = runUpKept(state, heading, maxAngle) ?: pick(targets, jumps, state, yaw, heading, maxAngle)
        if (target?.step != null && target !== this.target) {
            leap = Leap()
        }
        this.target = target
        if (target == null) {
            return if (ticksSinceLanding < BRAKE_TICKS) brake(state, yaw, keys) else null
        }

        val step = target.step
        if (step != null) {
            return leap.decide(state, yaw, step, target.jump, jumps).also { airborne = it.jump }
        }
        if (state.motionY > 0.0 && !target.jump.jumps) {
            // Off a bounce the player leaves the ground anyway, so this tick steers as in the air.
            airborne = true
            return steerInAir(state, yaw, jumps)
        }
        val jumpNow = target.jump.jumps && target.jump.takeoff == 0
        airborne = jumpNow
        return LeapInput(1, 0, jump = jumpNow, sprint = true)
    }

    /** Predicts the way to [target] for [state], a player moving with [yaw], into [path]. */
    fun trace(state: PlayerMotion, yaw: Float, jumps: Jumps) {
        val target = target
        if (target == null) {
            path.clear()
            takeoff = -1
            return
        }

        val runUp = target.step?.let {
            state.copy().apply { reset(target.jump.runUp, PlayerMotion.RESTING, onGround = true) }
        }
        val yawNow = if (runUp == null) yaw else null
        takeoff = jumps.trace(runUp ?: state, yawNow, target.pos, target.feet, target.jump, path)
    }

    fun release() {
        target = null
        path.clear()
    }

    fun reset() {
        release()
        airborne = false
        ticksSinceLanding = BRAKE_TICKS
    }

    /**
     * The keys that land the furthest inside the block jumped to, or with the most speed when it boosts; sneaking on
     * the tick it lands when it settles.
     */
    private fun steerInAir(state: PlayerMotion, yaw: Float, jumps: Jumps): LeapInput? {
        val target = target ?: return null
        val air = jumps.airKeys(state, yaw, target.pos, target.feet, keepSpeed = target.boosts) ?: return null
        val sprint = air.forward > 0
        val lands = target.settles && state.copy().apply { tick(yaw, air.forward, air.strafe, false, sprint) }.onGround
        return LeapInput(air.forward, air.strafe, jump = false, sprint = sprint && !lands, sneak = lands)
    }

    /** The target being run up to, while the player is still on the block it jumps from and heads for it. */
    private fun runUpKept(state: PlayerMotion, heading: Float, maxAngle: Float): Target? {
        val target = target ?: return null
        val step = target.step ?: return null
        val stays = BlockPos.containing(state.x, state.y + FEET_PROBE, state.z) == step.from
        return target.takeIf { stays && !leap.hopeless && angle(state.position, target.aim, heading) <= maxAngle }
    }

    /**
     * The block across a gap within [maxAngle] of [heading] that a jump from here lands on best. A block the player
     * clearly heads for more than that one is jumped to from a run-up when no jump from here works.
     */
    @Suppress("LongParameterList")
    private fun pick(
        targets: JumpTargets,
        jumps: Jumps,
        state: PlayerMotion,
        yaw: Float,
        heading: Float,
        maxAngle: Float,
    ): Target? {
        val position = state.position
        val feet = BlockPos.containing(position.x, position.y + FEET_PROBE, position.z)
        val standing = targets.terrain.standHeight(feet.x, feet.y, feet.z)
        val height = if (standing.isNaN()) position.y else standing
        val found = find(targets, jumps, state, yaw, heading, maxAngle, feet, height)
        return when {
            // Bouncing off slime leaves no run-up.
            state.motionY > 0.0 -> found.best ?: settle(state, yaw, jumps, feet.takeUnless { standing.isNaN() }, height)
            standing.isNaN() -> found.best
            else -> found.runUp(jumps, feet, height) ?: found.best
        }
    }

    /** The jumps from [feet], standing at [height], to the blocks within [maxAngle] of [heading]. */
    @Suppress("LongParameterList")
    private fun find(
        targets: JumpTargets,
        jumps: Jumps,
        state: PlayerMotion,
        yaw: Float,
        heading: Float,
        maxAngle: Float,
        feet: BlockPos,
        height: Double,
    ): Found {
        val terrain = targets.terrain
        val position = state.position
        val found = Found()
        targets.forEach(feet.x, feet.y, feet.z, height, MAX_DROP, MAX_BOUNCE_DROP, state.motionY) { x, y, z, top ->
            val angle = angle(position, Vec3(x + 0.5, top, z + 0.5), heading)
            if (angle > maxAngle) {
                return@forEach true
            }

            // Below a course is where a missed jump ends, so any block level with it or higher comes first. Slime
            // bounces back up.
            val bounces = terrain.isBouncy(x, y - 1, z)
            val drop = maxOf(0.0, height - top)
            val intent = angle / maxAngle + if (bounces) 0.0 else drop * DROP_PENALTY
            val pos = BlockPos(x, y, z)
            val jump = jumps.fromHere(state, yaw, pos, top)
            if (jump == null) {
                found.missed += Missed(pos, top, intent)
            } else if (intent - jump.margin < found.bestScore) {
                found.best = Target(pos, top, jump, boosts = bounces && drop >= BOOST_DROP)
                found.bestScore = intent - jump.margin
                found.bestIntent = intent
            }
            true
        }
        return found
    }

    /**
     * With nowhere to go off a bounce, coming down where it bounced off, or on [feet] at [height], without a jump that
     * would keep it bouncing.
     */
    private fun settle(state: PlayerMotion, yaw: Float, jumps: Jumps, feet: BlockPos?, height: Double): Target? {
        val from = target?.takeIf { it.isLanded(state.position, state.onGround) }
        val pos = from?.pos ?: feet ?: return null
        val top = from?.feet ?: height
        val keys = jumps.airKeys(state, yaw, pos, top) ?: return null
        val middle = Vec3(pos.x + HALF_BLOCK, top, pos.z + HALF_BLOCK)
        val jump = Jump(state.position, takeoff = 0, jumps = false, airTicks = 0, keys.margin, hops = 0, middle)
        return Target(pos, top, jump, settles = true)
    }

    /** After a landing, holds back when [keys] would run off a block too small to stop on later. */
    private fun brake(state: PlayerMotion, yaw: Float, keys: LeapInput): LeapInput? {
        val probe = state.copy()
        val runsOff = (0 until BRAKE_LOOKAHEAD).any {
            probe.tick(yaw, keys.forward, keys.strafe, jump = false, sprint = state.sprinting)
            !probe.onGround
        }
        if (!runsOff || state.horizontalSpeed <= BRAKE_SPEED) {
            return null
        }

        return LeapInput(-1, 0, jump = keys.jump, sprint = false)
    }


    private class Missed(val pos: BlockPos, val top: Double, val intent: Double)

    /** The [best] jump [find] came across, with its score and intent, and the blocks out of reach from here. */
    private class Found {
        var best: Target? = null
        var bestScore = Double.MAX_VALUE
        var bestIntent = Double.MAX_VALUE
        val missed = ArrayList<Missed>()

        /** A block clearly headed for more than [best] and out of reach from here, jumped to from a run-up. */
        fun runUp(jumps: Jumps, feet: BlockPos, height: Double): Target? {
            missed.sortBy(Missed::intent)
            for (candidate in missed.take(MAX_RUN_UPS)) {
                if (candidate.intent > bestIntent - INTENT_SLACK) {
                    break
                }
                val plan = jumps.plan(feet, height, candidate.pos, candidate.top) ?: continue
                val landing = Vec3(candidate.pos.x + 0.5, candidate.top, candidate.pos.z + 0.5)
                val step = Step(
                    feet, candidate.pos, Move.PARKOUR, landing, emptyList(), emptyList(), emptyList(), cost = 0.0,
                    jump = plan,
                )
                return Target(candidate.pos, candidate.top, plan, step)
            }
            return null
        }
    }

    private companion object {
        const val MAX_DROP = 4
        const val MAX_BOUNCE_DROP = 12

        /** How far a fall onto slime has to be for the bounce to get higher than a jump. */
        const val BOOST_DROP = 1.0
        const val DROP_PENALTY = 0.5

        /** How much more a block out of reach from here has to be headed for to be run up to instead. */
        const val INTENT_SLACK = 0.5
        const val MAX_RUN_UPS = 3
        const val FEET_PROBE = 1.0E-3
        const val HALF_BLOCK = 0.5
        const val MIN_AIM_DISTANCE = 0.75

        const val BRAKE_TICKS = 10
        const val BRAKE_LOOKAHEAD = 3
        const val BRAKE_SPEED = 0.1
    }

}

/** How far the way from [from] to [to] turns away from [heading], in degrees. */
private fun angle(from: Vec3, to: Vec3, heading: Float) = abs(Mth.wrapDegrees(to.subtract(from).yaw - heading))
