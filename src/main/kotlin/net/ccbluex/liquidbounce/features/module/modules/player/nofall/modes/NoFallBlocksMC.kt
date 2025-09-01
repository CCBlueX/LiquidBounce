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
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

internal object NoFallBlocksMC : Choice("BlocksMC") {

    private var shouldClip = false
    private var fallMotion = 0.0

    // Prevents this from running during AntiBot verification
    const val MIN_AGE = 20 * 5

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    override val running: Boolean
        get() = super.running && player.age > MIN_AGE

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.velocity.y < -0.7) {
            shouldClip = true
            fallMotion = player.velocity.y
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlayerMoveC2SPacket -> {
                if (player.isOnGround && shouldClip) {
                    packet.y -= 0.1
                }
            }
            is PlayerPositionLookS2CPacket -> shouldClip = false
        }
    }

}
