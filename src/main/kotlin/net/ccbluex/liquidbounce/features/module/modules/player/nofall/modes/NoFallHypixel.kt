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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.isFallingToVoid
import net.ccbluex.liquidbounce.utils.kotlin.Priority

internal object NoFallHypixel : Choice("Hypixel") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val repeatable = repeatable {
        if (player.fallDistance >= 2.5 && !player.isFallingToVoid()) {
            Timer.requestTimerSpeed(0.5f, Priority.IMPORTANT_FOR_PLAYER_LIFE, ModuleNoFall)
            network.sendPacket(MovePacketType.ON_GROUND_ONLY.generatePacket().apply {
                onGround = true
            })
            player.fallDistance = 0F
            waitTicks(1)
            Timer.requestTimerSpeed(1f, Priority.NORMAL, ModuleNoFall)
        }

    }

}
