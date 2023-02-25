/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C17PacketCustomPayload

object ClientFixes : MinecraftInstance(), Listenable {

    @JvmField
    var fmlFixesEnabled = true
    @JvmField
    var blockFML = true
    @JvmField
    var blockProxyPacket = true
    @JvmField
    var blockPayloadPackets = true
    @JvmField
    var blockResourcePackExploit = true

    @JvmField
    var clientBrand = "Vanilla"

    @JvmField
    var possibleBrands = arrayOf(
        "Vanilla",
        "Forge",
        "LunarClient",
        "CheatBreaker"
    )

    @EventTarget
    fun onPacket(event: PacketEvent) = runCatching {
        val packet = event.packet

        if (mc.isIntegratedServerRunning || !fmlFixesEnabled) {
            return@runCatching
        }

        when {
            blockProxyPacket && packet.javaClass.name == "net.minecraftforge.fml.common.network.internal.FMLProxyPacket" -> {
                event.cancelEvent()
                return@runCatching
            }

            packet is C17PacketCustomPayload -> {
                if (blockPayloadPackets && !packet.channelName.startsWith("MC|")) {
                    event.cancelEvent()
                } else if (packet.channelName.equals("MC|Brand", ignoreCase = true)) {
                    packet.data = PacketBuffer(Unpooled.buffer()).writeString(when (clientBrand) {
                        "Vanilla" -> "vanilla"
                        "LunarClient" -> "lunarclient:" + RandomUtils.randomString(7)
                        "CheatBreaker" -> "CB"
                        else -> {
                            // do nothing
                            return@runCatching
                        }
                    })
                }
            }
        }
    }.onFailure {
        ClientUtils.getLogger().error("Failed to handle packet on client fixes.", it)
    }

    override fun handleEvents() = true

}