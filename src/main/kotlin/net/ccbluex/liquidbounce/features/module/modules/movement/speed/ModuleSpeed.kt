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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.Custom
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.LegitHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.Spartan524
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.Spartan524GroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.Verus

/**
 * Speed module
 *
 * Allows you to move faster.
 */

object ModuleSpeed : Module("Speed", Category.MOVEMENT) {

    val modes = choices(
        "Mode", SpeedYPort, arrayOf(
            Verus, SpeedYPort, LegitHop, Custom, Spartan524, Spartan524GroundTimer
        )
    )
}
