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

package net.ccbluex.liquidbounce.features.module.modules.render.wings

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.wings.modes.WingsLines

object ModuleWings : ClientModule("Wings", ModuleCategories.RENDER) {

    object WingsPosition : ValueGroup("wingsPosition") {
        val wingsHeight by float("wingsHeight", 0.3f, -1f..1f)
        val behindScale by float("BackOffset", 0.25f, 0f..0.5f)
        val equipmentOffset by float("equipmentOffset", 0.1f, 0f..1f)
    }

    init {
        tree(WingsPosition)
    }

    val modes = choices("Mode", 0) {
        arrayOf(
            WingsLines,
        )
    }.apply { tagBy(this) }

}
