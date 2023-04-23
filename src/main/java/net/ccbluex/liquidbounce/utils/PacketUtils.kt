/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.roundToInt


object PacketUtils : MinecraftInstance() {
    var S12PacketEntityVelocity.realMotionX: Float
        get() = motionX / 8000f
        set(value) {
            motionX = (value * 8000f).roundToInt()
        }

    var S12PacketEntityVelocity.realMotionY: Float
        get() = motionY / 8000f
        set(value) {
            motionX = (value * 8000f).roundToInt()
        }

    var S12PacketEntityVelocity.realMotionZ: Float
        get() = motionZ / 8000f
        set(value) {
            motionX = (value * 8000f).roundToInt()
        }

    fun sendPacket(packet: Packet<*>, triggerEvent: Boolean = true) {
        if (triggerEvent) {
            mc.netHandler?.addToSendQueue(packet)
            return
        }

        val netManager = mc.netHandler?.networkManager ?: return
        if (netManager.isChannelOpen) {
            netManager.flushOutboundQueue()
            netManager.dispatchPacket(packet, null)
        } else {
            netManager.readWriteLock.writeLock().lock()
            try {
                netManager.outboundPacketsQueue.add(
                    NetworkManager.InboundHandlerTuplePacketListener(packet, null)
                )
            } finally {
                netManager.readWriteLock.writeLock().unlock()
            }
        }
    }

    fun sendPackets(vararg packets: Packet<*>, triggerEvents: Boolean = true) =
        packets.forEach { sendPacket(it, triggerEvents) }

    fun handlePacket(packet: Packet<INetHandlerPlayClient>?) = packet?.processPacket(mc.netHandler)

    val Packet<*>.type
        get() = when (this.javaClass.simpleName[0]) {
            'C' -> PacketType.CLIENT
            'S' -> PacketType.SERVER
            else -> PacketType.UNKNOWN
        }

    enum class PacketType { CLIENT, SERVER, UNKNOWN }
}