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
package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider

/**
 * Spider Bypass for Vulcan 2.8.8
 *
 * Sneaking seems to reduce flags a bit for some reason.
 *
 * @anticheat Vulcan 2.8.8
 * @testedOn eu.loyisa.cn
 * @see net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
 *
 * TODO: Detection for how many blocks you've gone up. Anything over 40ish seems to flag for Invalid (C)
 *   Proper implementation if there's something above you needs to be added.
 */
internal object SpiderVulcan288 : Choice("Vulcan288") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    private var requiresStop = false

    val repeatable = tickHandler {
        if (player.horizontalCollision) {
            if (!player.isClimbing) {
                requiresStop = true
                waitTicks(2)
                player.velocity.y = 9.6599696
                waitTicks(2)
                player.setVelocity(0.0, 0.0001, 0.0)
            }
        }else if (requiresStop) {
            player.velocity.y = 0.0
            requiresStop = false
        }
    }

}
