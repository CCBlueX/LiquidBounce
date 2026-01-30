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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.random

/**
 * @anticheat Sentinel
 * @anticheatVersion 27.01.2024
 * @testedOn cubecraft.net
 *
 * @note Tested in SkyWars and EggWars, works fine and no automatic ban.
 * @note Glides down and by pressing spacebar, it will go up. It also has a horizontal speed.
 * This fly does not require any disabler.
 *
 * Thanks to icewormy3
 */
internal object FlySentinel27thJan : Mode("Sentinel27thJan") {

    private val horizontalSpeed by floatRange("HorizontalSpeed", 0.33f..0.34f, 0.1f..1f)

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    val repeatable = tickHandler {
        if (player.onGround()) {
            return@tickHandler
        }

        player.deltaMovement.y = when {
            player.isShiftKeyDown -> -0.4
            player.input.keyPresses.jump -> 0.42
            else -> 0.2
        }
        player.deltaMovement = player.deltaMovement.withStrafe(speed = horizontalSpeed.random().toDouble())

        waitTicks(6)
    }

    val moveHandler = handler<PlayerMoveEvent> { event ->
        event.movement = event.movement.withStrafe()
    }

}
