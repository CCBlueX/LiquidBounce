/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.util.*

/**
 * @author CCBlueX
 */
class BungeeCordSpoof : MinecraftInstance(), Listenable
{
    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is CPacketHandshake && enabled && packet.asCPacketHandshake().requestedState.isHandshake)
        {
            val handshake = packet.asCPacketHandshake()

            handshake.ip = "${handshake.ip}\u0000${"%d.%d.%d.%d".format(getRandomIpPart(), getRandomIpPart(), getRandomIpPart(), getRandomIpPart())}\u0000${mc.session.playerId.replace("-", "")}"
        }
    }

    private fun getRandomIpPart(): String = RANDOM.nextInt(256).toString()

    override fun handleEvents(): Boolean = true

    companion object
    {
        private val RANDOM = Random()

        @JvmField
        var enabled = false
    }
}
