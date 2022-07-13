/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.network.play.client.C19PacketResourcePackStatus
import net.minecraft.network.play.server.S48PacketResourcePackSend
import java.net.URI
import java.net.URISyntaxException

@ModuleInfo(name = "ResourcePackSpoof", description = "Prevents servers from forcing you to download their resource pack.", category = ModuleCategory.MISC)
class ResourcePackSpoof : Module()
{

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S48PacketResourcePackSend)
        {
            val url = packet.url
            val hash = packet.hash

            val netHandler = mc.netHandler

            try
            {
                val scheme = URI(url).scheme.lowercase()
                val isLevelProtocol = scheme == "level"

                if ("http" != scheme && "https" != scheme && !isLevelProtocol) throw URISyntaxException(url, "Wrong protocol (only HTTP and HTTPS, LEVEL protocols are accepted)")

                if (isLevelProtocol && (url.contains("..") || !url.endsWith("/resources.zip"))) throw URISyntaxException(url, "Invalid levelstorage resourcepack path")

                netHandler.addToSendQueue(C19PacketResourcePackStatus(packet.hash, C19PacketResourcePackStatus.Action.ACCEPTED))
                netHandler.addToSendQueue(C19PacketResourcePackStatus(packet.hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED))
            }
            catch (e: URISyntaxException)
            {
                ClientUtils.logger.error("Failed to handle resource pack", e)
                netHandler.addToSendQueue(C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD))
            }
        }
    }
}
