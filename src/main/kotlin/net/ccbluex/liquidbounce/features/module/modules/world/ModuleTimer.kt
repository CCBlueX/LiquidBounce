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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.WorldDisconnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.timer
import net.ccbluex.liquidbounce.utils.entity.moving

/**
 * Changes the speed of the entire game.
 */
object ModuleTimer : Module("Timer", Category.WORLD) {

    val speed by float("Speed", 2f, 0.1f..10f)
    val onMove by boolean("OnMove", false)

    val repeatable = repeatable {
        mc.timer.timerSpeed = if (!onMove || player.moving) {
            speed
        } else {
            1f
        }
    }

    override fun disable() {
        mc.timer.timerSpeed = 1f
    }

    val disconnectHandler = handler<WorldDisconnectEvent> {
        enabled = false
    }

}
