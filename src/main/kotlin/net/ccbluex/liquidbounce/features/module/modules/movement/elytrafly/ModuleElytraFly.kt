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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes.*

/**
 * ElytraFly module
 *
 * Makes you fly faster on Elytra.
 */

object ModuleElytraFly : Module("ElytraFly", Category.MOVEMENT) {

    val instant by boolean("Instant", true)
    val instantStop by boolean("InstantStop", false)
    object Speed : ToggleableConfigurable(this, "Speed", true) {
        val vertical by float("Vertical", 0.5f, 0.1f..2f)
        val horizontal by float("Horizontal", 1f, 0.1f..2f)
    }

    init {
        tree(Speed)
    }

    internal val modes = choices("Mode", ElytraVanilla, arrayOf(
        ElytraStatic,
        ElytraVanilla
    ))
}
