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
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * Twerk module
 *
 * Automatically sneaks and unsneaks.
 */
object ModuleTwerk : ClientModule("Twerk", Category.FUN) {

    /**
     * How long each sneak / normal period is.
     */
    val delay by int("Delay", 2, 1..20)

    /**
     * Handles sneaking and unsneaking, has a high priority so that more important modules can override it.
     */
    val movementInputHandler = handler<MovementInputEvent>(priority = 10) {
        val sneaking = player.age % (delay * 2) < delay
        it.sneak = sneaking
    }

}
