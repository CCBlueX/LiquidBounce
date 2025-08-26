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
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.entity.set
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * @anticheat Vulcan
 * @anticheatVersion 2.8.8
 * @testedOn eu.loyisa.cn
 */
internal object NoFallVulcanTP : Choice("VulcanTP288") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private val voidThreshold by int("VoidLevel", 0, -256..0)

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket && player.fallDistance in 2.5..50.0
            // Check if the player is falling into the void and set safety expand to 0.0 - otherwise,
            // the player will be teleported to the void and flag
            && !player.doesNotCollideBelow(until = voidThreshold.toDouble())) {
            // Rewrite the packet to make the server think we're on the ground
            packet.onGround = true

            // Extreme high fall velocity
            player.setVelocity(0.0, -99.887575, 0.0)
            player.input.set(
                sneak = true
            )
        }
    }

}
