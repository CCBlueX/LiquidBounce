package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.util.Vec3

object BlinkUtils {

    val publicPacket: Packet<*>? = null
    val packets = mutableListOf<Packet<*>>()
    val packetsReceived = mutableListOf<Packet<*>>()
    private var fakePlayer: EntityOtherPlayerMP? = null
    val positions = mutableListOf<Vec3>()
    val isBlinking
        get() = (packets.size + packetsReceived.size) > 0

    // TODO: Make better & more reliable BlinkUtils.
    fun blink(packet: Packet<*>, event: PacketEvent, sent: Boolean? = true, receive: Boolean? = true) {
        if (mc.thePlayer == null || mc.thePlayer.isDead)
            return

        if (event.isCancelled)
            return

        when (packet) {
            is C00Handshake, is C00PacketServerQuery, is C01PacketPing, is S02PacketChat, is S40PacketDisconnect -> {
                return
            }
        }

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
                if (packet is C03PacketPlayer && packet.isMoving) {
                    val packetPos = Vec3(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
            }
        }

        if (receive == true && sent == false) {
            if (event.eventType == EventState.RECEIVE && mc.thePlayer.ticksExisted > 10) {
                event.cancelEvent()
                synchronized(packetsReceived) {
                    packetsReceived += packet
                }
            }
            if (event.eventType == EventState.SEND) {
                synchronized(packets) {
                    sendPackets(*packets.toTypedArray(), triggerEvents = false)
                }
                packets.clear()
            }
        }

        if (sent == true && receive == true) {
            if (event.eventType == EventState.RECEIVE && mc.thePlayer.ticksExisted > 10) {
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
                if (packet is C03PacketPlayer && packet.isMoving) {
                    val packetPos = Vec3(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                    if (packet.rotating) {
                        serverRotation = Rotation(packet.yaw, packet.pitch)
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
            packets.clear()
            packetsReceived.clear()
            positions.clear()
        }
    }

    fun syncSent() {
        synchronized(packetsReceived) {
            PacketUtils.queuedPackets.addAll(packetsReceived)
        }
        packetsReceived.clear()
    }

    fun syncReceived() {
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }
        packets.clear()
    }

    fun unblink() {
        synchronized(packetsReceived) {
            PacketUtils.queuedPackets.addAll(packetsReceived)
        }
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }

        packets.clear()
        packetsReceived.clear()
        positions.clear()

        // Remove fake player
        fakePlayer?.let {
            mc.theWorld?.removeEntityFromWorld(it.entityId)
            fakePlayer = null
        }
    }

    fun addFakePlayer() {
        val player = mc.thePlayer ?: return

        val faker = EntityOtherPlayerMP(mc.theWorld, player.gameProfile)

        faker.rotationYawHead = player.rotationYawHead
        faker.renderYawOffset = player.renderYawOffset
        faker.copyLocationAndAnglesFrom(player)
        faker.rotationYawHead = player.rotationYawHead
        faker.inventory = player.inventory
        mc.theWorld.addEntityToWorld(-1337, faker)

        fakePlayer = faker

        // Add positions indicating a blink start
        // val pos = player.positionVector
        // positions += pos.addVector(.0, player.eyeHeight / 2.0, .0)
        // positions += pos
    }
}