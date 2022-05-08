/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.MinecraftInstance

import net.minecraft.client.renderer.IImageBuffer
import net.minecraft.client.renderer.ThreadDownloadImageData
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.io.File
import java.util.*

object CapeAPI : MinecraftInstance() {

    private val capesCache = File(LiquidBounce.fileManager.dir, "capes").apply {
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
            // Get url of cape from cape service
            val (name, url) = CapeService.getCapeDownload(uuid) ?: return@refreshCapeCarriers

            // Load cape
            val resourceLocation = ResourceLocation("capes/%s.png".format(name))
            val cacheFile = File(capesCache, "%s.png".format(name))
            val capeInfo = CapeInfo(resourceLocation)
            val threadDownloadImageData = ThreadDownloadImageData(cacheFile, url, null, object : IImageBuffer {

                override fun parseUserSkin(image: BufferedImage?): BufferedImage? {
                    return image
                }

                override fun skinAvailable() {
                    capeInfo.isCapeAvailable = true
                }

            })

            mc.textureManager.loadTexture(resourceLocation, threadDownloadImageData)
            success(capeInfo)
        }
    }

}

data class CapeInfo(val resourceLocation: ResourceLocation, var isCapeAvailable: Boolean = false)