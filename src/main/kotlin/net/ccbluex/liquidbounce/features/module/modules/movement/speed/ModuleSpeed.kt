/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan.SpeedSpartan524
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan.SpeedSpartan524GroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.verus.SpeedVerusB3882
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcan286
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog.SpeedHypixelBHop
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

/**
 * Speed module
 *
 * Allows you to move faster.
 */

object ModuleSpeed : Module("Speed", Category.MOVEMENT) {

    init {
        enableLock()
    }

    val modes = choices(
        "Mode", SpeedLegitHop, arrayOf(
            SpeedLegitHop,
            SpeedCustom,
            SpeedSpeedYPort,

            SpeedVerusB3882,
            SpeedHypixelBHop,

            SpeedSpartan524,
            SpeedSpartan524GroundTimer,

            SpeedVulcan286,
        )
    )

    private val notDuringScaffold by boolean("NotDuringScaffold", true)

    override fun handleEvents(): Boolean {
        if (notDuringScaffold && ModuleScaffold.enabled) {
            return false
        }

        return super.handleEvents()
    }

}
