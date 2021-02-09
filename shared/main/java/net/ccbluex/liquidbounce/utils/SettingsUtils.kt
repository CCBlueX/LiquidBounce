/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.Spammer
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard

/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
object SettingsUtils
{

	/**
	 * Execute settings [script]
	 */
	fun executeScript(script: String)
	{
		val thePlayer = LiquidBounce.wrapper.minecraft.thePlayer

		script.lines().asSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.forEachIndexed { index, s ->
			val args = s.split(" ").toTypedArray()

			if (args.size <= 1)
			{
				ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7cSyntax error at line '$index' in setting script.\n\u00A78\u00A7lLine: \u00A77$s")
				return@forEachIndexed
			}

			when (args[0].toLowerCase())
			{
				"chat" -> ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7e${translateAlternateColorCodes(StringUtils.toCompleteString(args, 1))}")
				"unchat" -> ClientUtils.displayChatMessage(thePlayer, translateAlternateColorCodes(StringUtils.toCompleteString(args, 1)))

				"load" ->
				{
					val urlRaw = StringUtils.toCompleteString(args, 1)
					val url = if (urlRaw.startsWith("http")) urlRaw
					else "${LiquidBounce.CLIENT_CLOUD}/settings/${urlRaw.toLowerCase()}"

					try
					{
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A77Loading settings from \u00A7a\u00A7l$url\u00A77...")
						executeScript(get(url))
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A77Loaded settings from \u00A7a\u00A7l$url\u00A77.")
					}
					catch (e: Exception)
					{
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A77Failed to load settings from \u00A7a\u00A7l$url\u00A77.")
					}
				}

				"targetplayer", "targetplayers" ->
				{
					EntityUtils.targetPlayer = args[1].equals("true", ignoreCase = true)
					ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${args[0]}\u00A77 set to \u00A7c\u00A7l${EntityUtils.targetPlayer}\u00A77.")
				}

				"targetmobs" ->
				{
					EntityUtils.targetMobs = args[1].equals("true", ignoreCase = true)
					ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${args[0]}\u00A77 set to \u00A7c\u00A7l${EntityUtils.targetMobs}\u00A77.")
				}

				"targetanimals" ->
				{
					EntityUtils.targetAnimals = args[1].equals("true", ignoreCase = true)
					ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${args[0]}\u00A77 set to \u00A7c\u00A7l${EntityUtils.targetAnimals}\u00A77.")
				}

				"targetinvisible" ->
				{
					EntityUtils.targetInvisible = args[1].equals("true", ignoreCase = true)
					ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${args[0]}\u00A77 set to \u00A7c\u00A7l${EntityUtils.targetInvisible}\u00A77.")
				}

				"targetdead" ->
				{
					EntityUtils.targetDead = args[1].equals("true", ignoreCase = true)
					ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${args[0]}\u00A77 set to \u00A7c\u00A7l${EntityUtils.targetDead}\u00A77.")
				}

				else ->
				{
					if (args.size != 3)
					{
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7cSyntax error at line '$index' in setting script.\n\u00A78\u00A7lLine: \u00A77$s")
						return@forEachIndexed
					}

					val moduleName = args[0]
					val valueName = args[1]
					val value = args[2]
					val module = LiquidBounce.moduleManager.getModule(moduleName)

					if (module == null)
					{
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7cModule \u00A7a\u00A7l$moduleName\u00A7c was not found!")
						return@forEachIndexed
					}

					if (valueName.equals("toggle", ignoreCase = true))
					{
						module.state = value.equals("true", ignoreCase = true)
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${module.name} \u00A77was toggled \u00A7c\u00A7l${if (module.state) "on" else "off"}\u00A77.")
						return@forEachIndexed
					}

					if (valueName.equals("bind", ignoreCase = true))
					{
						module.keyBind = Keyboard.getKeyIndex(value)
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${module.name} \u00A77was bound to \u00A7c\u00A7l${Keyboard.getKeyName(module.keyBind)}\u00A77.")
						return@forEachIndexed
					}

					val moduleValue = module.getValue(valueName)
					if (moduleValue == null)
					{
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7cValue \u00A7a\u00A7l$valueName\u00A7c don't found in module \u00A7a\u00A7l$moduleName\u00A7c.")
						return@forEachIndexed
					}

					try
					{
						when (moduleValue)
						{
							is BoolValue -> moduleValue.changeValue(value.toBoolean())
							is FloatValue -> moduleValue.changeValue(value.toFloat())
							is IntegerValue -> moduleValue.changeValue(value.toInt())
							is TextValue -> moduleValue.changeValue(value)
							is ListValue -> moduleValue.changeValue(value)
						}

						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${module.name}\u00A77 value \u00A78\u00A7l${moduleValue.name}\u00A77 set to \u00A7c\u00A7l$value\u00A77.")
					}
					catch (e: Exception)
					{
						ClientUtils.displayChatMessage(thePlayer, "\u00A77[\u00A73\u00A7lAutoSettings\u00A77] \u00A7a\u00A7l${e.javaClass.name}\u00A77(${e.message}) \u00A7cAn Exception occurred while setting \u00A7a\u00A7l$value\u00A7c to \u00A7a\u00A7l${moduleValue.name}\u00A7c in \u00A7a\u00A7l${module.name}\u00A7c.")
					}
				}
			}
		}

		FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
	}

	/**
	 * Generate settings script
	 */
	fun generateScript(values: Boolean, binds: Boolean, states: Boolean): String
	{
		val stringBuilder = StringBuilder()

		LiquidBounce.moduleManager.modules.asSequence().filter {
			it.category != ModuleCategory.RENDER && it !is NameProtect && it !is Spammer
		}.forEach {
			if (values) it.values.forEach { value -> stringBuilder.append(it.name).append(" ").append(value.name).append(" ").append(value.get()).append("\n") }

			if (states) stringBuilder.append(it.name).append(" toggle ").append(it.state).append("\n")

			if (binds) stringBuilder.append(it.name).append(" bind ").append(Keyboard.getKeyName(it.keyBind)).append("\n")
		}

		return "$stringBuilder"
	}
}
