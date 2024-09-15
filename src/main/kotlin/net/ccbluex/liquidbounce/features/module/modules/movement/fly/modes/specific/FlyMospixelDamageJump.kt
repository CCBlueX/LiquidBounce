/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.specific

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.exactPosition
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket


object FlyMospixelDamageJump : Choice("MospixelDamageJump") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    private var ticksEnabled = 0

    val repeatable = repeatable {

        ticksEnabled++

        if (player.isOnGround) {
            player.jump()
        } else {

            player.velocity.y = 0.37
            player.strafe(speed = 0.82)
            Timer.requestTimerSpeed(1.2f, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
        }

        if (ticksEnabled == 8)
            ModuleFly.enabled = false

    }

    override fun enable() {
        ticksEnabled = 0

        val (x, y, z) = player.exactPosition

        repeat(65) {
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.049, z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false))
        }
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true))
        
        super.enable()
    }

}
