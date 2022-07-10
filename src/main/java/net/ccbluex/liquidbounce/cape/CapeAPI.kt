/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.GuiDonatorCape
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.minecraft.client.renderer.IImageBuffer
import net.minecraft.client.renderer.ThreadDownloadImageData
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.io.File
import java.util.*

object CapeAPI : MinecraftInstance()
{
    // Cape Service
    private var capeService: CapeService? = null

    /**
     * Register cape service
     */
    fun registerCapeService()
    {
        val logger = ClientUtils.logger

        if (GuiDonatorCape.transferCode.startsWith("file:", true))
        {
            logger.info("[Donator Cape] Offline cape registered.")
            return
        }

        // Read cape infos from web
        val jsonObject = JsonParser().parse(HttpUtils["${LiquidBounce.CLIENT_CLOUD}/capes.json"]).asJsonObject
        val serviceType = jsonObject.get("serviceType").asString

        // Setup service type
        when (serviceType.toLowerCase())
        {
            "api" ->
            {
                val url = jsonObject.get("api").asJsonObject.get("url").asString

                capeService = ServiceAPI(url)
                logger.info("Registered $url as '$serviceType' service type.")
            }

            "list" ->
            {
                val users = HashMap<String, String>()

                for ((key, value) in jsonObject.get("users").asJsonObject.entrySet())
                {
                    users[key] = value.asString
                    logger.info("Loaded user cape for '$key'.")
                }

                capeService = ServiceList(users)
                logger.info("Registered '$serviceType' service type.")
            }
        }

        logger.info("Loaded.")
    }

    /**
     * Load cape of user with uuid
     *
     * @param uuid
     * @return cape info
     */
    fun loadCape(uuid: UUID): CapeInfo?
    {
        var url: String? = null
        var resourceLocation: ResourceLocation? = null

        if (GuiDonatorCape.transferCode.startsWith("file:", true))
        {
            if (uuid != mc.session.profile.id) return null

            val fileManagerDir = LiquidBounce.fileManager.dir

            var capeFile = File(fileManagerDir, GuiDonatorCape.transferCode.substring(5))
            ClientUtils.logger.info("[Donator Cape] Loading offline cape from file ${capeFile.toPath()}")

            resourceLocation = ResourceLocation("offline-capes/%s".format("$capeFile"))

            if (!capeFile.exists())
            {
                // Fallback strategy
                capeFile = File(fileManagerDir, GuiDonatorCape.transferCode.substring(5) + ".png")

                ClientUtils.logger.info("[Donator Cape] Loaded offline cape from file (using fallback strategy) ${capeFile.toPath()}")
                if (!capeFile.exists())
                {
                    ClientUtils.logger.info("[Donator Cape] Failed to load offline cape from file. File doesn't exists.")
                    return null
                }
            }

            url = capeFile.toURI().toURL().toString() // Convert file path to URL-string
        }

        // Get url of cape from cape service
        if (url == null) url = (capeService ?: return null).getCape(uuid) ?: return null

        // Load cape
        if (resourceLocation == null) resourceLocation = ResourceLocation("capes/%s.png".format("$uuid"))

        val capeInfo = CapeInfo(resourceLocation)
        val threadDownloadImageData = ThreadDownloadImageData(null, url, null, object : IImageBuffer
        {
            override fun parseUserSkin(image: BufferedImage?): BufferedImage? = image

            override fun skinAvailable()
            {
                capeInfo.isCapeAvailable = true
            }
        })

        mc.textureManager.loadTexture(resourceLocation, threadDownloadImageData)

        return capeInfo
    }

    /**
     * Check if cape service is available
     *
     * @return capeservice status
     */
    fun hasCapeService() = capeService != null || GuiDonatorCape.transferCode.startsWith("file:", true)
}

data class CapeInfo(val resourceLocation: ResourceLocation, var isCapeAvailable: Boolean = false)
