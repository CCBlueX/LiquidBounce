/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.features.module.modules.render.XRay
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import java.io.File
import java.io.IOException

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class XRayConfig(file: File) : FileConfig(file)
{
	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun loadConfig()
	{
		val xRay = LiquidBounce.moduleManager[XRay::class.java] as XRay
		val jsonArray = JsonParser().parse(MiscUtils.createBufferedFileReader(file)).asJsonArray

		xRay.xrayBlocks.clear()

		for (jsonElement in jsonArray) try
		{
			val block = wrapper.functions.getBlockFromName(jsonElement.asString) ?: continue

			if (xRay.xrayBlocks.contains(block))
			{
				logger.error("[FileManager] Skipped xray block '{}' because the block is already added.", block.registryName)
				continue
			}

			xRay.xrayBlocks.add(block)
		}
		catch (throwable: Throwable)
		{
			logger.error("[FileManager] Failed to add block to xray.", throwable)
		}
	}

	/**
	 * Save config to file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun saveConfig()
	{
		val xRay = LiquidBounce.moduleManager[XRay::class.java] as XRay
		val jsonArray = JsonArray()

		for (block in xRay.xrayBlocks) jsonArray.add(FileManager.PRETTY_GSON.toJsonTree(wrapper.functions.getIdFromBlock(block)))

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonArray) + System.lineSeparator())
		writer.close()
	}
}
