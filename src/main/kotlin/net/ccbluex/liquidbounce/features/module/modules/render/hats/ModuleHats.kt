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

package net.ccbluex.liquidbounce.features.module.modules.render.hats

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsConeHat
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsHalo
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsOrbs
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsFlower
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsStar
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * @author minecrrrr
 */
object ModuleHats : ClientModule("Hats", Category.RENDER) {

    val height by float("HeightOffset", 0.1f, 0f..2f)
    val showInFirstPerson by boolean("FirstPersonView", true)

    object Colors : Configurable("Colors") {
        val syncColors by boolean("SyncColors", true)
        val firstColor by color("InnerColor", Color4b(0, 0, 255, 125))
        val secondColor by color("OuterColor", Color4b(0, 0, 255, 125))
    }

    init {
        tree(Colors)
    }
    val modes = choices(
        "Mode", HatsConeHat, arrayOf(
            HatsConeHat,
            HatsHalo,
            HatsOrbs,
            HatsFlower,
            HatsStar,
        )
    ).apply { tagBy(this) }

}
