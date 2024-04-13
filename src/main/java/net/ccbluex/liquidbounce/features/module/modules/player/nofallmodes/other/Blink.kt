package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.blinkDuringFallDistanceMax
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.blinkDuringFallDistanceMin
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.blinkOnGroundTicks
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.spoofDistance
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.util.Vec3

object Blink : NoFallMode("Blink") {

    private var managedToReset = false
    private var waitUntilGround = true
    private var onGroundSince = 1337

    private val packets = mutableListOf<Packet<*>>()
    private val packetsReceived = mutableListOf<Packet<*>>()
    private val positions = mutableListOf<Vec3>()

    override fun onEnable() {
        onGroundSince = 1337
    }

    @EventTarget
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (packet.isOnGround) {
                onGroundSince++
                managedToReset = false
                waitUntilGround = false
            } else {
                if (waitUntilGround) {
                    return
                }

                val fallDistance = mc.thePlayer?.fallDistance ?: 0F

                if (fallDistance >= (blinkDuringFallDistanceMin.get() + spoofDistance)
                    && fallDistance <= blinkDuringFallDistanceMax.get()
                ) {
                    managedToReset = false
                    onGroundSince = 0
                    blink(event)
                } else {
                    if (fallDistance > blinkDuringFallDistanceMax.get() && !managedToReset) {
                        unblink()
                        packet.onGround = false
                        managedToReset = true
                    }
                }
            }
        }
    }

    @EventTarget
    override fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isDead || mc.thePlayer.ticksExisted <= 10) {
            unblink()
        }
    }

    @EventTarget
    override fun onUpdate() {
        if ((mc.thePlayer.fallDistance in blinkDuringFallDistanceMin.range || onGroundSince <= blinkOnGroundTicks) && !managedToReset) {
            unblink()
            mc.thePlayer.onGround = false
        }
    }

    private fun blink(event: PacketEvent) {
        val packet = event.packet

        if (event.isCancelled)
            return

        when (packet) {
            is C00Handshake, is C00PacketServerQuery, is C01PacketPing, is S02PacketChat, is S40PacketDisconnect -> {
                return
            }
        }

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
                packet.onGround = true
            }
        }
    }

    private fun unblink() {
        synchronized(packetsReceived) {
            PacketUtils.queuedPackets.addAll(packetsReceived)
        }
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }

        packets.clear()
        packetsReceived.clear()
        positions.clear()
    }
}