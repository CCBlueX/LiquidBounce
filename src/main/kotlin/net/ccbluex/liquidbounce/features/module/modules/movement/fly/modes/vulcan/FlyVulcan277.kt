/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes

/**
 * @anticheat Vulcan
 * @anticheat Version 2.7.7
 * @testedOn anticheat-test.com
 * @note NA
 */
internal object FlyVulcan277 : Choice("Vulcan277") {

    override val parent: ChoiceConfigurable<*>
        get() = modes

    val repeatable = repeatable {
        if (player.fallDistance > 0.1) {
            if (player.age % 2 == 0) {
                player.velocity.y = -0.155
            } else {
                player.velocity.y = -0.1
            }
        }
    }

}

