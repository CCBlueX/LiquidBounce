package net.ccbluex.liquidbounce.features.fakelag

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleFakeLag
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleBugUp
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.Hypixel
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.RigidPlayerSimulation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.*
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
        return ModuleBlink.enabled || ModuleBugUp.shouldLag || ModuleFakeLag.shouldLag(packet) || Hypixel.shouldLag()
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
        }

        if (event.origin == TransferOrigin.SEND) {
            event.cancelEvent()
            synchronized(packetQueue) {
                packetQueue.add(DelayData(packet, System.currentTimeMillis()))
            }

            if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                synchronized(positions) {
                    positions.add(PositionData(Vec3d(packet.x, packet.y, packet.z),
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
        val (x, y, z) = firstPosition() ?: return

        player.setPosition(x, y, z)

        player.prevX = x
        player.prevY = y
        player.prevZ = z
        player.lastRenderX = x
        player.lastRenderY = y
        player.lastRenderZ = z

        player.velocity = Vec3d.ZERO
        player.updatePositionAndAngles(x, y, z, player.yaw, player.pitch)

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
                    drawLineStrip(*positions.map { Vec3(it.vec) }.toTypedArray())
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

    fun firstPosition(): Vec3d? {
        synchronized(positions) {
            return positions.firstOrNull()?.vec
        }
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

data class PositionData(val vec: Vec3d, val delay: Long)
