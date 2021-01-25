/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.cape

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.render.WIImageBuffer
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.GuiDonatorCape
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.HashMap

object CapeAPI : MinecraftInstance()
{

	// Cape Service
	private var capeService: CapeService? = null

	/**
	 * Register cape service
	 */
	fun registerCapeService()
	{

		if (GuiDonatorCape.transferCode.startsWith("file:", true))
		{
			ClientUtils.getLogger().info("[Donator Cape] Offline cape registered.")
			return
		}

		// Read cape infos from web
		val jsonObject = JsonParser().parse(HttpUtils.get("${LiquidBounce.CLIENT_CLOUD}/capes.json")).asJsonObject
		val serviceType = jsonObject.get("serviceType").asString

		// Setup service type
		when (serviceType.toLowerCase())
		{
			"api" ->
			{
				val url = jsonObject.get("api").asJsonObject.get("url").asString

				capeService = ServiceAPI(url)
				ClientUtils.getLogger().info("Registered $url as '$serviceType' service type.")
			}

			"list" ->
			{
				val users = HashMap<String, String>()

				for ((key, value) in jsonObject.get("users").asJsonObject.entrySet())
				{
					users[key] = value.asString
					ClientUtils.getLogger().info("Loaded user cape for '$key'.")
				}

				capeService = ServiceList(users)
				ClientUtils.getLogger().info("Registered '$serviceType' service type.")
			}
		}

		ClientUtils.getLogger().info("Loaded.")
	}

	/**
	 * Load cape of user with uuid
	 *
	 * @param uuid
	 * @return cape info
	 */
	fun loadCape(uuid: UUID): CapeInfo?
	{
		if (GuiDonatorCape.transferCode.startsWith("file:", true))
		{
			if (uuid != mc.session.profile.id) return null
			val capeInfo = CapeInfo(classProvider.createResourceLocation(RandomUtils.randomNumber(128)))
			var capeFile = File(LiquidBounce.fileManager.dir, GuiDonatorCape.transferCode.substring(5))
			ClientUtils.getLogger().info("[Donator Cape] Loading offline cape from file ${capeFile.toPath()}")

			if (!capeFile.exists())
			{
				capeFile = File(LiquidBounce.fileManager.dir, GuiDonatorCape.transferCode.substring(5) + ".png")
				if (capeFile.exists())
				{
					WorkerUtils.workers.submit {
						val byteArrayInputStream = ByteArrayInputStream(Files.readAllBytes(capeFile.toPath()))
						val bufferedImage = ImageIO.read(byteArrayInputStream)
						byteArrayInputStream.close()

						mc.textureManager.loadTexture(capeInfo.resourceLocation, classProvider.createDynamicTexture(bufferedImage))

						ClientUtils.getLogger().info("[Donator Cape] Successfully loaded cape from file ${capeFile.toPath()}")

						capeInfo.isCapeAvailable = true
					}

					return capeInfo
				}

				ClientUtils.getLogger().info("[Donator Cape] Failed to load offline cape from file. File doesn't exists.")
				return capeInfo
			}

			val byteArrayInputStream = ByteArrayInputStream(Files.readAllBytes(capeFile.toPath()))
			val bufferedImage = ImageIO.read(byteArrayInputStream)
			byteArrayInputStream.close()

			mc.textureManager.loadTexture(capeInfo.resourceLocation, classProvider.createDynamicTexture(bufferedImage))

			ClientUtils.getLogger().info("[Donator Cape] Successfully loaded cape from file ${capeFile.toPath()}")

			capeInfo.isCapeAvailable = true
			return capeInfo
		}

		// Get url of cape from cape service
		val url = (capeService ?: return null).getCape(uuid) ?: return null

		// Load cape
		val resourceLocation = LiquidBounce.wrapper.classProvider.createResourceLocation("capes/%s.png".format("$uuid"))
		val capeInfo = CapeInfo(resourceLocation)
		val threadDownloadImageData = LiquidBounce.wrapper.classProvider.createThreadDownloadImageData(null, url, null, object : WIImageBuffer
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

data class CapeInfo(val resourceLocation: IResourceLocation, var isCapeAvailable: Boolean = false)
