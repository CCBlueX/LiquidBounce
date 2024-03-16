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
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

/**
 * @anticheat DoubleTNT
 * @anticheatVersion 17.03.2024
 * @testedOn pikanetwork.net
 *
 * @note Tested in Bedwars, works fine but setback.
 */
internal object FlyDoubleTnT : Choice("DoubleTNT") {

    override val parent: ChoiceConfigurable
        get() = ModuleFly.modes

    private val packetQueue = LinkedHashSet<DelayData>()
    private var canCancel = false
    private var ticks = 0
    private lateinit var velocityPacket: EntityVelocityUpdateS2CPacket;

    override fun disable() {
        if (inGame) {
            packetQueue.forEach { handlePacket(it.packet) }
        }

        packetQueue.clear()
    }

    override fun enable() {
        ticks = 0
        canCancel = false

        super.enable()
    }

    val repeatable = repeatable {
        waitTicks(1)
        if (ticks > 0) ticks--
    }

    /* Will not works if your version is below 1.17 */
    /* Semi polar flight? Got patched before */

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityDamageS2CPacket && packet.entityId == mc.player!!.id) {
            canCancel = true
            ticks = 40
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.id == mc.player!!.id && canCancel) {
            velocityPacket = packet;
            canCancel = false
        }

        if (packet is CommonPingS2CPacket) {
            if (ticks > 0) {
                packetQueue.add(DelayData(packet, System.currentTimeMillis()))
                event.cancelEvent()
                ticks--
                if (ticks == 40 / 2) handlePacket(velocityPacket)
            } else {
                if (packetQueue.isNotEmpty()) {
                    packetQueue.forEach { handlePacket(it.packet) }
                    packetQueue.clear()
                }
            }
        }
    }

}
