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

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Makes a [WalkMove.PARKOUR] step by simulating the player in the live world every tick: running on and jumping at
 * the tick that lands best when the jump can be made from where the player is, else walking back to the run-up of
 * the [WalkJump] while facing the target, and in the air pressing the keys that land the furthest inside the
 * target block.
 */
internal class WalkLeap {

    /**
     * Where the player looks, the middle of the target block.
     */
    var target: Vec3? = null
        private set

    var sprint = false
        private set

    /**
     * Hops of a momentum jump made so far; the next one follows right on landing.
     */
    private var hops = 0

    /**
     * How far behind the run-up the player stops, after no jump worked from it.
     */
    private var retreat = 0.0

    /**
     * Ticks the player has stood still on the ground.
     */
    private var stillTicks = 0

    /**
     * Whether the player runs at a jump found on the spot.
     */
    private var running = false

    /**
     * A run-up found on the spot after the planned one did not work.
     */
    private var refined: WalkJump? = null

    /**
     * Whether no jump worked even from as far back as the player goes, so the step should be given up.
     */
    var hopeless = false
        private set

    fun tick(event: MovementInputEvent, frameYaw: Float, step: WalkStep, jump: WalkJump, abilities: WalkAbilities) {
        val terrain = NavigationTerrain(LiveWorld(world))
        val state = PlayerMotion(terrain, abilities.motion)
        state.reset(player)
        val input = decide(state, RotationManager.movementYaw, step, jump, WalkJumps(terrain, abilities))
        event.sneak = input.sneak
        event.directionalInput = keys(input.forward, input.strafe, frameYaw)
        event.jump = input.jump
        sprint = input.sprint
    }

    /**
     * The keys to press this tick in [state], moving along [yaw].
     */
    fun decide(state: PlayerMotion, yaw: Float, step: WalkStep, planned: WalkJump, jumps: WalkJumps): LeapInput {
        target = step.feet
        if (!state.onGround) {
            stillTicks = 0
            running = false
            val keys = jumps.airKeys(state, yaw, step.to, step.feet.y)
            val forward = keys?.forward ?: 1
            return LeapInput(forward, keys?.strafe ?: 0, jump = false, sprint = forward > 0)
        }

        val jump = refined ?: planned
        val ready = isReady(state, behind(jump.runUp, step.feet, RUN_UP_BIAS + retreat))

        // Away from the run-up, only a jump with room to spare beats walking back to it; a run started from where
        // the player was ready goes on as long as it still lands.
        val now = jumps.fromHere(state, yaw, step.to, step.feet.y)
            ?.takeIf { ready || running || it.margin >= LIVE_MARGIN }
        running = now != null
        if (now != null) {
            target = now.aim
            return LeapInput(1, 0, jump = now.jumps && now.takeoff == 0, sprint = jumps.canSprint)
        }

        // A momentum jump hops from its run-up, each hop right after the last one landed.
        if (hops > 0 && hops < jump.hops || hops == 0 && jump.hops > 0 && ready) {
            hops++
            return LeapInput(1, 0, jump = true, sprint = jumps.canSprint)
        }

        hops = 0
        if (ready && jump.hops == 0) {
            moveRunUp(step, jump, jumps)
        }
        val runUp = (refined ?: planned).runUp
        return approach(state, yaw, behind(runUp, step.feet, RUN_UP_BIAS + retreat), step.feet)
    }

    /**
     * Whether the player in [state] stands still at [stop]; where the approach cannot get any closer, as at the edge
     * of the block, the player is as ready as it gets.
     */
    private fun isReady(state: PlayerMotion, stop: Vec3): Boolean {
        stillTicks = if (state.horizontalSpeed < READY_SPEED) stillTicks + 1 else 0
        return stillTicks > 0 &&
            (Vec3(state.x, stop.y, state.z).distanceTo(stop) < WalkJump.RUN_UP_TOLERANCE || stillTicks >= STILL_TICKS)
    }

    /**
     * After no jump worked from the run-up of [jump]: a run-up with more room, or one further back.
     */
    private fun moveRunUp(step: WalkStep, jump: WalkJump, jumps: WalkJumps) {
        if (refined == null) {
            // The planned run-up only just works and was not hit closely enough: one that works with more room.
            refined = jumps.refine(step.from, jump.runUp.y, step.to, step.feet.y)
                ?.takeIf { it.runUp.distanceTo(jump.runUp) > WalkJump.RUN_UP_TOLERANCE }
        }
        if (refined == null || jump === refined) {
            // Further back, the run has more room.
            hopeless = retreat >= MAX_RETREAT
            retreat = minOf(retreat + RETREAT_STEP, MAX_RETREAT)
        }
    }

    fun clear() {
        target = null
        sprint = false
        hops = 0
        stillTicks = 0
        running = false
        retreat = 0.0
        refined = null
        hopeless = false
    }

