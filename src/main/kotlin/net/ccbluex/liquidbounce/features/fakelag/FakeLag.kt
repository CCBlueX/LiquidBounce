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
package net.ccbluex.liquidbounce.features.fakelag

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleFakeLag
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleClickTp
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.specific.FlyNcpClip
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldBlinkFeature
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.RigidPlayerSimulation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d

/**
 * FakeLag
 *
 * Simulates lag by holding back packets.
 */
object FakeLag : Listenable {

    /**
     * Whether we are lagging.
     */
    val isLagging
        get() = packetQueue.isNotEmpty() || positions.isNotEmpty()

    /**
     * Whether we should lag.
     * Implement your module here if you want to enable lag.
     */
    private fun shouldLag(packet: Packet<*>?): Boolean {
        return ModuleBlink.enabled || ModuleAntiVoid.needsArtificialLag || ModuleFakeLag.shouldLag(packet)
            || NoFallBlink.shouldLag() || ModuleInventoryMove.Blink.shouldLag() || ModuleClickTp.requiresLag
            || FlyNcpClip.shouldLag
            || ScaffoldBlinkFeature.shouldBlink
    }

    val packetQueue = LinkedHashSet<DelayData>()
    val positions = LinkedHashSet<PositionData>()

    val repeatable = repeatable {
        if (!inGame) {
            return@repeatable
        }

        if (!shouldLag(null)) {
            flush()
        }
    }

    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        // Ignore packets that are already cancelled, as they are already handled
        if (event.isCancelled || !inGame) {
            return@handler
        }

        val packet = event.packet

        // If we shouldn't lag, don't do anything
        if (!shouldLag(packet)) {
            flush()
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
                flush()
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
                    flush()
                    return@handler
                }
            }

            // Prevent lagging inventory actions if inventory move blink is enabled
            is ClickSlotC2SPacket, is ButtonClickC2SPacket, is CreativeInventoryActionC2SPacket,
                is SlotChangedStateC2SPacket -> {
                if (ModuleInventoryMove.Blink.shouldLag()) {
                    return@handler
                }
            }

        }

        if (event.origin == TransferOrigin.SEND) {
            event.cancelEvent()
            synchronized(packetQueue) {
                packetQueue.add(DelayData(packet, System.currentTimeMillis()))
            }

            if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                synchronized(positions) {
                    positions.add(PositionData(Vec3d(packet.x, packet.y, packet.z), player.velocity,
                        System.currentTimeMillis()))
                }
            }
        }
    }

    val worldChangeHandler = handler<WorldChangeEvent> {
        // Clear packets on disconnect
        if (it.world == null) {
            clear()
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        // Use LiquidBounce accent color
        val color = Color4b(0x00, 0x80, 0xFF, 0xFF)

        drawStrip(matrixStack, color)
    }

    fun flush() {
        synchronized(packetQueue) {
            packetQueue.removeIf {
                sendPacketSilently(it.packet)
                true
            }
        }

        synchronized(positions) {
            positions.clear()
        }
    }

    fun flush(count: Int) {
        synchronized(packetQueue) {
            // Take all packets until the counter of move packets reaches count and send them
            var counter = 0

            for (packetData in packetQueue.iterator()) {
                val packet = packetData.packet

                if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                    counter += 1
                }

                sendPacketSilently(packet)
                packetQueue.remove(packetData)

                if (counter >= count) {
                    break
                }
            }
        }

        synchronized(positions) {
            positions.removeAll(positions.take(count).toSet())
        }
    }

    fun cancel() {
        val (playerPosition, velocity, _) = firstPosition() ?: return

        player.setPosition(playerPosition)
        player.velocity = velocity

        synchronized(packetQueue) {
            packetQueue.removeIf {
                if (it.packet is PlayerMoveC2SPacket) {
                    return@removeIf true
                }

                sendPacketSilently(it.packet)
                true
            }
        }

        synchronized(positions) {
            positions.clear()
        }
    }

    fun clear() {
        synchronized(packetQueue) {
            packetQueue.clear()
        }

        synchronized(positions) {
            positions.clear()
        }
    }

    fun drawStrip(matrixStack: MatrixStack, color: Color4b) {
        synchronized(positions) {
            renderEnvironmentForWorld(matrixStack) {
                withColor(color) {
                    @Suppress("SpreadOperator")
                    drawLineStrip(*positions.map { Vec3(relativeToCamera(it.vec)) }.toTypedArray())
                }
            }
        }
    }

    fun isAboveTime(delay: Long): Boolean {
        synchronized(packetQueue) {
            val entryPacketTime = (packetQueue.firstOrNull()?.delay ?: return false)
            return System.currentTimeMillis() - entryPacketTime >= delay
        }
    }

    fun entryPacket(): DelayData? {
        synchronized(packetQueue) {
            return packetQueue.firstOrNull()
        }
    }

    fun firstPosition(): PositionData? {
        synchronized(positions) {
            return positions.firstOrNull()
        }
    }

    inline fun <reified T> rewrite(action: (T) -> Unit) {
        synchronized(packetQueue) {
            packetQueue
                .filterIsInstance<T>()
                .forEach(action)
        }
    }

    inline fun <reified T> rewriteAndFlush(action: (T) -> Unit) {
        rewrite(action)
        flush()
    }

    data class EvadingPacket(
        val idx: Int,
        /**
         * Ticks until impact. Null if evaded
         */
        val ticksToImpact: Int?
    )

    /**
     * Returns the index of the first position packet that avoids all arrows in the next X seconds
     */
    fun findAvoidingArrowPosition(): EvadingPacket? {
        var packetIndex = 0

        var lastPosition: Vec3d? = null

        var bestPacketPosition: Vec3d? = null
        var bestPacketIdx: Int? = null
        var bestTimeToImpact = 0

        for ((packetPosition, _) in this.positions) {
            packetIndex += 1

            // Process packets only if they are at least some distance away from each other
            if (lastPosition != null) {
                if (lastPosition.squaredDistanceTo(packetPosition) < 0.9 * 0.9) {
                    continue
                }
            }

            lastPosition = packetPosition

            val inflictedHit = getInflictedHit(packetPosition)

            if (inflictedHit == null)
                return EvadingPacket(packetIndex - 1, null)
            else if (inflictedHit.tickDelta > bestTimeToImpact) {
                bestTimeToImpact = inflictedHit.tickDelta
                bestPacketIdx = packetIndex - 1
                bestPacketPosition = packetPosition
            }
        }

        // If the evading packet is less than one player hitbox away from the current position, we should rather
        // call the evasion a failure
        if (bestPacketIdx != null && bestPacketPosition!!.squaredDistanceTo(lastPosition!!) > 0.9) {
            return EvadingPacket(bestPacketIdx, bestTimeToImpact)
        }

        return null
    }

    fun getInflictedHit(pos: Vec3d): ModuleAutoDodge.HitInfo? {
        val arrows = ModuleAutoDodge.findFlyingArrows(world)
        val playerSimulation = RigidPlayerSimulation(pos)

        return ModuleAutoDodge.getInflictedHits(playerSimulation, arrows, maxTicks = 40) { }
    }

}

data class DelayData(val packet: Packet<*>, val delay: Long)

data class PositionData(val vec: Vec3d, val velocity: Vec3d, val delay: Long)
