/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.serialized
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.item.Item

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
                    is FontValue -> "<font name> <font size>"
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
            valueNames.value.forEach { name -> chat(thePlayer, "> \u00A77$name") }
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
                chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 = ${valueToString(value)}\u00A77.") // Print current state
                if (value is IntegerValue || value is FloatValue || value is TextValue) chatSyntax(thePlayer, "$moduleName $valueName <value>")
                else if (value is ListValue) chatSyntax(thePlayer, "$moduleName $valueName <${value.values.joinToString(separator = "|").toLowerCase()}>")
                return
            }

            try
            {
                if (args.size < 4) when (value)
                {
                    is RangeValue<*> ->
                    {
                        chatSyntax(thePlayer, "$moduleName $valueName <min|max> <value>")
                        return
                    }

                    is RGBColorValue ->
                    {
                        if (args[2].startsWith('#') && args[2].length <= 9)
                        {
                            value.set(args[2].substring(1).takeLast(6).toInt(16))
                            chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 was set to \u00A78${valueToString(value)}\u00A77.")
                            return
                        }

                        chatSyntax(thePlayer, "$moduleName $valueName <<red|green|blue> <value> or #<hex>>")
                        return
                    }

                    is RGBAColorValue ->
                    {
                        if (args[2].startsWith('#') && args[2].length <= 9)
                        {
                            val hex = args[2].substring(1)
                            value.set(hex.takeLast(6).toInt(16) or ((hex.take(2).toInt(16) and 0xFF shl 24)))
                            chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 was set to \u00A78${valueToString(value)}\u00A77.")
                            return
                        }

                        chatSyntax(thePlayer, "$moduleName $valueName <<red|green|blue|alpha> <value> or #<hex>>")
                        return
                    }
                }
            }
            catch (e: NumberFormatException)
            {
                chat(thePlayer, "\u00A78${args[2]}\u00A77 cannot be converted to number!")
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
                            val tmpId = Block.getBlockFromName(args[2])?.let(Block::getIdFromBlock)

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

                    is FloatValue -> value.set(args[2].toFloat())

                    is ListValue ->
                    {
                        if (!value.contains(args[2]))
                        {
                            chatSyntax(thePlayer, "$moduleName $valueName <${value.values.joinToString(separator = "|").toLowerCase()}>")
                            return
                        }

                        value.set(args[2])
                    }

                    is TextValue -> value.set(StringUtils.toCompleteString(args, 2))

                    is FontValue ->
                    {
                        val fontName = args[2].replace("_", " ", false)

                        if (Fonts.fonts.firstOrNull { (Fonts.getFontDetails(it) ?: return@firstOrNull false).name.equals(fontName, true) && (args.size <= 3 || Fonts.getFontDetails(it)?.fontSize == args[3].toInt()) }?.let(value::set) == null)
                        {
                            chat(thePlayer, "Font \"$fontName\" ${if (args.size >= 4) "- ${args[3]}" else ""} not found.")
                            return
                        }
                    }

                    else -> try
                    {
                        val newValue = lazy(LazyThreadSafetyMode.NONE) { if (args[3].startsWith("0x", ignoreCase = true)) args[3].substring(2).toInt(16) else args[3].toInt(10) }

                        when (value)
                        {
                            is RGBColorValue ->
                            {
                                when (args[2].toLowerCase())
                                {
                                    "r", "red" -> value.set(newValue.value, value.getGreen(), value.getBlue(), value.getAlpha())
                                    "g", "green" -> value.set(value.getRed(), newValue.value, value.getBlue(), value.getAlpha())
                                    "b", "blue" -> value.set(value.getRed(), value.getGreen(), newValue.value, value.getAlpha())
                                    else -> chatSyntax(thePlayer, "$moduleName $valueName <red|green|blue> <value>")
                                }
                            }

                            is RGBAColorValue ->
                            {
                                when (args[2].toLowerCase())
                                {
                                    "r", "red" -> value.set(newValue.value, value.getGreen(), value.getBlue(), value.getAlpha())
                                    "g", "green" -> value.set(value.getRed(), newValue.value, value.getBlue(), value.getAlpha())
                                    "b", "blue" -> value.set(value.getRed(), value.getGreen(), newValue.value, value.getAlpha())
                                    "a", "alpha", "o", "opacity" -> value.set(value.getRed(), value.getGreen(), value.getBlue(), newValue.value)
                                    else -> chatSyntax(thePlayer, "$moduleName $valueName <red|green|blue|alpha> <value>")
                                }
                            }

                            is IntegerRangeValue ->
                            {
                                when (args[2].toLowerCase())
                                {
                                    "min" -> value.setMin(newValue.value)
                                    "max" -> value.setMax(newValue.value)
                                    else -> chatSyntax(thePlayer, "$moduleName $valueName <min|max> <value>")
                                }
                            }

                            is FloatRangeValue ->
                            {
                                when (args[2].toLowerCase())
                                {
                                    "min" -> value.setMin(args[3].toFloat())
                                    "max" -> value.setMax(args[3].toFloat())
                                    else -> chatSyntax(thePlayer, "$moduleName $valueName <min|max> <value>")
                                }
                            }
                        }
                    }
                    catch (e: NumberFormatException)
                    {
                        chat(thePlayer, "\u00A78${args[3]}\u00A77 cannot be converted to number!")
                        return
                    }
                }
            }
            catch (e: NumberFormatException)
            {
                chat(thePlayer, "\u00A78${args[2]}\u00A77 cannot be converted to number!")
                return
            }

            chat(thePlayer, "\u00A77${module.name} \u00A78$valueName\u00A77 was set to \u00A78${valueToString(value)}\u00A77.")
            playEdit()
        }
    }

    private fun valueToString(value: AbstractValue): String = when (value)
    {
        is RangeValue<*> -> "${value.getMin()}-${value.getMax()}"

        is ColorValue ->
        {
            val hasAlpha = value is RGBAColorValue
            "\u00A7cRed: ${value.getRed()} \u00A7aGreen: ${value.getGreen()} \u00A79Blue: ${value.getBlue()}${if (hasAlpha) " \u00A77Alpha: ${value.getAlpha()}" else ""} \u00A78(Hex: #${if (hasAlpha) encodeToHex(value.getAlpha()) else ""}${encodeToHex(value.getRed())}${encodeToHex(value.getGreen())}${encodeToHex(value.getBlue())})"
        }

        is FontValue -> value.get().serialized ?: "(Unknown font)"

        else -> "${(value as Value<*>).get()}"
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        return when (args.size)
        {
            1 -> values.map(AbstractValue::name).filter { it.startsWith(args[0], true) }

            2 -> when (val value = module.getValue(args[0]))
            {
                is BlockValue -> return Item.itemRegistry.keys.map { it.resourcePath.toLowerCase() }.filter { it.startsWith(args[1], true) }

                is ListValue -> return value.values.filter { it.startsWith(args[1], true) }

                is FontValue -> Fonts.fonts.mapNotNull(Fonts::getFontDetails).map { it.name.replace(' ', '_') }.filter { it.startsWith(args[1], true) }

                else -> emptyList()
            }

            3 -> if (module.getValue(args[0]) is FontValue) Fonts.fonts.mapNotNull(Fonts::getFontDetails).filter { it.name.equals(args[1].replace('_', ' '), ignoreCase = true) }.map { "${it.fontSize}" }.filter { it.startsWith(args[2], true) } else emptyList()

            else -> emptyList()
        }
    }

    private fun encodeToHex(hex: Int) = hex.toString(16).toUpperCase().padStart(2, '0')
}
