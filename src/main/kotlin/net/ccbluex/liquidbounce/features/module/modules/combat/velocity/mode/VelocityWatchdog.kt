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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

internal object VelocityWatchdog : Choice("Watchdog") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    /**
     * Cancel complete
     */
    private var cancelComplete by intRange("CancelComplete", 0..0, 0..5)
    private var hits = 0

    /**
     * Blink until we reach ground after cancelling in air
     */
    private var blinkUntilGroundOpt by boolean("BlinkUntilGround", false)
    private var blinkUntilGround = false

    internal val shouldLag: Boolean
        get() = isActive && handleEvents() && blinkUntilGroundOpt && blinkUntilGround

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        // Check if this is a regular velocity update
        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            if (!player.isOnGround) {
                hits++
                if (cancelComplete == 0..0 || hits in cancelComplete) {
                    event.cancelEvent()
                    blinkUntilGround = true
                    return@handler
                }
            }

            packet.velocityX = (player.velocity.x * 8000).toInt()
            packet.velocityZ = (player.velocity.z * 8000).toInt()
        }
    }

    @Suppress("unused")
    private val gameHandler = repeatable {
        if (player.isOnGround) {
            blinkUntilGround = false
        }

        // Check if there is enemy nearby
        if (hits > 0 && world.findEnemy(range = 0f..4f) == null) {
            hits = 0
        }
    }

}
