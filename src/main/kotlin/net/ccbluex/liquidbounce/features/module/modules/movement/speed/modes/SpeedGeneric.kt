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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed.doOptimizationsPreventJump
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy

class SpeedSpeedYPort(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("YPort", parent) {

    private val speed by float("Speed", 0.4f, 0.1f..1f)

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!player.isOnGround && player.moving) {
            player.velocity = player.velocity.copy(y = -1.0)
        }
    }

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        player.velocity = player.velocity.withStrafe(speed = speed.toDouble())
    }

}

class SpeedLegitHop(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("LegitHop", parent)

open class SpeedBHopBase(name: String, override val parent: ChoiceConfigurable<*>) : Choice(name) {

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (!player.isOnGround || !event.directionalInput.isMoving) {
            return@handler
        }

        if (doOptimizationsPreventJump()) {
            return@handler
        }

        event.jump = true
    }

}
