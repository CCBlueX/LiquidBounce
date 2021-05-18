/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.features.module.modules.render.XRay
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClientUtils
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
		val json = JsonParser().parse(MiscUtils.createBufferedFileReader(file))

		xRay.xrayBlocks.clear()

		val func = wrapper.functions

		(try
		{
			val jsonObject = json.asJsonObject

			xRay.orbfuscatorBypass = jsonObject["orbfuscatorBypass"].asBoolean

			jsonObject["blocks"].asJsonArray
		}
		catch (e: IllegalStateException)
		{
			json.asJsonArray // Backward-compatibility
		}).mapNotNull { func.getBlockFromName(it.asString) }.forEach { block ->
			try
			{
				if (xRay.xrayBlocks.contains(block))
				{
					ClientUtils.logger.warn("[FileManager] Skipped xray block '{}' because the block is already added.", block.registryName)
					return@forEach
				}

				xRay.xrayBlocks.add(block)
			}
			catch (throwable: Throwable)
			{
				ClientUtils.logger.error("[FileManager] Failed to add block to xray.", throwable)
			}
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

		val jsonObject = JsonObject()

		jsonObject.addProperty("orbfuscatorBypass", xRay.orbfuscatorBypass)

		val blocks = JsonArray()

		val func = wrapper.functions
		xRay.xrayBlocks.map(func::getIdFromBlock).forEach { blockID ->
			blocks.add(FileManager.PRETTY_GSON.toJsonTree(blockID))
		}

		jsonObject.add("blocks", blocks)

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonObject) + System.lineSeparator())
		writer.close()
	}
}
