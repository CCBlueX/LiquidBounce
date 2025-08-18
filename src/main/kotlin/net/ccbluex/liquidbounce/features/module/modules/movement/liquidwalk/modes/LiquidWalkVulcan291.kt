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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * LiquidWalk Vulcan 2.8.5 - 2.9.1(+?)
 *
 * @tested eu.loyisa.cn
 * @note May cause occasional step flags if player is in a 1x1 water hole.
 */
internal object LiquidWalkVulcan291 : Choice("Vulcan291") {

    private val motion by float("Motion", 0.8f, 0.2f..1.4f)

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleLiquidWalk.modes

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // It DOES NOT bypass with Lava - do not use [player.isInFluid].
        if (player.isInsideWaterOrBubbleColumn) {
            // One tick speed-up for extra speed
            Timer.requestTimerSpeed(1.125f, Priority.IMPORTANT_FOR_USAGE_1, ModuleLiquidWalk)

            // Acts as a high-jump
            player.velocity.y = motion.toDouble()
        } else {
            Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleLiquidWalk)
        }
    }

}

