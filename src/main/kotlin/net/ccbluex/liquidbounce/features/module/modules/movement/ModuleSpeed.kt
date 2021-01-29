/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.config.mode
import net.ccbluex.liquidbounce.event.EntityTickEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Mode
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.sequenceHandler
import net.ccbluex.liquidbounce.utils.extensions.moving
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

object ModuleSpeed : Module("Speed", Category.COMBAT) {

    val mode by mode("Mode", "YPort", arrayOf(
        ModeSpeedYPort(this)
    ))

}

private class ModeSpeedYPort(module: Module) : Mode("YPort", module) {

    val tickHandler = sequenceHandler<EntityTickEvent> {
        if (player.isOnGround && player.moving) {
            val angle = toRadians(player.yaw.toDouble())
            val x = -sin(angle) * 0.4
            val z = cos(angle) * 0.4

            player.setVelocity(x, 0.42, z)
            wait(1)
            player.setVelocity(x, -1.0, z)
        }

    }

}
