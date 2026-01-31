/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.blink

import com.google.common.collect.Queues
import net.ccbluex.fastutil.filterIsInstance
import net.ccbluex.fastutil.forEachIsInstance
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameRenderTaskQueueEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspBox
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspModel
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspWireframe
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspNone
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspData
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.minecraft.client.CameraType
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Allows queueing packets and flush them later on demand.
 *
 * Fires [BlinkPacketEvent] to determine whether a packet should be queued or not. They can be
 * from origin [TransferOrigin.INCOMING] or [TransferOrigin.OUTGOING], but will be handled separately.
 */
object BlinkManager : EventListener, ValueGroup("BlinkManager") {

    val packetQueue: ConcurrentLinkedQueue<PacketSnapshot> = Queues.newConcurrentLinkedQueue()
    val positions
        get() = packetQueue
            .map { snapshot -> snapshot.packet }
            .filterIsInstance<ServerboundMovePlayerPacket> { playerMoveC2SPacket -> playerMoveC2SPacket.hasPos }
            .map { playerMoveC2SPacket -> Vec3(playerMoveC2SPacket.x, playerMoveC2SPacket.y, playerMoveC2SPacket.z) }

    val isLagging
        get() = packetQueue.isNotEmpty()

    private val espMode = modes(this, "Esp", 2) {
        arrayOf(
            BlinkEspBox(it, ::getEspData),
            BlinkEspModel(it, ::getEspData),
            BlinkEspWireframe(it, ::getEspData),
            BlinkEspNone(it),
        )
    }.apply {
        doNotIncludeAlways()
    }

    private val lineColor by color("Line", Color4b.LIQUID_BOUNCE)

    @Suppress("unused")
    private val flushHandler = handler<GameRenderTaskQueueEvent> {
        if (mc.connection?.connection?.isConnected != true) {
            packetQueue.clear()
            return@handler
        }

        if (fireEvent(null, TransferOrigin.OUTGOING) == Action.FLUSH) {
            flush(TransferOrigin.OUTGOING)
        }
    }

    @Suppress("unused")
    private val flushReceiveHandler = handler<TickPacketProcessEvent> {
        if (mc.connection?.connection?.isConnected != true) {
            packetQueue.clear()
            return@handler
        }

        if (fireEvent(null, TransferOrigin.INCOMING) == Action.FLUSH) {
            flush(TransferOrigin.INCOMING)
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
            flush(origin)
            return@handler
        }

        if (lagResult == Action.PASS) {
            return@handler
        }

        when (packet) {

            is ClientIntentionPacket, is ServerboundStatusRequestPacket, is ServerboundPingRequestPacket -> {
                return@handler
            }

            // Ignore message-related packets
            is ServerboundChatPacket, is ClientboundSystemChatPacket, is ServerboundChatCommandPacket -> {
                return@handler
            }

            // Flush on teleport or disconnect
            is ClientboundPlayerPositionPacket, is ClientboundDisconnectPacket -> {
                flush(origin)
                return@handler
            }

            // Ignore own hurt sounds
            is ClientboundSoundPacket if packet.sound.value() == SoundEvents.PLAYER_HURT -> {
                return@handler
            }

            // Flush on own death
            is ClientboundSetHealthPacket if packet.health <= 0 -> {
                flush(origin)
                return@handler
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

    private fun getEspData(): BlinkEspData? {
        val pos = positions.firstOrNull() ?: return null
        val rotation = RotationManager.actualServerRotation

        val perspectiveEvent = EventManager.callEvent(PerspectiveEvent(mc.options.cameraType))
        if (perspectiveEvent.perspective == CameraType.FIRST_PERSON) {
            return null
        }

        return BlinkEspData(player, pos, rotation)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        if (lineColor.a > 0) {
            renderEnvironmentForWorld(matrixStack) {
                drawLineStrip(
                    argb = lineColor.argb,
                    positions = positions.mapToArray { vec3d -> Vec3f(relativeToCamera(vec3d)) },
                )
            }
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

    fun flush(origin: TransferOrigin) {
        flush { it.origin == origin }
    }

    fun flush(count: Int) {
        // Take all packets until the counter of move packets reaches count and send them
        var counter = 0

        with(packetQueue.iterator()) {
            while (hasNext()) {
                val snapshot = next()
                val packet = snapshot.packet

                if (packet is ServerboundMovePlayerPacket && packet.hasPos) {
                    counter += 1
                }

                flushSnapshot(snapshot)
                remove()

                if (counter >= count) {
                    break
                }
            }
        }
    }

    fun cancel() {
        positions.firstOrNull()?.let { pos ->
            player.setPos(pos)
        }

        for (snapshot in packetQueue) {
            when (snapshot.packet) {
                is ServerboundMovePlayerPacket -> continue
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
        packetQueue.forEachIsInstance<T>(action)
    }

    private fun flushSnapshot(snapshot: PacketSnapshot) {
        when (snapshot.origin) {
            TransferOrigin.OUTGOING -> sendPacketSilently(snapshot.packet)
            TransferOrigin.INCOMING -> handlePacket(snapshot.packet)
        }
    }

    private fun fireEvent(packet: Packet<*>?, origin: TransferOrigin) =
        EventManager.callEvent(BlinkPacketEvent(packet, origin)).action

    enum class Action(val priority: Int) {
        FLUSH(0),
        PASS(1),
        QUEUE(2),
    }

}

@JvmRecord
data class PacketSnapshot(
    val packet: Packet<*>,
    val origin: TransferOrigin,
    val timestamp: Long
)

