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
package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_12_2
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.maxCosToBox
import net.ccbluex.liquidbounce.utils.math.toDegrees
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import kotlin.math.acos

fun LocalPlayer.setRotation(rotation: Rotation) {
    rotation.normalize().let { normalizedRotation ->
        xRotO = xRot
        yRotO = yRot
        yBob = yRot
        yBobO = yRot

        yRot = normalizedRotation.yaw
        xRot = normalizedRotation.pitch
    }
}

fun LocalPlayer.withFixedYaw(rotation: Rotation) = rotation.yaw + angleDifference(yRot, rotation.yaw)

object RotationUtil {

    private const val MOUSE_TURN_SCALE_FLOAT = 0.15f
    private const val MOUSE_TURN_SCALE_DOUBLE = 0.15

    val gcd: Double
        get() {
            val sensitivityFactor = mouseSensitivityFactor()

            return if (isOlderThanOrEqual1_12_2) {
                (sensitivityFactor * MOUSE_TURN_SCALE_DOUBLE).toFloat().toDouble()
            } else {
                (sensitivityFactor.toFloat() * MOUSE_TURN_SCALE_FLOAT).toDouble()
            }
        }

    /**
     * Calculates the sensitivity part from the vanilla mouse input path.
     *
     * [1.12.2 reference](https://github.com/WangTingZheng/mcp940/blob/d0c030a4139ce7cf3f284b180f0d9ea87bdf8141/src/minecraft/net/minecraft/client/renderer/EntityRenderer.java#L1268-L1299)
     *
     * @see net.minecraft.client.MouseHandler.turnPlayer
     */
    private fun mouseSensitivityFactor(): Double {
        val sensitivity = mc.options.sensitivity().get()

        return if (isOlderThanOrEqual1_12_2) {
            val f = sensitivity.toFloat() * 0.6f + 0.2f
            (f * f * f * 8.0f).toDouble()
        } else {
            val f = sensitivity * 0.6f + 0.2f
            f * f * f * 8.0
        }
    }

    /**
     * Converts the values passed from `MouseHandler.turnPlayer` to the yaw/pitch delta applied by vanilla.
     *
     * [1.12.2 reference](https://github.com/WangTingZheng/mcp940/blob/d0c030a4139ce7cf3f284b180f0d9ea87bdf8141/src/minecraft/net/minecraft/entity/Entity.java#L479-L497)
     *
     * @see net.minecraft.world.entity.Entity.turn
     */
    fun mouseTurnDelta(cursorDeltaX: Double, cursorDeltaY: Double): RotationDelta {
        val deltaPitch: Float
        val deltaYaw: Float

        if (isOlderThanOrEqual1_12_2) {
            deltaPitch = (cursorDeltaY * MOUSE_TURN_SCALE_DOUBLE).toFloat()
            deltaYaw = (cursorDeltaX * MOUSE_TURN_SCALE_DOUBLE).toFloat()
        } else {
            deltaPitch = cursorDeltaY.toFloat() * MOUSE_TURN_SCALE_FLOAT
            deltaYaw = cursorDeltaX.toFloat() * MOUSE_TURN_SCALE_FLOAT
        }

        return RotationDelta(deltaYaw, deltaPitch)
    }

    fun applyMouseTurnDelta(rotation: Rotation, cursorDeltaX: Double, cursorDeltaY: Double): Rotation {
        val delta = mouseTurnDelta(cursorDeltaX, cursorDeltaY)

        return Rotation(
            yaw = rotation.yaw + delta.deltaYaw,
            pitch = (rotation.pitch + delta.deltaPitch).coerceIn(-90f, 90f)
        )
    }

    /**
     * How well the cross-hair points at [entity], as the cosine of the smallest angle between the
     * view direction and a direction towards the entity: 1.0 while the view ray passes through its
     * bounding box. Monotonic in that angle, so callers compare against `cos(limit)` or sort
     * descending instead of converting back to degrees.
     */
    fun crosshairCosToEntity(entity: Entity): Double {
        val player = mc.player ?: return 1.0

        return maxCosToBox(player.eyePosition, player.rotation.directionVector, entity.boundingBox)
    }

    /**
     * Angle in degrees between the cross-hair and the closest direction towards [entity], i.e. the
     * arccosine of [crosshairCosToEntity].
     *
     * The cosine comes from a slab test against the bounding box, falling back to the minimum over
     * its silhouette edges, where each edge's stationary point solves a linear equation. Aiming at
     * the closest point of the box rather than at its centre is what keeps the value usable up
     * close, where a box can span most of the view.
     *
     * Prefer [crosshairCosToEntity] for sorting and comparing: it orders identically while skipping
     * the arccosine.
     */
    fun crosshairAngleToEntity(entity: Entity): Float =
        acos(crosshairCosToEntity(entity).coerceIn(-1.0, 1.0)).toDegrees().toFloat()

    /**
     * Calculate difference between two angle points
     */
    fun angleDifference(a: Float, b: Float) = Mth.wrapDegrees(a - b)
}
