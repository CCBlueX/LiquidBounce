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

import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.downwards
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.entity.effect.StatusEffects

/**
 * BHop Speed for Vulcan 286
 * Taken from InspectorBoat Vulcan Bypasses (He agreed to it)
 *
 * Tested on both anticheat-test.com and loyisa.cn
 */
object SpeedVulcan286 : SpeedBHopBase("Vulcan286") {

    private inline val goingSideways: Boolean
        get() = player.input.movementSideways != 0f

    val afterJumpEvent = sequenceHandler<PlayerAfterJumpEvent> {
        // We might lose the effect during runtime of the sequence,
        // but we don't care, since it is Vulcan.
        val speedLevel = (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)

        waitTicks(1)
        player.strafe(speed = if (goingSideways) 0.3345 else 0.3355 * (1 + speedLevel * 0.3819))
        waitTicks(1)
        if (player.isSprinting) {
            player.strafe(speed = if (goingSideways) 0.3235 else 0.3284 * (1 + speedLevel * 0.355))
        }

        waitTicks(2)
        player.downwards(0.376f)

        waitTicks(2)
        if (player.speed > 0.298) {
            player.strafe(speed = 0.298)
        }
    }

}
