/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.utils.entity.strafe

/**
 * @anticheat NoCheatPlus
 * @testedOn anticheat-test.com + eu.loyisa.cn + blocksmc.com
 */
 
class SpeedNoCheatPlusLowHop(override val parent: ChoiceConfigurable<*>) : Choice("NoCheatPlusLowHop") {

    private var airTicks = 0

    val repeatable = repeatable {

        player.strafe()

        if (player.isOnGround) {
            player.jump()
            airTicks = 0
        } else {
            if (airTicks == 3) {
                player.velocity.y = -0.09800000190734863
            }
            airTicks++
        }
    }
}
