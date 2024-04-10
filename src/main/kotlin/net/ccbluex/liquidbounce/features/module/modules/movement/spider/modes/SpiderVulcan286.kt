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
package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider


    /*
    * Vulcan mode for the Spider module.
    * Made for Vulcan286
    * Tested on Eu.loyisa.cn and Anticheat-test.com
    * It may still flag sometimes, particularly when going more then 15-30 blocks up.
     */

internal object SpiderVulcan286 : Choice("Vulcan") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    private var tickCounter = 0
    private var jumpDelayCounter = 0

    val repeatable = repeatable {
        if (player.isHoldingOntoLadder || player.isTouchingWater || player.isInLava) {
            tickCounter = 0
            return@repeatable
        }

        if (tickCounter >= 3)
            tickCounter = 0

        tickCounter++

        if (player.horizontalCollision && tickCounter == 2 % 3) {
            jumpDelayCounter++
            if (jumpDelayCounter >= 3) {
                player.jump()
                player.forwardSpeed = 0F
                player.sidewaysSpeed = 0F
                jumpDelayCounter = 0
            }
        }
    }
}
