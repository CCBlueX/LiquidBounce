/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.utils.aiming.angleSmooth

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.utils.aiming.Attention
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

/**
 * A smoother is being used to limit the angle change between two rotations.
 */
abstract class AngleSmoothMode(name: String) : Choice(name) {
    abstract fun limitAngleChange(
        attention: Attention,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d? = null,
        entity: Entity? = null
    ): Rotation
    abstract fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int
}
