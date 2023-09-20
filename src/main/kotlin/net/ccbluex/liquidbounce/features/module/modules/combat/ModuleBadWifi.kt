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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.util.math.Vec3d
import java.util.concurrent.LinkedBlockingQueue

/**
 * BadWifi module
 *
 * Holds back packets so as to prevent you from being hit by an enemy.
 */

object ModuleBadWifi : Module("BadWIFI", Category.COMBAT) {

    val maxPacketsInBuffer by intRange("MaxPacketsInBuffer", 20..30, 5..100)
    val enemyRange by float("EnemyRange", 5.0f, 1.0f..10.0f)

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var disablelogger = false
    private var currentlyBlinking = false
    private var currentMaxPackets: Int = 0

    private var serverSidePosition: Vec3d? = null

    override fun enable() {
        if (ModuleBlink.enabled) {
            this.enabled = false

            notification("Compatibility error", "BadWIFI is incompatible with Blink", NotificationEvent.Severity.ERROR)
        }

        refreshMaxPackets()
    }

    override fun disable() {
        if (mc.player == null) {
            return
        }

        blink()
    }

    val packetHandler = handler<PacketEvent>(priority = -1) { event ->
        if (mc.player == null || disablelogger || !currentlyBlinking || event.origin != TransferOrigin.SEND) {
            return@handler
        }

        if (event.packet is PlayerInteractEntityC2SPacket) {
            blink()

            return@handler
        }

        if (event.packet is PlayerMoveC2SPacket && !event.packet.changePosition) return@handler

        if (event.packet is PlayerMoveC2SPacket || event.packet is PlayerInteractBlockC2SPacket || event.packet is HandSwingC2SPacket || event.packet is PlayerActionC2SPacket || event.packet is PlayerInteractEntityC2SPacket) {
            event.cancelEvent()

            packets.add(event.packet)
        }
    }

    val repeatable = repeatable {
        val player = player
        val rangeSquared = enemyRange * enemyRange

        var currentPosition = if (currentlyBlinking) {
            serverSidePosition
        } else {
            player.pos
        }

        if (currentPosition == null) currentPosition = player.pos

        val threat =
            world.entities.firstOrNull { it.squaredDistanceTo(currentPosition) <= rangeSquared && it.shouldBeAttacked() }

        if (threat != null && currentlyBlinking && threat.squaredDistanceTo(currentPosition) < threat.squaredDistanceTo(
                player
            )
        ) {
            blink()
        }

        if (threat == null && !currentlyBlinking) {
            currentlyBlinking = true

            serverSidePosition = player.pos

            chat("START")
        }

        disablelogger = true

        runCatching {
            while (packets.size > currentMaxPackets) {
                network.sendPacket(packets.take())
            }
        }

        disablelogger = false
    }

    private fun blink() {
        chat("BLINK")

        try {
            disablelogger = true

            while (!packets.isEmpty()) {
                network.sendPacket(packets.take())
            }

            disablelogger = false
        } catch (e: Exception) {
            e.printStackTrace()

            disablelogger = false
        }

        currentlyBlinking = false

        refreshMaxPackets()
    }

    private fun refreshMaxPackets() {
        currentMaxPackets = maxPacketsInBuffer.random()
    }
}
