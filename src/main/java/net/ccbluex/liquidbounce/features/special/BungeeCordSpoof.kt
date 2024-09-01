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
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.minecraft.network.EnumConnectionState
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket

object BungeeCordSpoof : MinecraftInstance(), Listenable {
    var enabled = false

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is HandshakeC2SPacket && packet.requestedState == EnumConnectionState.LOGIN) {
            packet.ip = packet.ip + "\u0000" + String.format(
                "{0}.{1}.{2}.{3}", getRandomIpPart(), getRandomIpPart(), getRandomIpPart(), getRandomIpPart()
            ) + "\u0000" + mc.session.playerID.replace("-", "")
        }
    }

    private fun getRandomIpPart() = nextInt(endExclusive = 256).toString()

    override fun handleEvents()= enabled
}