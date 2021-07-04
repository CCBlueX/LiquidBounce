/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.lwjgl.input.Keyboard
import java.io.File
import java.io.IOException

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class ModulesConfig(file: File) : FileConfig(file)
{
	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun loadConfig()
	{
		val jsonElement = JsonParser().parse(MiscUtils.createBufferedFileReader(file))

		if (jsonElement is JsonNull) return

		val moduleManager = LiquidBounce.moduleManager

		jsonElement.asJsonObject.entrySet().filter { it.value is JsonObject }.mapNotNull { (moduleName, jsonModule) -> (moduleManager.getModule(moduleName) ?: return@mapNotNull null) to jsonModule }.forEach { (module, jsonObj) ->
			val jsonModule = jsonObj as JsonObject

			module.state = jsonModule["State"].asBoolean

			if (jsonModule.has("KeyBind")) try
			{
				module.keyBinds = jsonModule["KeyBind"].asJsonArray.map { it.asInt }.filterTo(HashSet()) { it != Keyboard.KEY_NONE }
			}
			catch (e: IllegalStateException)
			{
				// Backward-compatibility

				val singleBind = jsonModule["KeyBind"].asInt
				if (singleBind != Keyboard.KEY_NONE) module.keyBinds = mutableSetOf(singleBind)
			}
			if (jsonModule.has("Array")) module.array = jsonModule["Array"].asBoolean
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
		val jsonObject = JsonObject()
		for (module in LiquidBounce.moduleManager.modules)
		{
			val jsonMod = JsonObject()
			jsonMod.addProperty("State", module.state)

			val keyBinds = module.keyBinds
			if (keyBinds.isNotEmpty())
			{
				val keybindArray = JsonArray()
				keyBinds.forEach { keybindArray.add(JsonPrimitive(it)) }
				jsonMod.add("KeyBind", keybindArray)
			}

			jsonMod.addProperty("Array", module.array)
			jsonObject.add(module.name, jsonMod)
		}

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonObject) + System.lineSeparator())
		writer.close()
	}
}
