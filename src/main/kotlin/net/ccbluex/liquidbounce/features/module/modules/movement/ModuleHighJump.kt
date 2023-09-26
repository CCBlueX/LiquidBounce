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
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * HighJump module
 *
 * Allows you to jump higher.
 */
object ModuleHighJump : Module("HighJump", Category.MOVEMENT) {

    private val modes = choices(
        "Mode", Vanilla, arrayOf(
            Vanilla, Vulcan
        )
    )
    private val motion by float("Motion", 0.8f, 0.2f..10f)

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable
            get() = modes

        val jumpEvent = sequenceHandler<PlayerJumpEvent> {
            it.motion = motion
        }
    }

    /**
     * @anticheat Vulcan
     * @anticheatVersion 2.7.5
     * @testedOn eu.loyisa.cn; eu.anticheat-test.com
     * @note this still flags a bit
     */
    private object Vulcan : Choice("Vulcan") {

        override val parent: ChoiceConfigurable
            get() = modes

        var glide by boolean("Glide", false)

        var shouldGlide = false

        val repeatable = repeatable {
            if (glide && shouldGlide) { // if the variable is true, then glide
                if (player.isOnGround) {
                    shouldGlide = false
                    return@repeatable
                }
                if (player.fallDistance > 0) {
                    if (player.age % 2 == 0) {
                        player.velocity.y = -0.155
                    }
                } else player.velocity.y = -0.1
            }
        }
        val jumpEvent = sequenceHandler<PlayerJumpEvent> {
            it.motion = motion
            wait { 100 }
            player.velocity.y = 0.0
            shouldGlide = true
        }
    }
}
