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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.verus

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.MovementType

/**
 * @anticheat Verus
 * @anticheatVersion b3882
 * @testedOn eu.anticheat-test.com
 */
class SpeedVerusB3882(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("VerusB3882", parent) {

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        player.velocity.x *= 1.1
        player.velocity.z *= 1.1
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        // Might just strafe when player controls itself
        if (event.type == MovementType.SELF && player.moving) {
            event.movement = event.movement.withStrafe(strength = 1.0)
        }
    }

    @Suppress("unused")
    private val timerHandler = tickHandler {
        Timer.requestTimerSpeed(2.0F, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        waitTicks(101)
    }

}
