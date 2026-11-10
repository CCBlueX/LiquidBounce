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
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

/** Makes one parkour step by simulating the player every tick: jumps where it works, else walks back to the run-up. */
internal class WalkLeap {

    var target: Vec3? = null
        private set

    var sprint = false
        private set

    var hopeless = false
        private set

    private var hopsMade = 0
    private var stillTicks = 0

    /** How far behind the run-up the player stops, after no jump worked from it. */
    private var retreat = 0.0

    /** Whether the player runs at a jump found on the spot. */
    private var running = false
    private var refined: WalkJump? = null

    fun tick(event: MovementInputEvent, frameYaw: Float, step: WalkStep, jump: WalkJump, abilities: WalkAbilities) {
        val terrain = NavigationTerrain.live()
        val state = PlayerMotion(terrain, abilities.motion)
        state.reset(player)
        val input = decide(state, RotationManager.movementYaw, step, jump, WalkJumps(terrain, abilities))
        event.directionalInput = keys(input.forward, input.strafe, frameYaw)
        event.jump = input.jump
        event.sneak = input.sneak
        sprint = input.sprint
    }

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
        val ready = isReady(state, stop(jump, step))

        // Away from the run-up, only a jump with room to spare beats walking back to it; a run once started goes on.
        val now = jumps.fromHere(state, yaw, step.to, step.feet.y)
            ?.takeIf { ready || running || it.margin >= LIVE_MARGIN }
        running = now != null
        if (now != null) {
            target = now.aim
            return LeapInput(1, 0, jump = now.jumps && now.takeoff == 0, sprint = jumps.canSprint)
        }

        // A momentum jump hops from its run-up, each hop right after the last one landed.
        if (hopsMade < jump.hops && (hopsMade > 0 || ready)) {
            hopsMade++
            return LeapInput(1, 0, jump = true, sprint = jumps.canSprint)
        }

        hopsMade = 0
        if (ready && jump.hops == 0) {
            moveRunUp(step, jump, jumps)
        }
        return approach(state, yaw, stop(refined ?: planned, step), step.feet)
    }

    /** Standing still for [STILL_TICKS] counts where the approach cannot get closer, as at the edge of a block. */
    private fun isReady(state: PlayerMotion, stop: Vec3): Boolean {
        stillTicks = if (state.horizontalSpeed < READY_SPEED) stillTicks + 1 else 0
        val atStop = stop.horizontalDistanceToSqr(state.x, state.z) < Mth.square(WalkJump.RUN_UP_TOLERANCE)
        return stillTicks > 0 && (atStop || stillTicks >= STILL_TICKS)
    }

    /** After no jump worked from the run-up of [jump]: once a run-up with more room, then a stop further back. */
    private fun moveRunUp(step: WalkStep, jump: WalkJump, jumps: WalkJumps) {
        if (refined == null) {
            refined = jumps.refine(step.from, jump.runUp.y, step.to, step.feet.y)
                ?.takeIf { it.runUp.distanceToSqr(jump.runUp) > Mth.square(WalkJump.RUN_UP_TOLERANCE) }
            if (refined != null) {
                return
            }
        }

        hopeless = retreat >= MAX_RETREAT
        retreat = minOf(retreat + RETREAT_STEP, MAX_RETREAT)
    }

    /** Where to stand before the run: behind the run-up, as a stop short of it costs a jump less than one past it. */
    private fun stop(jump: WalkJump, step: WalkStep): Vec3 {
        val runUp = jump.runUp
        val distance = RUN_UP_BIAS + retreat
        val dx = step.feet.x - runUp.x
        val dz = step.feet.z - runUp.z
        val length = Mth.length(dx, dz)
        if (length < MIN_LENGTH) {
            return runUp
        }
        return Vec3(runUp.x - dx / length * distance, runUp.y, runUp.z - dz / length * distance)
    }

    /** Keys that bring the player to rest at [stop] on the ground: the best of every choice held for a few ticks. */
    private fun approach(state: PlayerMotion, yaw: Float, stop: Vec3, look: Vec3): LeapInput {
        val probe = state.copy()
        var best = LeapInput(0, 0, jump = false, sprint = false)
        var bestScore = Double.MAX_VALUE
        for (sneak in SNEAKS) {
            for (forward in -1..1) {
                for (strafe in -1..1) {
                    probe.copyFrom(state)
                    val input = LeapInput(forward, strafe, jump = false, sprint = false, sneak = sneak)
                    val score = restDistanceSqr(probe, yaw, input, stop, look)
                    if (score < bestScore - SCORE_EPSILON) {
                        bestScore = score
                        best = input
                    }
                }
            }
        }
        return best
    }

    private fun restDistanceSqr(probe: PlayerMotion, yaw: Float, input: LeapInput, stop: Vec3, look: Vec3): Double {
        for (tick in 0 until HOLD_TICKS + SETTLE_TICKS) {
            val held = tick < HOLD_TICKS
            val tickYaw = if (tick == 0) yaw else look.subtract(probe.x, probe.y, probe.z).yaw
            probe.tick(
                tickYaw, if (held) input.forward else 0, if (held) input.strafe else 0, jump = false, sprint = false,
                sneak = held && input.sneak,
            )
            if (!probe.onGround) {
                // Leaving the ground later is the lesser evil.
                return FALL_PENALTY - tick
            }
        }
        return stop.horizontalDistanceToSqr(probe.x, probe.z)
    }

    private companion object {
        const val HOLD_TICKS = 2
        const val SETTLE_TICKS = 6
        const val FALL_PENALTY = 100.0
        const val SCORE_EPSILON = 1.0E-4

        const val READY_SPEED = 0.03
        const val STILL_TICKS = 5
        const val LIVE_MARGIN = 0.35

        val SNEAKS = booleanArrayOf(false, true)

        const val RUN_UP_BIAS = 0.05
        const val RETREAT_STEP = 0.1
        const val MAX_RETREAT = 0.5
        const val MIN_LENGTH = 1.0E-6
    }

}

internal class LeapInput(
    val forward: Int,
    val strafe: Int,
    val jump: Boolean,
    val sprint: Boolean,
    val sneak: Boolean = false,
)

/** Keys that move [forward] and [strafe] relative to [RotationManager.movementYaw]. */
internal fun keys(forward: Int, strafe: Int, frameYaw: Float): DirectionalInput {
    val rotation = RotationManager.currentRotation
    if (rotation == null || RotationManager.activeRotationTarget?.movementCorrection != MovementCorrection.SILENT) {
        return DirectionalInput(forward.toFloat(), strafe.toFloat())
    }

    // Silent correction reads the keys relative to the camera at frameYaw and turns them onto the rotation.
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
