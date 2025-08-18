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
package net.ccbluex.liquidbounce.utils.client

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.client.option.Perspective
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Allows to queue packets and flush them later on demand.
 *
 * Fires [QueuePacketEvent] to determine whether a packet should be queued or not. They can be
 * from origin [TransferOrigin.INCOMING] or [TransferOrigin.OUTGOING], but will be handled separately.
 */
object PacketQueueManager : EventListener {

    val packetQueue: ConcurrentLinkedQueue<PacketSnapshot> = Queues.newConcurrentLinkedQueue()
    val positions
        get() = packetQueue
            .map { snapshot -> snapshot.packet }
            .filterIsInstance<PlayerMoveC2SPacket>()
            .filter { playerMoveC2SPacket -> playerMoveC2SPacket.changePosition }
            .map { playerMoveC2SPacket -> Vec3d(playerMoveC2SPacket.x, playerMoveC2SPacket.y, playerMoveC2SPacket.z) }

    val isLagging
        get() = packetQueue.isNotEmpty()

    @Suppress("unused")
    private val flushHandler = handler<GameRenderTaskQueueEvent> {
        if (mc.networkHandler?.connection?.isOpen != true) {
            packetQueue.clear()
            return@handler
        }

        if (fireEvent(null, TransferOrigin.OUTGOING) == Action.FLUSH) {
            flush { snapshot -> snapshot.origin == TransferOrigin.OUTGOING }
        }
    }

    @Suppress("unused")
    private val flushReceiveHandler = handler<TickPacketProcessEvent> {
        if (mc.networkHandler?.connection?.isOpen != true) {
            packetQueue.clear()
            return@handler
        }

        if (fireEvent(null, TransferOrigin.INCOMING) == Action.FLUSH) {
            flush { snapshot -> snapshot.origin == TransferOrigin.INCOMING }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = FINAL_DECISION) { event ->
        // Ignore packets that are already cancelled, as they are already handled
        if (event.isCancelled) {
            return@handler
        }

        val packet = event.packet
        val origin = event.origin

        // If we shouldn't lag, don't do anything
        val lagResult = fireEvent(packet, origin)
        if (lagResult == Action.FLUSH) {
            flush { snapshot -> snapshot.origin == origin }
            return@handler
        }

        if (lagResult == Action.PASS) {
            return@handler
        }

        when (packet) {

            is HandshakeC2SPacket, is QueryRequestC2SPacket, is QueryPingC2SPacket -> {
                return@handler
            }

            // Ignore message-related packets
            is ChatMessageC2SPacket, is GameMessageS2CPacket, is CommandExecutionC2SPacket -> {
                return@handler
            }

            // Flush on teleport or disconnect
            is PlayerPositionLookS2CPacket, is DisconnectS2CPacket -> {
                flush { snapshot -> snapshot.origin == origin }
                return@handler
            }

            // Ignore own hurt sounds
            is PlaySoundS2CPacket -> {
                if (packet.sound.value() == SoundEvents.ENTITY_PLAYER_HURT) {
                    return@handler
                }
            }

            // Flush on own death
            is HealthUpdateS2CPacket -> {
                if (packet.health <= 0) {
                    flush { snapshot -> snapshot.origin == origin }
                    return@handler
                }
            }

        }

        event.cancelEvent()
        packetQueue.add(
            PacketSnapshot(
                packet,
                origin,
                System.currentTimeMillis()
            )
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        // Clear packets on disconnect
        if (event.world == null) {
            packetQueue.clear()
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            // Use LiquidBounce accent color
            withColor(Color4b.LIQUID_BOUNCE) {
                drawLineStrip(positions = positions.mapArray { vec3d -> Vec3(relativeToCamera(vec3d)) })
            }
        }

        val perspectiveEvent = EventManager.callEvent(PerspectiveEvent(mc.options.perspective))
        if (perspectiveEvent.perspective != Perspective.FIRST_PERSON) {
            val pos = positions.firstOrNull() ?: return@handler
            val rotation = RotationManager.actualServerRotation

            val wireframePlayer = WireframePlayer(pos, rotation.yaw, rotation.pitch)
            wireframePlayer.render(event, Color4b(36, 32, 147, 87), Color4b(36, 32, 147, 255))
        }
    }

    fun flush(flushWhen: (PacketSnapshot) -> Boolean) {
        packetQueue.removeIf { snapshot ->
            if (flushWhen(snapshot)) {
                flushSnapshot(snapshot)
                true
            } else {
                false
            }
        }
    }

    fun flush(count: Int) {
        // Take all packets until the counter of move packets reaches count and send them
        var counter = 0

        for (snapshot in packetQueue.iterator()) {
            val packet = snapshot.packet

            if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                counter += 1
            }

            flushSnapshot(snapshot)
            packetQueue.remove(snapshot)

            if (counter >= count) {
                break
            }
        }
    }

    fun cancel() {
        positions.firstOrNull()?.let { pos ->
            player.setPosition(pos)
        }

        for (snapshot in packetQueue) {
            when (snapshot.packet) {
                is PlayerMoveC2SPacket -> continue
                else -> flushSnapshot(snapshot)
            }
        }
        packetQueue.clear()
    }

    fun isAboveTime(delay: Long): Boolean {
        val entryPacketTime = (packetQueue.firstOrNull()?.timestamp ?: return false)
        return System.currentTimeMillis() - entryPacketTime >= delay
    }

    inline fun <reified T> rewrite(action: (T) -> Unit) {
        packetQueue
            .filterIsInstance<T>()
            .forEach(action)
    }

    private fun flushSnapshot(snapshot: PacketSnapshot) {
        when (snapshot.origin) {
            TransferOrigin.OUTGOING -> sendPacketSilently(snapshot.packet)
            TransferOrigin.INCOMING -> handlePacket(snapshot.packet)
        }
    }

    private fun fireEvent(packet: Packet<*>?, origin: TransferOrigin) =
        EventManager.callEvent(QueuePacketEvent(packet, origin)).action

    enum class Action {
        QUEUE,
        PASS,
        FLUSH,
    }

}

data class PacketSnapshot(
    val packet: Packet<*>,
    val origin: TransferOrigin,
    val timestamp: Long
)

