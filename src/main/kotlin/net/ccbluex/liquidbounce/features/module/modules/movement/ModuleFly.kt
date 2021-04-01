/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable

object ModuleFly : Module("Fly", Category.MOVEMENT) {

    val mode by chooseList("Mode", "Creative", arrayOf("Creative", "Jetpack"))

    override fun disable() {
        player.abilities.flying = false
    }

    val repeatable = repeatable {
        when (mode) {
            "Creative" -> player.abilities.flying = true
            "Jetpack" -> {
                if (mc.options.keyJump.isPressed) {
                    player.velocity.x *= 1.1
                    player.velocity.y += 0.15
                    player.velocity.z *= 1.1
                }
            }
        }
    }
}