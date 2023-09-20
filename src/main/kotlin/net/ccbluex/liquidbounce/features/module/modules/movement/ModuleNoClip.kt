/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * NoClip module
 *
 * Allows you to fly through blocks.
 */

object ModuleNoClip : Module("NoClip", Category.MOVEMENT) {

    val speed by float("Speed", 0.32f, 0.1f..0.4f)

    val repeatable = repeatable {
        player.noClip = true
        player.fallDistance = 0f
        player.isOnGround = false

        val speed = speed.toDouble()
        player.strafe(speed = speed)

        player.velocity.y = when {
            mc.options.jumpKey.isPressed -> speed
            mc.options.sneakKey.isPressed -> -speed
            else -> 0.0
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        // Setback detection
        if (event.packet is PlayerPositionLookS2CPacket) {
            chat(regular(this.message("setbackDetected")))
            enabled = false
        }
    }

    override fun disable() {
        player.noClip = false
    }

}
