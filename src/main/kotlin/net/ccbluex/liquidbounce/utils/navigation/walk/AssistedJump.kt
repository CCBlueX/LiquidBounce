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

    /** The block jumped to, with the feet at [feet], and the jump onto it. */
    class Target(val pos: BlockPos, val feet: Double, val jump: Jump) {
        val aim = Vec3(pos.x + 0.5, feet, pos.z + 0.5)
    }

    var target: Target? = null
        private set

    /** Whether an assisted jump is in the air. */
    private var airborne = false
    private var ticksSinceLanding = BRAKE_TICKS

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

        val target = pick(targets, jumps, state, yaw, heading, maxAngle)
        this.target = target
        if (target == null) {
            return if (ticksSinceLanding < BRAKE_TICKS) brake(state, yaw, keys) else null
        }

        val jumpNow = target.jump.jumps && target.jump.takeoff == 0
        airborne = jumpNow
        return LeapInput(1, 0, jump = jumpNow, sprint = true)
    }

    fun release() {
        target = null
    }

    fun reset() {
        target = null
        airborne = false
        ticksSinceLanding = BRAKE_TICKS
    }

    /** The keys that land the furthest inside the block jumped to. */
    private fun steerInAir(state: PlayerMotion, yaw: Float, jumps: Jumps): LeapInput? {
        val target = target ?: return null
        val air = jumps.airKeys(state, yaw, target.pos, target.feet) ?: return null
        return LeapInput(air.forward, air.strafe, jump = false, sprint = air.forward > 0)
    }

    /** The block across a gap within [maxAngle] of [heading] that a jump from here lands on best. */
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
        val feet = BlockPos.containing(position)
        val height = targets.terrain.standHeight(feet.x, feet.y, feet.z).ifNaN { position.y }
        var best: Target? = null
        var bestScore = Double.MAX_VALUE
        targets.forEach(feet.x, feet.y, feet.z, height, MAX_DROP) { x, y, z, top ->
            val angle = angle(position, Vec3(x + 0.5, top, z + 0.5), heading)
            if (angle > maxAngle) {
                return@forEach true
            }

            val pos = BlockPos(x, y, z)
            val jump = jumps.fromHere(state, yaw, pos, top) ?: return@forEach true
            val score = angle / maxAngle - jump.margin
            if (score < bestScore) {
                best = Target(pos, top, jump)
                bestScore = score
            }
            true
        }
        return best
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

    private fun angle(from: Vec3, to: Vec3, heading: Float) = abs(Mth.wrapDegrees(to.subtract(from).yaw - heading))

    private companion object {
        const val MAX_DROP = 4

        const val BRAKE_TICKS = 10
        const val BRAKE_LOOKAHEAD = 3
        const val BRAKE_SPEED = 0.1
    }

}
