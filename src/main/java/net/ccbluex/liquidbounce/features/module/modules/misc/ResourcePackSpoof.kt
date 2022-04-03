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
class ResourcePackSpoof : Module() {

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S48PacketResourcePackSend) {
            val packet = event.packet

            val url = packet.url
            val hash = packet.hash

            try {
                val scheme = URI(url).scheme
                val isLevelProtocol = "level" == scheme

                if ("http" != scheme && "https" != scheme && !isLevelProtocol)
                    throw URISyntaxException(url, "Wrong protocol")

                if (isLevelProtocol && (url.contains("..") || !url.endsWith("/resources.zip")))
                    throw URISyntaxException(url, "Invalid levelstorage resourcepack path")

                mc.netHandler.addToSendQueue(C19PacketResourcePackStatus(packet.hash,
                    C19PacketResourcePackStatus.Action.ACCEPTED))
                mc.netHandler.addToSendQueue(C19PacketResourcePackStatus(packet.hash,
                    C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED))
            } catch (e: URISyntaxException) {
                ClientUtils.getLogger().error("Failed to handle resource pack", e)
                mc.netHandler.addToSendQueue(C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD))
            }
        }
    }

}