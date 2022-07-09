/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketResourcePackStatus
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.net.URI
import java.net.URISyntaxException

@ModuleInfo(name = "ResourcePackSpoof", description = "Prevents servers from forcing you to download their resource pack.", category = ModuleCategory.MISC)
class ResourcePackSpoof : Module()
{

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (classProvider.isSPacketResourcePackSend(event.packet))
        {
            val packet = event.packet.asSPacketResourcePackSend()

            val url = packet.url
            val hash = packet.hash

            val netHandler = mc.netHandler

            try
            {
                val scheme = URI(url).scheme.toLowerCase()
                val isLevelProtocol = scheme == "level"

                if ("http" != scheme && "https" != scheme && !isLevelProtocol) throw URISyntaxException(url, "Wrong protocol (only HTTP and HTTPS, LEVEL protocols are accepted)")

                if (isLevelProtocol && (url.contains("..") || !url.endsWith("/resources.zip"))) throw URISyntaxException(url, "Invalid levelstorage resourcepack path")

                netHandler.addToSendQueue(classProvider.createCPacketResourcePackStatus(packet.hash, ICPacketResourcePackStatus.WAction.ACCEPTED))
                netHandler.addToSendQueue(classProvider.createCPacketResourcePackStatus(packet.hash, ICPacketResourcePackStatus.WAction.SUCCESSFULLY_LOADED))
            }
            catch (e: URISyntaxException)
            {
                ClientUtils.logger.error("Failed to handle resource pack", e)
                netHandler.addToSendQueue(classProvider.createCPacketResourcePackStatus(hash, ICPacketResourcePackStatus.WAction.FAILED_DOWNLOAD))
            }
        }
    }
}
