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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.AlignmentConfigurable

object ModuleScoreboard : Module("Scoreboard", Category.RENDER) {

    val turnOff by boolean("TurnOff", false)
    val alignment =
        AlignmentConfigurable(
            horizontalAlignment = AlignmentConfigurable.ScreenAxisX.RIGHT,
            horizontalPadding = 0,
            verticalAlignment = AlignmentConfigurable.ScreenAxisY.BOTTOM,
            verticalPadding = 16,
        )

    init {
        tree(alignment)
    }


}
