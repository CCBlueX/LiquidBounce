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
class ModuleCommand(val module: Module, val values: List<AbstractValue> = module.flatValues) : Command(module.name.toLowerCase())
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
		val valueNames = lazy(LazyThreadSafetyMode.NONE) {
			values.map {
				val newValueParameters = when (it)
				{
					is BoolValue -> "<on/off>"
					is RangeValue<*> -> "<min/max> <new value>"
					else -> "<new value>"
				}
				"${it.name.toLowerCase()} $newValueParameters"
			}
		}

		val moduleName = module.name.toLowerCase()

		val thePlayer = mc.thePlayer
		if (args.size < 2)
		{
			if (values.size == 1) chatSyntax(thePlayer, "$moduleName ${valueNames.value[0]}")
			else
			{
				chatSyntax(thePlayer, "$moduleName <value name>")
				chat(thePlayer, "Available value names:")
				valueNames.value.forEach { name -> chat(thePlayer, "> \u00A77$name") }
			}
			return
		}

		val value = module.getValue(args[1])

		if (value == null)
		{
			chat(thePlayer, "Available value names:")
			valueNames.value.forEach { name -> chat(thePlayer, "> $name") }
			return
		}

		val valueName = value.name.toLowerCase()
		if (value is BoolValue)
		{
			var newValue = !value.get()
			if (args.size > 2) when (args[2].toLowerCase())
			{
				"on", "true", "1", "yes", "positive", "pos" -> newValue = true
				"off", "false", "0", "no", "negative", "neg" -> newValue = false
			}
			value.set(newValue)

			chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 was toggled ${if (newValue) "\u00A7aON" else "\u00A7cOFF"}\u00A77.")
			playEdit()
		}
		else
		{
			if (args.size < 3)
			{
				chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 = ${
					when (value)
					{
						is RangeValue<*> -> "${value.getMin()}-${value.getMax()}"
						is ColorValue -> "\u00A7cRed: ${value.getRed()} \u00A7aGreen: ${value.getGreen()} \u00A79Blue: ${value.getBlue()}${if (value is RGBAColorValue) " \u00A77Alpha: ${value.getAlpha()}" else ""}"
						else -> (value as Value<*>).get()
					}
				}\u00A77.") // Print current state
				if (value is IntegerValue || value is FloatValue || value is TextValue) chatSyntax(thePlayer, "$moduleName $valueName <value>")
				else if (value is ListValue) chatSyntax(thePlayer, "$moduleName $valueName <${value.values.joinToString(separator = "/").toLowerCase()}>")
				return
			}

			if (args.size < 4) when (value)
			{
				is RangeValue<*> ->
				{
					chatSyntax(thePlayer, "$moduleName $valueName <min/max> <value>")
					return
				}

				is RGBColorValue ->
				{
					chatSyntax(thePlayer, "$moduleName $valueName <red/green/blue> <value>")
					return
				}

				is RGBAColorValue ->
				{
					chatSyntax(thePlayer, "$moduleName $valueName <red/green/blue/alpha> <value>")
					return
				}
			}


			try
			{
				when (value)
				{
					is RGBColorValue ->
					{
						when (args[2].toLowerCase())
						{
							"r", "red" -> value.set(args[3].toInt(), value.getGreen(), value.getBlue(), value.getAlpha())
							"g", "green" -> value.set(value.getRed(), args[3].toInt(), value.getBlue(), value.getAlpha())
							"b", "blue" -> value.set(value.getRed(), value.getGreen(), args[3].toInt(), value.getAlpha())
							else -> chatSyntax(thePlayer, "$moduleName $valueName <red/green/blue> <value>")
						}
					}

					is RGBAColorValue ->
					{
						when (args[2].toLowerCase())
						{
							"r", "red" -> value.set(args[3].toInt(), value.getGreen(), value.getBlue(), value.getAlpha())
							"g", "green" -> value.set(value.getRed(), args[3].toInt(), value.getBlue(), value.getAlpha())
							"b", "blue" -> value.set(value.getRed(), value.getGreen(), args[3].toInt(), value.getAlpha())
							"a", "alpha", "opacity" -> value.set(value.getRed(), value.getGreen(), value.getBlue(), args[3].toInt())
							else -> chatSyntax(thePlayer, "$moduleName $valueName <red/green/blue/alpha> <value>")
						}
					}

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
						chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 was set to \u00A78${BlockUtils.getBlockName(id)}\u00A77.")
						playEdit()
						return
					}

					is IntegerValue -> value.set(args[2].toInt())

					is IntegerRangeValue ->
					{
						when (args[2].toLowerCase())
						{
							"min" -> value.setMin(args[3].toInt())
							"max" -> value.setMax(args[3].toInt())
							else -> chatSyntax(thePlayer, "$moduleName $valueName <min/max> <value>")
						}
					}

					is FloatValue -> value.set(args[2].toFloat())

					is FloatRangeValue ->
					{
						when (args[2].toLowerCase())
						{
							"min" -> value.setMin(args[3].toFloat())
							"max" -> value.setMax(args[3].toFloat())
							else -> chatSyntax(thePlayer, "$moduleName $valueName <min/max> <value>")
						}
					}

					is ListValue ->
					{
						if (!value.contains(args[2]))
						{
							chatSyntax(thePlayer, "$moduleName $valueName <${value.values.joinToString(separator = "/").toLowerCase()}>")
							return
						}

						value.set(args[2])
					}

					is TextValue -> value.set(StringUtils.toCompleteString(args, 2))
				}

				chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 was set to \u00A78${
					when (value)
					{
						is RangeValue<*> -> "${value.getMin()}-${value.getMax()}"
						is ColorValue -> "\u00A7cRed: ${value.getRed()} \u00A7aGreen: ${value.getGreen()} \u00A79Blue: ${value.getBlue()}${if (value is RGBAColorValue) " \u00A77Alpha: ${value.getAlpha()}" else ""}"
						else -> (value as Value<*>).get()
					}
				}\u00A77.")
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
			1 -> values.asSequence().map { it.name.toLowerCase() }.filter { it.startsWith(args[0].toLowerCase(), false) }.toList()

			2 ->
			{
				when (module.getValue(args[0]))
				{
					is BlockValue -> return functions.getItemRegistryKeys().map { it.resourcePath.toLowerCase() }.filter { it.startsWith(args[1], true) }.toList()

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
