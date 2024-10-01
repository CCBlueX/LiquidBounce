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
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b

/**
 * CustomAmbience module
 *
 * Override the ambience of the game
 */
object ModuleCustomAmbience : Module("CustomAmbience", Category.RENDER) {

    val weather = enumChoice("Weather", WeatherType.SUNNY)
    val time = enumChoice("Time", TimeType.NOON)

    object CustomLightColor : ToggleableConfigurable(this, "CustomLightColor", false) {

        private val lightColor by color("LightColor", Color4b(70, 119, 255, 255))

        fun blendWithLightColor(srcColor: Int): Int {
            if (lightColor.a == 255) {
                return lightColor.toABGR()
            } else if (lightColor.a == 0) {
                return srcColor
            }

            val srcB = (srcColor shr 16) and 0xFF
            val srcG = (srcColor shr 8) and 0xFF
            val srcR = srcColor and 0xFF

            val dstAlpha = lightColor.a / 255f

            val outB = ((srcB * (1 - dstAlpha)) + (lightColor.b * dstAlpha)).toInt()
            val outG = ((srcG * (1 - dstAlpha)) + (lightColor.g * dstAlpha)).toInt()
            val outR = ((srcR * (1 - dstAlpha)) + (lightColor.r * dstAlpha)).toInt()

            return (255 shl 24) or (outB shl 16) or (outG shl 8) or outR
        }

    }

    init {
        tree(CustomLightColor)
    }

    enum class WeatherType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        SUNNY("Sunny"),
        RAINY("Rainy"),
        SNOWY("Snowy"),
        THUNDER("Thunder")
    }

    enum class TimeType(override val choiceName: String) : NamedChoice {
        NO_CHANGE("NoChange"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("MidNight")
    }

}
