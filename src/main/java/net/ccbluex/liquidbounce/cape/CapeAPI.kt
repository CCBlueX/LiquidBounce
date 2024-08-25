/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import net.ccbluex.liquidbounce.file.FileManager.dir
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.render.IImageBuffer
import net.minecraft.client.render.ThreadDownloadImageData
import net.minecraft.util.Identifier
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER

object CapeAPI : MinecraftInstance() {

    private val capesCache = File(dir, "capes").apply {
        mkdir()
    }

    /**
     * Load cape of user with uuid
     *
     * @param uuid
     * @return cape info
     */
    fun loadCape(uuid: UUID, success: (CapeInfo) -> Unit) {
        CapeService.refreshCapeCarriers {
            runCatching {
                // Get URL of cape from cape service
                val (name, url) = CapeService.getCapeDownload(uuid) ?: return@refreshCapeCarriers

                // Load cape
                val Identifier = Identifier("capes/$name.png")
                val cacheFile = File(capesCache, "$name.png")
                val capeInfo = CapeInfo(Identifier)
                val threadDownloadImageData = ThreadDownloadImageData(cacheFile, url, null, object : IImageBuffer {

                    override fun parseUserSkin(image: BufferedImage?) = image

                    override fun skinAvailable() {
                        capeInfo.isCapeAvailable = true
                    }

                })

                mc.textureManager.loadTexture(Identifier, threadDownloadImageData)
                success(capeInfo)
            }.onFailure {
                LOGGER.error("Failed to load cape for UUID: $uuid", it)
            }
        }
    }
}

data class CapeInfo(val Identifier: Identifier, var isCapeAvailable: Boolean = false)