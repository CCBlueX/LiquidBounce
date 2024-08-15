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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * CustomAmbience module
 *
 * Override the ambience of the game
 */

object ModuleCustomAmbience : Module("CustomAmbience", Category.RENDER) {

    val weather = enumChoice("Weather", WeatherType.SUNNY)
    val time = enumChoice("Time", TimeType.NOON)

    enum class WeatherType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        SUNNY("Sunny"),
        RAINY("Rainy"),
        SNOWY("Snowy"),
        THUNDER("Thunder")
    }

    enum class TimeType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        DAY("Day"),
        NOON("Noon"),
        NIGHT("Night"),
        MID_NIGHT("MidNight")
    }

}