    /**
     * The point [distance] back from [runUp], away from [target]; a stop short of the run-up costs a jump less than
     * one past it.
     */
    private fun behind(runUp: Vec3, target: Vec3, distance: Double): Vec3 {
        val dx = target.x - runUp.x
        val dz = target.z - runUp.z
        val length = sqrt(dx * dx + dz * dz)
        if (length < MIN_LENGTH) {
            return runUp
        }
        return Vec3(runUp.x - dx / length * distance, runUp.y, runUp.z - dz / length * distance)
    }

    /**
     * Keys that bring the player to a stop at [runUp] without leaving the ground, looking at [look]: each choice is
     * held for a few ticks, walking or sneaking for small steps, and then let go, and the one that stops closest wins.
     */
    private fun approach(state: PlayerMotion, yaw: Float, runUp: Vec3, look: Vec3): LeapInput {
        val probe = state.copy()
        var best = LeapInput(0, 0, jump = false, sprint = false)
        var bestScore = Double.MAX_VALUE
        for (sneak in SNEAKS) {
            for (forward in -1..1) {
                for (strafe in -1..1) {
                    probe.copyFrom(state)
                    val input = LeapInput(forward, strafe, jump = false, sprint = false, sneak = sneak)
                    val score = stopsAt(probe, yaw, input, runUp, look)
                    if (score < bestScore - SCORE_EPSILON) {
                        bestScore = score
                        best = input
                    }
                }
            }
        }
        return best
    }

    /**
     * How far from [runUp], squared, [probe] stops when it holds [input] a few ticks; much more when it leaves the
     * ground on the way.
     */
    private fun stopsAt(probe: PlayerMotion, yaw: Float, input: LeapInput, runUp: Vec3, look: Vec3): Double {
        for (tick in 0 until HOLD_TICKS + SETTLE_TICKS) {
            val held = tick < HOLD_TICKS
            val tickYaw = if (tick == 0) yaw else yawTo(probe, look)
            probe.tick(
                tickYaw, if (held) input.forward else 0, if (held) input.strafe else 0, jump = false, sprint = false,
                sneak = held && input.sneak,
            )
            if (!probe.onGround) {
                // Leaving the ground later is the lesser evil.
                return FALL_PENALTY - tick
            }
        }

        val dx = probe.x - runUp.x
        val dz = probe.z - runUp.z
        return dx * dx + dz * dz
    }

    private fun yawTo(motion: PlayerMotion, look: Vec3): Float =
        (atan2(look.z - motion.z, look.x - motion.x) * Mth.RAD_TO_DEG - QUARTER_TURN).toFloat()

    private companion object {
        const val HOLD_TICKS = 2
        const val SETTLE_TICKS = 6
        const val FALL_PENALTY = 100.0
        const val SCORE_EPSILON = 1.0E-4
        const val QUARTER_TURN = 90.0

        /**
         * Fastest a player may move and still count as standing at a run-up, in blocks a tick.
         */
        const val READY_SPEED = 0.03
        const val STILL_TICKS = 5

        /**
         * Least landing margin a jump started elsewhere than at the run-up needs.
         */
        const val LIVE_MARGIN = 0.35

        val SNEAKS = booleanArrayOf(false, true)

        const val RUN_UP_BIAS = 0.05
        const val RETREAT_STEP = 0.1
        const val MAX_RETREAT = 0.5
        const val MIN_LENGTH = 1.0E-6
    }

}

/**
 * Keys for one tick of a jump: [forward] and [strafe] relative to the movement yaw.
 */
internal class LeapInput(
    val forward: Int,
    val strafe: Int,
    val jump: Boolean,
    val sprint: Boolean,
    val sneak: Boolean = false,
)

/**
 * Keys that move [forward] and [strafe] relative to [RotationManager.movementYaw]. Under silent movement correction
 * the keys are read relative to the camera at [frameYaw] and turned onto the rotation, see MixinKeyboardInput, so
 * they are chosen to come out right after the turn.
 */
internal fun keys(forward: Int, strafe: Int, frameYaw: Float): DirectionalInput {
    val rotation = RotationManager.currentRotation
    if (rotation == null || RotationManager.activeRotationTarget?.movementCorrection != MovementCorrection.SILENT) {
        return DirectionalInput(forward.toFloat(), strafe.toFloat())
    }

    val delta = (frameYaw - rotation.yaw) * Mth.DEG_TO_RAD
    val cos = Mth.cos(delta.toDouble())
    val sin = Mth.sin(delta.toDouble())
    for (z in -1..1) {
        for (x in -1..1) {
            val turnedX = (x * cos - z * sin).roundToInt()
            val turnedZ = (z * cos + x * sin).roundToInt()
            if (turnedX == strafe && turnedZ == forward) {
                return DirectionalInput(z.toFloat(), x.toFloat())
            }
        }
    }
    return DirectionalInput(forward.toFloat(), strafe.toFloat())
}
