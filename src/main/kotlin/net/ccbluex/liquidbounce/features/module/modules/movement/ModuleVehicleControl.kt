/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.util.math.Vec3d

/**
 * Vehicle control module
 *
 * Move with your vehicle however you want.
 */
@IncludeModule
object ModuleVehicleControl : Module("VehicleControl", Category.MOVEMENT) {

    init {
        enableLock()
    }

    private val speedVertical by float("Vertical", 0.32f, 0.1f..1f)
    private val glideVertical by float("GlideVertical", -0.2f, -0.3f..0.3f)

    private val speedHorizontal by float("Horizontal", 0.48f, 0.1f..2f)

    val repeatable = repeatable {
        val vehicle = player.vehicle ?: return@repeatable

        val horizontalSpeed = if (player.moving) speedHorizontal.toDouble() else 0.0
        val verticalSpeed = when {
            mc.options.jumpKey.isPressed -> speedVertical.toDouble()
            else -> glideVertical.toDouble()
        }

        // Vehicle control velocity
        val velocity = Vec3d(
            vehicle.velocity.x,
            verticalSpeed,
            vehicle.velocity.z
        ).strafe(yaw = player.directionYaw, speed = horizontalSpeed)

        vehicle.velocity = velocity
    }

}
