/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.value.*

/**
 * Module command
 *
 * @author SenkJu
 */
class ModuleCommand(val module: Module, val values: List<Value<*>> = module.values) : Command(module.name.toLowerCase())
{

	init
	{
		require(values.isNotEmpty()) { "Values are empty!" }
	}

	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val valueNames = values.asSequence().filter { it !is FontValue }.joinToString(separator = "/") { it.name.toLowerCase() }

		val moduleName = module.name.toLowerCase()

		val thePlayer = mc.thePlayer
		if (args.size < 2)
		{
			chatSyntax(thePlayer, if (values.size == 1) "$moduleName $valueNames <value>" else "$moduleName <$valueNames>")
			return
		}

		val value = module.getValue(args[1])

		if (value == null)
		{
			chatSyntax(thePlayer, "$moduleName <$valueNames>")
			return
		}

		if (value is BoolValue)
		{
			val newValue = !value.get()
			value.set(newValue)

			chat(thePlayer, "\u00A77${module.name} \u00A78${args[1]}\u00A77 was toggled ${if (newValue) "\u00A78on\u00A77" else "\u00A78off\u00A77" + "."}")
			playEdit()
		}
		else
		{
			if (args.size < 3)
			{
				if (value is IntegerValue || value is FloatValue || value is TextValue) chatSyntax(thePlayer, "$moduleName ${args[1].toLowerCase()} <value>")
				else if (value is ListValue) chatSyntax(thePlayer, "$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}>")
				return
			}

			try
			{
				when (value)
				{
					is BlockValue ->
					{

						val id: Int = try
						{
							args[2].toInt()
						}
						catch (exception: NumberFormatException)
						{
							val tmpId = functions.getBlockFromName(args[2])?.let(functions::getIdFromBlock)

							if (tmpId == null || tmpId <= 0)
							{
								chat(thePlayer, "\u00A77Block \u00A78${args[2]}\u00A77 does not exist!")
								return
							}

							tmpId
						}

						value.set(id)
						chat(thePlayer, "\u00A77${module.name} \u00A78${args[1].toLowerCase()}\u00A77 was set to \u00A78${BlockUtils.getBlockName(id)}\u00A77.")
						playEdit()
						return
					}

					is IntegerValue -> value.set(args[2].toInt())
					is FloatValue -> value.set(args[2].toFloat())

					is ListValue ->
					{
						if (!value.contains(args[2]))
						{
							chatSyntax(thePlayer, "$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}>")
							return
						}

						value.set(args[2])
					}

					is TextValue -> value.set(StringUtils.toCompleteString(args, 2))
				}

				chat(thePlayer, "\u00A77${module.name} \u00A78${args[1]}\u00A77 was set to \u00A78${value.get()}\u00A77.")
				playEdit()
			}
			catch (e: NumberFormatException)
			{
				chat(thePlayer, "\u00A78${args[2]}\u00A77 cannot be converted to number!")
			}
		}
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> values.asSequence().filter { it !is FontValue && it.name.startsWith(args[0], true) }.map { it.name.toLowerCase() }.toList()

			2 ->
			{
				when (module.getValue(args[0]))
				{
					is BlockValue -> return functions.getItemRegistryKeys().asSequence().map { it.resourcePath.toLowerCase() }.filter { it.startsWith(args[1], true) }.toList()

					is ListValue ->
					{
						values.forEach { value ->
							if (!value.name.equals(args[0], true)) return@forEach
							if (value is ListValue) return@tabComplete value.values.filter { it.startsWith(args[1], true) }
						}
						return emptyList()
					}

					else -> emptyList()
				}
			}

			else -> emptyList()
		}
	}
}
