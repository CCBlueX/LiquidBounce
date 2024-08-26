package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.entity.player.OtherClientPlayerEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.util.math.Vec3d

object BlinkUtils {

    val publicPacket: Packet<*>? = null
    val packets = mutableListOf<Packet<*>>()
    val packetsReceived = mutableListOf<Packet<*>>()
    private var fakePlayer: OtherClientPlayerEntity? = null
    val positions = mutableListOf<Vec3d>()
    val isBlinking
        get() = (packets.size + packetsReceived.size) > 0

    // TODO: Make better & more reliable BlinkUtils.
    fun blink(packet: Packet<*>, event: PacketEvent, sent: Boolean? = true, receive: Boolean? = true) {
        val player = mc.player ?: return

        if (event.isCancelled || !player.isAlive)
            return

        when (packet) {
            is HandshakeC2SPacket, is QueryRequestC2SPacket, is QueryPingC2SPacket, is ChatMessageS2CPacket, is ChatMessageC2SPacket -> {
                return
            }

            is PlaySoundIdS2CPacket -> {
                if (packet.sound == "game.player.hurt") {
                    return
                }
            }
        }


        // Don't blink on singleplayer
        if (mc.currentServerEntry != null) {
            if (sent == true && receive == false) {
                if (event.eventType == EventState.RECEIVE) {
                    synchronized(packetsReceived) {
                        PacketUtils.queuedPackets.addAll(packetsReceived)
                    }
                    packetsReceived.clear()
                }
                if (event.eventType == EventState.SEND) {
                    event.cancelEvent()
                    synchronized(packets) {
                        packets += packet
                    }
                    if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                        val packetPos = Vec3d(packet.x, packet.y, packet.z)
                        synchronized(positions) {
                            positions += packetPos
                        }
                    }
                }
            }
        }

        if (receive == true && sent == false) {
            if (event.eventType == EventState.RECEIVE && player.ticksAlive > 10) {
                event.cancelEvent()
                synchronized(packetsReceived) {
                    packetsReceived += packet
                }
            }
            if (event.eventType == EventState.SEND) {
                synchronized(packets) {
                    sendPackets(*packets.toTypedArray(), triggerEvents = false)
                }
                if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                    val packetPos = Vec3d(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
                packets.clear()
            }
        }

        // Don't blink on singleplayer
        if (mc.currentServerEntry != null) {
            if (sent == true && receive == true) {
                if (event.eventType == EventState.RECEIVE && player.ticksAlive > 10) {
                    event.cancelEvent()
                    synchronized(packetsReceived) {
                        packetsReceived += packet
                    }
                }
                if (event.eventType == EventState.SEND) {
                    event.cancelEvent()
                    synchronized(packets) {
                        packets += packet
                    }
                    if (packet is PlayerMoveC2SPacket && packet.changePosition) {
                        val packetPos = Vec3d(packet.x, packet.y, packet.z)
                        synchronized(positions) {
                            positions += packetPos
                        }
                        if (packet.changeLook) {
                            serverRotation = Rotation(packet.yaw, packet.pitch)
                        }
                    }
                }
            }
        }

        if (sent == false && receive == false)
            unblink()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // Clear packets on disconnect only
        if (event.worldClient == null) {
            clear()
        }
    }

    fun syncSent() {
        synchronized(packetsReceived) {
            PacketUtils.queuedPackets.addAll(packetsReceived)
            packetsReceived.clear()
        }
    }

    fun syncReceived() {
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
            packets.clear()
        }
    }

    fun cancel() {
        val player = mc.player ?: return
        val firstPosition = positions.firstOrNull() ?: return

        player.updatePosition(firstPosition.x, firstPosition.y, firstPosition.z)

        synchronized(packets) {
            val iterator = packets.iterator()
            while (iterator.hasNext()) {
                val packet = iterator.next()
                if (packet is PlayerMoveC2SPacket) {
                    iterator.remove()
                } else {
                    sendPacket(packet)
                    iterator.remove()
                }
            }
        }

        synchronized(positions) {
            positions.clear()
        }

        // Remove fake player
        fakePlayer?.let { entity: OtherClientPlayerEntity ->
            mc.world?.removeEntity(entity.entityId)
            fakePlayer = null
        }
    }

    fun unblink() {
        synchronized(packetsReceived) {
            PacketUtils.queuedPackets.addAll(packetsReceived)
        }
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }

        clear()

        // Remove fake player
        fakePlayer?.let { entity: OtherClientPlayerEntity ->
            mc.world?.removeEntity(entity.entityId)
            fakePlayer = null
        }
    }

    fun clear() {
        synchronized(packetsReceived) {
            packetsReceived.clear()
        }

        synchronized(packets) {
            packets.clear()
        }

        synchronized(positions) {
            positions.clear()
        }
    }

    fun addFakePlayer() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val faker = OtherClientPlayerEntity(world, player.gameProfile)

        faker.headYaw = player.headYaw
        faker.bodyYaw = player.bodyYaw
        faker.copyFrom(player, true)
        faker.headYaw = player.headYaw
        faker.inventory = player.inventory
        world.addEntity(RandomUtils.nextInt(Int.MIN_VALUE, Int.MAX_VALUE), faker)

        fakePlayer = faker

        // Add positions indicating a blink start
        // val pos = thePlayer.positionVector
        // positions += pos.addVector(.0, thePlayer.eyeHeight / 2.0, .0)
        // positions += pos
    }
}