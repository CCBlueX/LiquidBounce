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

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.BlinkManager.Action
import net.ccbluex.liquidbounce.features.blink.PacketSnapshot
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.network.handlePacket
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import org.lwjgl.glfw.GLFW
import java.util.concurrent.ConcurrentLinkedQueue

object LagLongJump : Mode("Lag") {
    override val parent: ModeValueGroup<*>
        get() = ModuleLongJump.mode

    private var shouldLag = false
    private var shouldRelease = false
    private var jumpKeyTimestamp = 0L
    private var velocityAmount = 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet.isLocalPlayerVelocity(true)) {
            shouldLag = true
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (!shouldLag || event.origin != TransferOrigin.INCOMING) {
            return@handler
        } else if (event.packet.isLocalPlayerVelocity(true)) {
            velocityAmount += 1
            chat(translation("liquidbounce.module.longJump.messages.lagModeReceivePacket", velocityAmount))
        }

        event.action = Action.QUEUE
    }

    @Suppress("unused")
    private val keyboardKeyHandler = suspendHandler<KeyboardKeyEvent> { event ->
        if (event.action == GLFW.GLFW_PRESS && event.key == mc.options.keyJump.key) {
            val currentTimeMillis = System.currentTimeMillis()
            if (currentTimeMillis - jumpKeyTimestamp < 400 && velocityAmount > 0) {
                shouldRelease = true
            }
            jumpKeyTimestamp = currentTimeMillis
        }
    }

    @Suppress("unused")
    private val tickPacketProcessHandler = sequenceHandler<TickPacketProcessEvent> { event ->
        if (shouldRelease) {
            var splitPacketQueue = splitByLocalVelocityPacket(BlinkManager.packetQueue)
            BlinkManager.packetQueue.removeIf { packetSnapshot ->
                if (splitPacketQueue.first().contains(packetSnapshot)
                    && packetSnapshot.origin == TransferOrigin.INCOMING) {
                    handlePacket(packetSnapshot.packet)
                    true
                } else {
                    false
                }
            }

            shouldRelease = false
            velocityAmount -= 1
            if (velocityAmount == 0) {
                shouldLag = false
                ModuleLongJump.enabled = false
            }
            chat(translation("liquidbounce.module.longJump.messages.lagModeReleasePacket", velocityAmount))
        }
    }


    fun splitByLocalVelocityPacket(queue: ConcurrentLinkedQueue<PacketSnapshot>)
        : ArrayList<ConcurrentLinkedQueue<PacketSnapshot>> {
        val result = ArrayList<ConcurrentLinkedQueue<PacketSnapshot>>()
        val buffer = ArrayList<PacketSnapshot>()

        queue.forEach { item ->
            buffer.add(item)
            if (item.packet.isLocalPlayerVelocity(true)) {
                result.add(ConcurrentLinkedQueue(buffer))
                buffer.clear()
            }
        }

        if (buffer.isNotEmpty()) {
            buffer.forEach { packetSnapshot ->
                result.last().offer(packetSnapshot)
            }
        }

        return result
    }

    override fun enable() {
        shouldLag = false
        velocityAmount = 0
        chat(regular(translation("liquidbounce.module.longJump.messages.lagModeTip")))
    }

    override fun disable() {
        shouldLag = false
        velocityAmount = 0
        BlinkManager.flush(TransferOrigin.INCOMING)
    }
}
