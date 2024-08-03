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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.kotlin.Priority

class SpeedIntave14(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Intave14", parent) {
    private class Boost(parent: Listenable?) : ToggleableConfigurable(parent, "Boost", false) {
        private val boost by float("Boost", 1.0015F, 1.0F..2.0F)
            val repeatable = repeatable {

            if (player.velocity.y > 0.003) {
                player.velocity = player.velocity.multiply(
                    boost.toDouble(),
                    1.0,
                    boost.toDouble()
                )
            }
        }
    }

    init {
        tree(Boost(this))
    }

    private val groundtimer by float("GroundTimer", 1.06F, 0.1F..10.0F)
    private val airtimer by float("AirTimer", 0.98F, 0.1F..10.0F)

        val repeatable = repeatable {

            if (player.isOnGround && player.isSprinting) {
                player.strafe(player.directionYaw, strength = 0.29)
            }

            if (player.isOnGround) {
                Timer.requestTimerSpeed(groundtimer, priority = Priority.NORMAL, provider = ModuleSpeed)
            } else {
                Timer.requestTimerSpeed(airtimer, priority = Priority.NORMAL, provider = ModuleSpeed)
            }
        }
    }
