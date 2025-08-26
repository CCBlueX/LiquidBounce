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
package net.ccbluex.liquidbounce.features.module.modules.movement.highjump

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * HighJump module
 *
 * Allows you to jump higher.
 */
object ModuleHighJump : ClientModule("HighJump", Category.MOVEMENT) {

    init {
        enableLock()
    }

    private val modes = choices(
        "Mode", Vanilla, arrayOf(
            Vanilla, Vulcan
        )
    ).apply { tagBy(this) }
    private val motion by float("Motion", 0.8f, 0.2f..10f)

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
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

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        var glide by boolean("Glide", false)

        var shouldGlide = false

        @Suppress("unused")
        val repeatable = tickHandler {
            if (glide && shouldGlide) { // if the variable is true, then glide
                if (player.isOnGround) {
                    shouldGlide = false
                    return@tickHandler
                }
                if (player.fallDistance > 0) {
                    if (player.age % 2 == 0) {
                        player.velocity.y = -0.155
                    }
                } else {
                    player.velocity.y = -0.1
                }
            }
        }

        @Suppress("unused")
        val jumpEvent = sequenceHandler<PlayerJumpEvent> {
            it.motion = motion
            waitTicks(100)
            player.velocity.y = 0.0
            shouldGlide = true
        }
    }
}
