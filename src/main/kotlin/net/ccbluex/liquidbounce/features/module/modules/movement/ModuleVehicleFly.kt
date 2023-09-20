/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe

/**
 * Vehicle Fly module
 *
 * Fly with your vehicle.
 */
object ModuleVehicleFly : Module("VehicleFly", Category.MOVEMENT) {

    val speedVertical by float("Vertical", 0.32f, 0.1f..0.4f)
    val speedHorizontal by float("Horizontal", 0.48f, 0.1f..0.4f)

    val repeatable = repeatable {
        val vehicle = player.vehicle ?: return@repeatable

        vehicle.velocity.y = when {
            mc.options.jumpKey.isPressed -> speedVertical.toDouble()
            else -> 0.0
        }
        vehicle.velocity.strafe(yaw = player.directionYaw, speed = if (player.moving) speedHorizontal.toDouble() else 0.0)
    }

}
