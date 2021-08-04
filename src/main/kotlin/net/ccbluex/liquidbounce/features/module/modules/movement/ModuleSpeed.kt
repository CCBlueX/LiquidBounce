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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.utils.client.timer
import net.ccbluex.liquidbounce.utils.entity.downwards
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.upwards

/**
 * Speed module
 *
 * Allows you to move faster.
 */

object ModuleSpeed : Module("Speed", Category.MOVEMENT) {

    private val modes = choices(
        "Mode",
        SpeedYPort,
        arrayOf(
            SpeedYPort,
            LegitHop,
            Custom
        )
    )

    private val optimizeForCriticals by boolean("OptimizeForCriticals", true)

    private object SpeedYPort : Choice("YPort") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (player.isOnGround && player.moving) {
                player.strafe(speed = 0.4)
                player.upwards(0.42f)
                wait(1)
                player.downwards(-1f)
            }
        }

    }

    private object LegitHop : Choice("LegitHop") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (optimizeForCriticals && ModuleCriticals.shouldWaitForJump(0.42f)) {
                return@repeatable
            }

            if (player.isOnGround && player.moving) {
                player.jump()
            }
        }

    }

    private object Custom : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        private val horizontalSpeed by float("HorizontalSpeed", 1f, 0.1f..10f)
        private val resetHorizontalSpeed by boolean("ResetHorizontalSpeed", true)
        private val customStrafe by boolean("CustomStrafe", false)
        private val strafe by float("Strafe", 1f, 0.1f..10f)
        private val verticalSpeed by float("VerticalSpeed", 0.42f, 0.1f..10f)
        private val resetVerticalSpeed by boolean("ResetVerticalSpeed", true)
        private val timerSpeed by float("TimerSpeed", 1f, 0.1f..10f)

        val repeatable = repeatable {
            if (player.moving) {
                mc.timer.timerSpeed = timerSpeed

                when {
                    player.isOnGround -> {
                        player.strafe(speed = horizontalSpeed.toDouble())
                        player.velocity.y = verticalSpeed.toDouble()
                    }
                    customStrafe -> player.strafe(speed = strafe.toDouble())
                    else -> player.strafe()
                }
            }
        }

        override fun enable() {
            if(resetHorizontalSpeed) {
                player.velocity.x = 0.0
                player.velocity.z = 0.0
            }

            if (resetVerticalSpeed) player.velocity.y = 0.0
        }

        override fun disable() {
            mc.timer.timerSpeed = 1f
        }
    }

}
