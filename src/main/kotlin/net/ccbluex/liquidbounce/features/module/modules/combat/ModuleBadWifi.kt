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
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.EventScheduler
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.util.math.Vec3d
import java.util.concurrent.CopyOnWriteArrayList

/**
 * BadWifi module
 *
 * Holds back packets to prevent you from being hit by an enemy.
 */
@Suppress("detekt:all")

object ModuleBadWifi : Module("BadWIFI", Category.COMBAT) {

    private val delay by int("Delay", 550, 0..1000)
    private val recoilTime by int("RecoilTime", 750, 0..2000)

    private val packetQueue = CopyOnWriteArrayList<ModulePingSpoof.DelayData>()
    private val positions = CopyOnWriteArrayList<PositionData>()

    private val resetTimer = Chronometer()

    override fun enable() {
        if (ModuleBlink.enabled) {
            // Cannot disable on the moment it's enabled, so schedule module deactivation in the next few milliseconds.
            EventScheduler.schedule(this, GameRenderEvent::class.java, action = {
                this.enabled = false
            })

            notification("Compatibility error", "BadWIFI is incompatible with Blink", NotificationEvent.Severity.ERROR)
        }
    }

    override fun disable() {
        if (mc.player == null) {
            return
        }

        blink()
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (player.isDead || event.isCancelled) {
            return@handler
        }

        val packet = event.packet

        when (packet) {
            is HandshakeC2SPacket,
            is QueryRequestC2SPacket,
            is QueryPingC2SPacket,
            is ChatMessageS2CPacket,
            is DisconnectS2CPacket -> {
                return@handler
            }

            // Flush on doing action, getting action
            is PlayerPositionLookS2CPacket,
            is PlayerInteractBlockC2SPacket,
            is PlayerActionC2SPacket,
            is UpdateSignC2SPacket,
            is PlayerInteractEntityC2SPacket,
            is ResourcePackStatusC2SPacket -> {
                blink()
                return@handler
            }

            // Flush on kb
            is EntityVelocityUpdateS2CPacket -> {
                if (packet.id == player.id
                    &&
                    (packet.velocityX != 0 ||
                        packet.velocityY != 0 ||
                        packet.velocityZ != 0)
                ) {
                    blink()
                    return@handler
                }
            }

            is ExplosionS2CPacket -> {
                if (packet.playerVelocityX != 0f ||
                    packet.playerVelocityY != 0f ||
                    packet.playerVelocityZ != 0f
                ) {
                    blink()
                    return@handler
                }
            }

            // Flush on damage
            is HealthUpdateS2CPacket -> {
                if (packet.health < player.health) {
                    blink()
                    return@handler
                }
            }
        }

        if (!resetTimer.hasElapsed(recoilTime.toLong())) {
            return@handler
        }

        if (event.origin == TransferOrigin.SEND) {
            event.cancelEvent()

            packetQueue.add(ModulePingSpoof.DelayData(packet, System.currentTimeMillis(), System.nanoTime()))
        }
    }

    val worldChangeHandler = handler<WorldChangeEvent> {
        // Clear packets on disconnect only
        if (it.world == null) {
            blink(false)
        }
    }

    val tickHandler = repeatable {
        if (player.isDead || player.isUsingItem) {
            blink()
            return@repeatable
        }

        if (!resetTimer.hasElapsed(recoilTime.toLong())) {
            return@repeatable
        }

        positions.add(PositionData(player.pos, System.currentTimeMillis(), System.nanoTime()))

        handlePackets()
    }


    // TODO: Add lines just like how Breadcrumbs module does it

    /*
    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val color = if (ModuleBreadcrumbs.colorRainbow) rainbow() else ModuleBreadcrumbs.color

        synchronized(ModuleBreadcrumbs.positions) {
            renderEnvironmentForWorld(matrixStack) {
                withColor(color) {
                    drawLineStrip(*ModuleBreadcrumbs.makeLines(color, ModuleBreadcrumbs.positions, event.partialTicks))
                }
            }
        }
    }*/

    private fun handlePackets() {
        // Use removeIf on this too? But this one has to be sorted
        val filteredPackets = packetQueue.filter {
            it.delay <= System.currentTimeMillis() - delay
        }.sortedBy { it.registration }

        for (data in filteredPackets) {
            sendPacketSilently(data.packet)

            packetQueue.remove(data)
        }

        positions.removeIf { it.delay <= System.currentTimeMillis() - delay }
    }

    private fun blink(handlePackets: Boolean = true) {
        if (handlePackets) {
            resetTimer.reset()

            val filtered = packetQueue.sortedBy { it.registration }

            for (data in filtered) {
                sendPacketSilently(data.packet)

                packetQueue.remove(data)
            }
        } else {
            packetQueue.clear()
        }

        positions.clear()
    }

    data class PositionData(val vec: Vec3d, val delay: Long, val registration: Long)
}
