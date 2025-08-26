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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.kotlin.Priority

internal object NoFallHypixelPacket : Choice("HypixelPacket") {

    private val void by boolean("OverVoid", false)

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private fun voidCheck(): Boolean {
        return (!player.doesNotCollideBelow() && !void || void)
    }

    val repeatable = tickHandler {
        if (player.fallDistance - player.velocity.y >= 3.3 && voidCheck()) {
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
