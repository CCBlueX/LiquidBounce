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
package net.ccbluex.liquidbounce.utils.aiming.features

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Corrects movement when aiming away from client-side view direction.
 */
@AddonApi
enum class MovementCorrection(override val tag: String) : Tagged {

    /**
     * No movement correction is applied. This feels the best, as it does not
     * change the movement of the player and also not affects Sprinting.
     * However, this can be detected by anti-cheats.
     */
    OFF("Off"),

    /**
     * Corrects movement by changing the yaw when updating the movement.
     */
    STRICT("Strict"),

    /**
     * Correct movement by changing the yaw when updating the movement,
     * but also tweaks the keyboard input to not aggressively change the
     * players walk direction.
     */
    SILENT("Silent"),

    /**
     * Corrects movement by changing the actual look direction of the player.
     */
    CHANGE_LOOK("ChangeLook")

}

/**
 * The keys [SILENT] presses: the player's keys turned by the angle between where they look and the rotation. A turned
 * direction only replaces the last one once it fits clearly better, so the keys do not flip every tick while the angle
 * hovers around a boundary.
 */
object SilentMovementKeys {

    private const val HYSTERESIS = 10f

    private var lastInput: DirectionalInput? = null
    private var lastTurned: DirectionalInput? = null

    /** The keys for this tick, where [deltaYaw] is the player's yaw minus the rotation's. */
    fun turn(input: DirectionalInput, deltaYaw: Float): DirectionalInput {
        val turned = peek(input, deltaYaw)
        lastInput = input
        lastTurned = turned
        return turned
    }

    /** What [turn] would give, without remembering it. */
    fun peek(input: DirectionalInput, deltaYaw: Float): DirectionalInput {
        val z = impulse(input.forwards, input.backwards)
        val x = impulse(input.left, input.right)
        if (x == 0f && z == 0f) {
            return DirectionalInput.NONE
        }

        val radians = deltaYaw * Mth.DEG_TO_RAD
        val turnedX = x * cos(radians) - z * sin(radians)
        val turnedZ = z * cos(radians) + x * sin(radians)
        val fresh = DirectionalInput(turnedZ.roundToInt().toFloat(), turnedX.roundToInt().toFloat())
        val last = lastTurned
        if (input != lastInput || last == null || last == fresh) {
            return fresh
        }

        val angle = atan2(turnedX, turnedZ) * Mth.RAD_TO_DEG
        return if (offBy(angle, last) < offBy(angle, fresh) + HYSTERESIS) last else fresh
    }

    private fun impulse(positive: Boolean, negative: Boolean) = (if (positive) 1f else 0f) - (if (negative) 1f else 0f)

    private fun offBy(angle: Float, keys: DirectionalInput): Float {
        val keysAngle = atan2(impulse(keys.left, keys.right), impulse(keys.forwards, keys.backwards)) * Mth.RAD_TO_DEG
        return abs(Mth.wrapDegrees(angle - keysAngle))
    }

}
