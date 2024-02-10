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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.script.bindings.api.JsMovementUtil.strafe
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object SpeedVulcan286 : Choice("Vulcan286") {

    /*
        * Bypasses Vulcan 286
        * Tested on eu.loyisa.cn
        * Note: Ported from LiquidBounce Legacy
     */


    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    val repeatable = repeatable {
        if (player.isTouchingWater || player.isInLava || player.isHoldingOntoLadder) {
            return@repeatable
        }

        if (player.moving) {
            if (player.isOnGround) {
                player.jump()
                Timer.requestTimerSpeed(0.45f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            } else {
                Timer.requestTimerSpeed(1.125f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
            }
        } else {
            Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed)
        }
    }
}
