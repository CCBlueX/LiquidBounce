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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.polar

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.fakelag.DelayData
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

/**
 * @anticheat Hycraft (Polar)
 * @anticheat Version 15.05.2024
 * @testedOn mc.hycraft.us
 *
 * @note Tested in Bedwars, Skywars. Pretty much flagless
 */
internal object FlyHycraftDamage : Choice("HycraftDamage") {

    override val parent: ChoiceConfigurable<*>
        get() = modes

    private val packetQueue = LinkedHashSet<DelayData>()
    private var damageTaken = false
    private var release = false
    private var ticks = 0

    override fun disable() {
        if (inGame) {
            packetQueue.forEach { handlePacket(it.packet) }
        }

        packetQueue.clear()
    }

    override fun enable() {
        ticks = 0
        damageTaken = false
        release = false
    }

    val repeatable = repeatable {
        waitTicks(1)
        if (ticks > 0) ticks--
    }

    /**
     * Used to works on different servers as well but now only Hycraft
     */
    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id && ticks <= 0) {
            damageTaken = true
            ticks = 40
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && damageTaken) {
            packetQueue.add(DelayData(packet, System.currentTimeMillis()))
            damageTaken = false
            release = true
        }

        if (packet is CommonPingS2CPacket) {
            if (ticks > 0) {
                packetQueue.add(DelayData(packet, System.currentTimeMillis()))
                event.cancelEvent()

                ticks--
            } else {
                if (release) {
                    ModuleFly.enabled = false
                }
            }
        }

    }

}
