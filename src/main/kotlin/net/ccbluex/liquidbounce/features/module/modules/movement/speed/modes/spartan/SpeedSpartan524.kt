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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.zeroXZ


/**
 * @anticheat Spartan
 * @anticheatVersion phase 524
 * @testedOn minecraft.vagdedes.com
 * @note it might flag a bit at the start, but then stops for some reason
 */
class SpeedSpartan524(override val parent: ChoiceConfigurable<*>) : Choice("Spartan524") {

    val repeatable = repeatable {
        if (!player.moving) {
            return@repeatable
        }

        Timer.requestTimerSpeed(1.1f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)

        when {
            player.isOnGround -> {
                player.strafe(speed = 0.83)
                player.velocity.y = 0.16
            }
        }
        player.strafe()
    }

    override fun enable() {
        player.zeroXZ()
        player.velocity.y = 0.0
    }
}

/**
 * @anticheat Spartan
 * @anticheatVersion phase 524
 * @testedOn minecraft.vagdedes.com
 * @note it will flag you for jumping
 */
class SpeedSpartan524GroundTimer(override val parent: ChoiceConfigurable<*>) : Choice("Spartan524GroundTimer") {

    private val additionalTicks by int("AdditionalTicks", 2, 1..10, "ticks")

    val repeatable = handler<PlayerPostTickEvent> {
        repeat(additionalTicks) {
            player.tickMovement()
        }
    }

    val jumpEvent = handler<PlayerJumpEvent> { event ->
        event.cancelEvent()
    }
}

