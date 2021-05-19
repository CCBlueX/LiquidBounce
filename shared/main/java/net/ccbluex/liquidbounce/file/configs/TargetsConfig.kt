/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class TargetsConfig(file: File) : FileConfig(file)
{
	val targets: MutableSet<String> = HashSet()

	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun loadConfig()
	{
		clearTargets()

		val jsonElement = JsonParser().parse(MiscUtils.createBufferedFileReader(file))

		if (jsonElement is JsonNull) return

		jsonElement.asJsonArray.map { it.asString }.forEach { addTarget(it) }
	}

	/**
	 * Save config to file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun saveConfig()
	{
		val jsonArray = JsonArray()

		for (target in targets) jsonArray.add(target)

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonArray) + System.lineSeparator())
		writer.close()
	}
	/**
	 * Add target to config
	 *
	 * @param  playerName
	 * of target
	 * @param  alias
	 * of target
	 * @return            of successfully added target
	 */
	/**
	 * Add target to config
	 *
	 * @param  playerName
	 * of target
	 * @return            of successfully added target
	 */
	fun addTarget(playerName: String): Boolean
	{
		if (isTarget(playerName)) return false

		targets.add(playerName)

		return true
	}

	/**
	 * Remove target from config
	 *
	 * @param playerName
	 * of target
	 */
	fun removeTarget(playerName: String): Boolean
	{
		if (!isTarget(playerName)) return false

		targets.removeIf { it == playerName }

		return true
	}

	/**
	 * Check is target
	 *
	 * @param  playerName
	 * of target
	 * @return            is target
	 */
	fun isTarget(playerName: String): Boolean = targets.any { it == playerName }

	/**
	 * Clear all targets from config
	 */
	fun clearTargets()
	{
		targets.clear()
	}
}
