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

import net.ccbluex.liquidbounce.event.EntityTickEvent
import net.ccbluex.liquidbounce.features.module.*
import net.ccbluex.liquidbounce.utils.extensions.downwards
import net.ccbluex.liquidbounce.utils.extensions.moving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.upwards

object ModuleSpeed : Module("Speed", Category.COMBAT) {

    private object SpeedChoiceConfigurable : ChoiceConfigurable(this, "Mode", "YPort", {
        SpeedYPort
    })

    private object SpeedYPort : Choice("YPort", SpeedChoiceConfigurable) {

        val tickHandler = sequenceHandler<EntityTickEvent> {
            if (player.isOnGround && player.moving) {
                player.strafe(0.4f)
                player.upwards(0.42f)
                wait(1)
                player.downwards(-1f)
            }

        }

    }

    init {
        SpeedChoiceConfigurable.initialize()
        tree(SpeedChoiceConfigurable)
    }

}

