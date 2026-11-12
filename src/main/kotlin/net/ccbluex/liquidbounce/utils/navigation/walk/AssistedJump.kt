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

import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.utils.entity.PlayerMotion
import net.ccbluex.liquidbounce.utils.math.ifNaN
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
     * player is has a [step], which [Leap] makes from its run-up. A landing that [bounces] off slime [takesOff] again
     * when the bounce gets higher than a jump or a jump on it reaches a block further on, so the speed is kept onto
     * it. One it [settles] on is landed on sneaking, which stops a bounce.
     */
    class Target(
        val pos: BlockPos,
        val feet: Double,
        val jump: Jump,
        val step: Step? = null,
        val bounces: Boolean = false,
        val takesOff: Boolean = false,
        val settles: Boolean = false,
    ) {
        /** Where to turn to: the jump was simulated running there, which can be beside the middle of the block. */
        val aim: Vec3
            get() = jump.aim

        fun isLanded(position: Vec3, onGround: Boolean) = onGround && abs(position.y - feet) < FEET_PROBE &&
            isOver(position)

        private fun isOver(position: Vec3) =
            abs(position.x - pos.x - HALF_BLOCK) < HALF_BLOCK + PlayerMotion.HALF_WIDTH &&
                abs(position.z - pos.z - HALF_BLOCK) < HALF_BLOCK + PlayerMotion.HALF_WIDTH

        /**
         * The yaw a player at [position], moving by [motion], turns to for the jump towards [look], or NaN to hold the
         * one it has: close to it, where the way there turns around once it is passed, past it in the air, and down on
         * the block until the next tick picks the jump after it. Over a landing that [takesOff], it keeps facing on,
         * as the jump off the bounce goes the way it faces.
         */
        fun aimYaw(position: Vec3, motion: Vec3, onGround: Boolean, look: Vec3 = aim): Float {
            val delta = look.subtract(position)
            val passed = !onGround && delta.x * motion.x + delta.z * motion.z < 0.0
            val near = delta.horizontalDistanceSqr() < MIN_AIM_DISTANCE * MIN_AIM_DISTANCE
            val bouncesOn = takesOff && isOver(position)
            return if (near || passed || bouncesOn || isLanded(position, onGround)) Float.NaN else delta.yaw
        }

        /**
         * The keys for [state], a player in the air moving with [yaw], that land the furthest inside the block, or
         * with the most speed when the landing takes off again; sneaking on the tick it lands when it settles.
         */
        fun steer(state: PlayerMotion, yaw: Float, jumps: Jumps): LeapInput? {
            val air = jumps.airKeys(state, yaw, pos, feet, keepSpeed = takesOff) ?: return null
            val sprint = air.forward > 0
            val lands = settles && state.copy().apply { tick(yaw, air.forward, air.strafe, false, sprint) }.onGround
            return LeapInput(air.forward, air.strafe, jump = false, sprint = sprint && !lands, sneak = lands)
        }
    }

    /**
     * A jump of the way [trace] predicts: onto [target], the positions tick for tick in [path], and the index in it of
     * the last one on the ground before the jump, or -1 when it starts in the air or off a bounce.
     */
    class Arc(val target: Target) {
        val path = ArrayList<Vec3>()
        var takeoff = -1
            private set

        /**
         * Moves [state], moving with [yaw] or towards the aim when null, the way the assist takes it onto [target]:
         * through the take-off of its jump, then steered and turned as in the air until it comes down. The yaw it
         * comes down with.
         */
        fun follow(state: PlayerMotion, yaw: Float?, jumps: Jumps): Float {
            path += state.position
            // Off a bounce the player leaves the ground anyway and steers as in the air.
            val bounces = state.motionY > 0.0 && !target.jump.jumps
            if (state.onGround && !bounces) {
                if (!jumps.takeOff(state, yaw, target.pos, target.feet, target.jump, path)) {
                    return Float.NaN
                }
                takeoff = path.lastIndex - 1
            }

            var turn = yaw ?: target.aim.subtract(state.position).yaw
            var ticks = 0
            do {
                turn = target.aimYaw(state.position, state.deltaMovement, state.onGround).ifNaN { turn }
                val keys = target.steer(state, turn, jumps) ?: break
                state.tick(turn, keys.forward, keys.strafe, keys.jump, keys.sprint, keys.sneak)
                path += state.position
            } while (!state.onGround && ++ticks < MAX_AIR_TICKS)
            return turn
        }
    }

    var target: Target? by clientState { null }
        private set

    /**
     * The way to [target] as [trace] predicted it, jump by jump: from its run-up when it has one, and on to the next
     * block when the landing takes off again.
     */
    val arcs by clientState { ArrayList<Arc>() }

    private var leap by clientState { Leap() }

    /** The run-up targets planned while standing on [plannedFrom], by the block jumped to; null where none works. */
    private val planned by clientState { HashMap<BlockPos, Target?>() }
    private var plannedFrom: BlockPos? by clientState { null }

    /** Whether an assisted jump is in the air. */
    private var airborne by clientState { false }
    private var ticksSinceLanding by clientState { BRAKE_TICKS }

    /**
     * [Target.aimYaw] of [target], or NaN without one: towards where [Leap] looks on the way to a run-up, which it
     * simulated walking there with.
     */
    fun aimYaw(position: Vec3, motion: Vec3, onGround: Boolean): Float {
        val target = target ?: return Float.NaN
        val look = leap.target.takeIf { target.step != null } ?: target.aim
        return target.aimYaw(position, motion, onGround, look)
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
            return target?.steer(state, yaw, jumps)
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
            return untargeted(state, yaw, keys)
        }

        val step = target.step
        if (step != null) {
            return leap.decide(state, yaw, step, target.jump, jumps).also { airborne = it.jump }
        }
        if (state.motionY > 0.0 && !target.jump.jumps) {
            // Off a bounce the player leaves the ground anyway, so this tick steers as in the air.
            airborne = true
            return target.steer(state, yaw, jumps)
        }
        val jumpNow = target.jump.jumps && target.jump.takeoff == 0
        airborne = jumpNow
        return LeapInput(1, 0, jump = jumpNow, sprint = true)
    }

    /**
     * Predicts the way to [target] into [arcs] for [state], a player moving with [yaw] who heads for [heading], and
     * from a landing that takes off again on to the block within [maxAngle] of it that the bounce goes for.
     */
    @Suppress("LongParameterList")
    fun trace(
        state: PlayerMotion,
        yaw: Float,
        heading: Float,
        maxAngle: Float,
        jumps: Jumps,
        targets: JumpTargets,
    ) {
        arcs.clear()
        val target = target ?: return
        val probe = state.copy()
        val runUp = target.step != null
        if (runUp) {
            probe.reset(target.jump.runUp, PlayerMotion.RESTING, onGround = true)
        }
        val first = Arc(target)
        arcs += first
        val turn = first.follow(probe, yaw.takeUnless { runUp }, jumps)
        if (!target.takesOff || probe.motionY <= 0.0 || !target.isLanded(probe.position, probe.onGround)) {
            return
        }

        val next = pick(targets, jumps, probe, turn, heading, maxAngle) ?: return
        arcs += Arc(next).also { it.follow(probe, turn, jumps) }
    }

    fun release() {
        target = null
        arcs.clear()
    }

    fun reset() {
        release()
        airborne = false
        ticksSinceLanding = BRAKE_TICKS
        planned.clear()
        plannedFrom = null
    }

    /** The target being run up to, while the player is still on the block it jumps from and heads for it. */
    private fun runUpKept(state: PlayerMotion, heading: Float, maxAngle: Float): Target? {
        val target = target ?: return null
        val step = target.step ?: return null
        if (leap.hopeless) {
            planned[step.to] = null
            return null
        }
        val stays = state.feetBlock == step.from
        return target.takeIf { stays && angle(state.position, target.aim, heading) <= maxAngle }
    }

    /**
     * The block across a gap within [maxAngle] of [heading] that a jump from here lands on best. A block the player
     * clearly heads for more than that one is jumped to from a run-up when no jump from here works. Slime that a jump
     * on its bounce goes on from is jumped to as late as it still lands, to come down as far on it as it can.
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
        val feet = state.feetBlock
        val standing = targets.terrain.standHeight(feet.x, feet.y, feet.z)
        val height = if (standing.isNaN()) position.y else standing
        val found = find(targets, jumps, state, yaw, heading, maxAngle, feet, height)
        fun best(): Target? {
            val best = found.best ?: return null
            if (!best.bounces || best.takesOff) {
                return best
            }
            val jump = jumps.lastFromHere(state, yaw, best.pos, best.feet) ?: return best
            val chained = Target(best.pos, best.feet, jump, bounces = true, takesOff = true)
            val landing = state.copy()
            val turn = Arc(chained).follow(landing, yaw, jumps)
            val leadsOn = chained.isLanded(landing.position, landing.onGround) && landing.motionY > 0.0 &&
                find(targets, jumps, landing, turn, heading, maxAngle, best.pos, best.feet).best != null
            return if (leadsOn) chained else best
        }
        return when {
            // Bouncing off slime leaves no run-up.
            state.motionY > 0.0 -> best() ?: settle(state, yaw, jumps, feet.takeUnless { standing.isNaN() }, height)
            standing.isNaN() -> best()
            else -> found.runUp { runUpTo(it, jumps, feet, height) } ?: best()
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
        targets.forEach(
            feet.x, feet.y, feet.z, height, MAX_DROP, MAX_BOUNCE_DROP, state.motionY, around = true,
        ) { x, y, z, top ->
            val angle = angle(position, Vec3(x + 0.5, top, z + 0.5), heading)
            if (angle > maxAngle) {
                return@forEach true
            }

            // Below a course is where a missed jump ends, so any block level with it or higher comes first. Slime and
            // beds bounce back up.
            val restitution = terrain.restitution(x, top, z)
            val bounces = restitution > 0.0
            val drop = maxOf(0.0, height - top)
            val intent = angle / maxAngle + if (bounces) 0.0 else drop * DROP_PENALTY
            val pos = BlockPos(x, y, z)
            val jump = jumps.fromHere(state, yaw, pos, top)
            if (jump == null) {
                found.missed += Missed(pos, top, intent)
            } else if (intent - jump.margin < found.bestScore) {
                // The bounce gets as high as the fall times the restitution squared.
                val takesOff = bounces && drop * restitution * restitution >= BOOST_DROP
                found.best = Target(pos, top, jump, bounces = bounces, takesOff = takesOff)
                found.bestScore = intent - jump.margin
                found.bestIntent = intent
            }
            true
        }
        return found
    }

    /**
     * The jump to [missed] from a run-up on [feet], standing at [height]: planned once while the player stays on the
     * block, as a jump around something takes a while to find.
     */
    private fun runUpTo(missed: Missed, jumps: Jumps, feet: BlockPos, height: Double): Target? {
        if (feet != plannedFrom) {
            planned.clear()
            plannedFrom = feet
        }
        if (missed.pos in planned) {
            return planned[missed.pos]
        }

        val plan = jumps.plan(feet, height, missed.pos, missed.top, around = true)
        val target = plan?.let {
            val landing = Vec3(missed.pos.x + HALF_BLOCK, missed.top, missed.pos.z + HALF_BLOCK)
            val step = Step(
                feet, missed.pos, Move.PARKOUR, landing, emptyList(), emptyList(), emptyList(), cost = 0.0, jump = it,
            )
            Target(missed.pos, missed.top, it, step)
        }
        planned[missed.pos] = target
        return target
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

    /**
     * The keys without a target, or null to leave [keys] to the jump at the edge. After a landing, holds back when
     * they would run off a block too small to stop on later. Where they only step down onto ground a little lower,
     * like off a bed onto the floor beside it, goes on without a jump, as the one at the edge would take off there,
     * short of the edge after.
     */
    private fun untargeted(state: PlayerMotion, yaw: Float, keys: LeapInput): LeapInput? {
        val probe = state.copy()
        var leaves = -1
        var lands = false
        for (tick in 0 until STEP_DOWN_TICKS) {
            probe.tick(yaw, keys.forward, keys.strafe, jump = false, sprint = state.sprinting)
            if (!probe.onGround && leaves < 0) {
                leaves = tick
            } else if (probe.onGround && leaves >= 0) {
                lands = true
                break
            }
        }

        val brakes = ticksSinceLanding < BRAKE_TICKS && leaves in 0 until BRAKE_LOOKAHEAD &&
            state.horizontalSpeed > BRAKE_SPEED
        // Off a bed or slime, the probe bounces and comes down where it was, which is no step.
        val drop = state.y - probe.y
        return when {
            brakes -> LeapInput(-1, 0, jump = keys.jump, sprint = false)
            leaves == 0 && lands && state.motionY <= 0.0 && drop > 0.0 && drop <= MAX_STEP_DOWN ->
                LeapInput(keys.forward, keys.strafe, jump = false, sprint = keys.sprint)
            else -> null
        }
    }


    private class Missed(val pos: BlockPos, val top: Double, val intent: Double)

    /** The [best] jump [find] came across, with its score and intent, and the blocks out of reach from here. */
    private class Found {
        var best: Target? = null
        var bestScore = Double.MAX_VALUE
        var bestIntent = Double.MAX_VALUE
        val missed = ArrayList<Missed>()

        /** A block clearly headed for more than [best] and out of reach from here, jumped to from its [runUp]. */
        fun runUp(runUp: (Missed) -> Target?): Target? {
            missed.sortBy(Missed::intent)
            for (candidate in missed.take(MAX_RUN_UPS)) {
                if (candidate.intent > bestIntent - INTENT_SLACK) {
                    break
                }
                runUp(candidate)?.let { return it }
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
        const val MAX_AIR_TICKS = 60

        const val BRAKE_TICKS = 10
        const val BRAKE_LOOKAHEAD = 3
        const val BRAKE_SPEED = 0.1

        /** How far and for how many ticks a step down falls, which is walked rather than jumped off. */
        const val MAX_STEP_DOWN = 0.6
        const val STEP_DOWN_TICKS = 5
    }

}

/** How far the way from [from] to [to] turns away from [heading], in degrees. */
private fun angle(from: Vec3, to: Vec3, heading: Float) = abs(Mth.wrapDegrees(to.subtract(from).yaw - heading))
