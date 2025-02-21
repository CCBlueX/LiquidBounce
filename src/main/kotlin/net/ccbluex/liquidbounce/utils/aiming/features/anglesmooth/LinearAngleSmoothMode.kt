/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class LinearAngleSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Linear") {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f,
        0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f,
        0.0f..180f)

    override fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val diff = currentRotation.rotationDeltaTo(targetRotation)

        val rotationDifference = diff.length()
        val (factorH, factorV) = horizontalTurnSpeed.random().toFloat() to
            verticalTurnSpeed.random().toFloat()

        val straightLineYaw = abs(diff.deltaYaw / rotationDifference) * (factorH * factorModifier)
        val straightLinePitch = abs(diff.deltaPitch / rotationDifference) * (factorV * factorModifier)

        return Rotation(
            currentRotation.yaw + diff.deltaYaw.coerceIn(-straightLineYaw, straightLineYaw),
            currentRotation.pitch + diff.deltaPitch.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int {
        val difference = currentRotation.angleTo(targetRotation)
        val turnSpeed = min(horizontalTurnSpeed.start, verticalTurnSpeed.start)

        if (difference <= 0.0 || turnSpeed <= 0.0) {
            return 0
        }

        return (difference / turnSpeed).roundToInt()
    }

}
