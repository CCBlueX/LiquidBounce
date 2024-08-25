/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.minecraft.network.packet.c2s.play.C19PacketResourcePackStatus
import net.minecraft.network.packet.c2s.play.C19PacketResourcePackStatus.Action.*
import net.minecraft.network.packet.s2c.play.S48PacketResourcePackSend
import java.net.URI
import java.net.URISyntaxException

object ResourcePackSpoof : Module("ResourcePackSpoof", Category.MISC, gameDetecting = false, hideModule = false) {

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

                if (isLevelProtocol && (".." in url || !url.endsWith("/resources.zip")))
                    throw URISyntaxException(url, "Invalid levelstorage resourcepack path")

                sendPackets(
                    C19PacketResourcePackStatus(packet.hash, ACCEPTED),
                    C19PacketResourcePackStatus(packet.hash, SUCCESSFULLY_LOADED)
                )
            } catch (e: URISyntaxException) {
                LOGGER.error("Failed to handle resource pack", e)
                sendPacket(C19PacketResourcePackStatus(hash, FAILED_DOWNLOAD))
            }
        }
    }

}