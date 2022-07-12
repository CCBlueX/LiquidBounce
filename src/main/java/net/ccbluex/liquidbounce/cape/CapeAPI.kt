/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.renderer.IImageBuffer
import net.minecraft.client.renderer.ThreadDownloadImageData
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

object CapeAPI : MinecraftInstance()
{
    private val capesCache: File = File(LiquidBounce.fileManager.dir, "capes").apply(File::mkdir)

    /**
     * Load cape of user with uuid
     *
     * @param uuid
     * @return cape info
     */
    fun loadCape(uuid: UUID, success: (CapeInfo) -> Unit)
    {
        if (GuiDonatorCape.transferCode.startsWith("file:", true))
        {
            val fileManagerDir = LiquidBounce.fileManager.dir
            var capeFile = File(fileManagerDir, GuiDonatorCape.transferCode.substring(5))
            val resourceLocation = ResourceLocation("offline-capes/%s".format("$capeFile"))
            ClientUtils.logger.info("[Offline Cape] Loading offline cape from file ${capeFile.toPath()}")

            if (!capeFile.exists())
            {
                // Fallback strategy
                capeFile = File(fileManagerDir, GuiDonatorCape.transferCode.substring(5) + ".png")

                ClientUtils.logger.info("[Offline Cape] Loaded offline cape from file (using fallback strategy) ${capeFile.toPath()}")
                if (!capeFile.exists())
                {
                    ClientUtils.logger.info("[Offline Cape] Failed to load offline cape from file. File doesn't exists.")
                    return
                }
            }

            val bufferedImage = try
            {
                ImageIO.read(capeFile)
            }
            catch (e: IOException)
            {
                ClientUtils.logger.info("[Offline Cape] IOException occurred while reading image file.", e)
                return
            }

            mc.textureManager.loadTexture(resourceLocation, DynamicTexture(bufferedImage))
            success(CapeInfo(resourceLocation).apply { isCapeAvailable = true })
        }

        CapeService.refreshCapeCarriers {
            // Get url of cape from cape service
            val (name, url) = CapeService.getCapeDownload(uuid) ?: return@refreshCapeCarriers

            // Load cape
            val resourceLocation = ResourceLocation("capes/%s.png".format(name))
            val cacheFile = File(capesCache, "%s.png".format(name))
            val capeInfo = CapeInfo(resourceLocation)
            println(url)
            val threadDownloadImageData = ThreadDownloadImageData(cacheFile, url, null, object : IImageBuffer
            {

                override fun parseUserSkin(image: BufferedImage?): BufferedImage?
                {
                    return image
                }

                override fun skinAvailable()
                {
                    capeInfo.isCapeAvailable = true
                }
            })

            mc.textureManager.loadTexture(resourceLocation, threadDownloadImageData)
            success(capeInfo)
        }
    }
}

data class CapeInfo(val resourceLocation: ResourceLocation, var isCapeAvailable: Boolean = false)
