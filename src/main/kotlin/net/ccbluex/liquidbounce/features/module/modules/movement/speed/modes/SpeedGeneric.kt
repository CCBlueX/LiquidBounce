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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.entity.downwards
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.upwards

class SpeedSpeedYPort(override val parent: ChoiceConfigurable<*>) : Choice("YPort") {

    val repeatable = repeatable {
        if (player.isOnGround && player.moving) {
            player.strafe(speed = 0.4)
            player.upwards(0.42f)
            waitTicks(1)
            player.downwards(1f)
        }
    }

}

class SpeedLegitHop(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("LegitHop", parent)

open class SpeedBHopBase(name: String, override val parent: ChoiceConfigurable<*>) : Choice(name) {

    val handleMovementInput = handler<MovementInputEvent> {
        if (!player.isOnGround || !player.moving) {
            return@handler
        }

        // We want the player to be able to jump if he wants to
        if (!mc.options.jumpKey.isPressed && ModuleSpeed.shouldDelayJump())
            return@handler

        it.jumping = true
    }

}
