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
import net.minecraft.block.Block
import net.minecraft.item.Item

/**
 * Module command
 *
 * @author SenkJu
 */
class ModuleCommand(val module: Module, val values: List<Value<*>> = module.values) :
        Command(module.name.toLowerCase()) {

    init {
        if (values.isEmpty())
            throw IllegalArgumentException("Values are empty!")
    }

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val valueNames = values
            .filter { it !is FontValue }
            .joinToString(separator = "/") { it.name.toLowerCase() }

        val moduleName = module.name.toLowerCase()

        if (args.size < 2) {
            chatSyntax(if (values.size == 1) "$moduleName $valueNames <value>" else "$moduleName <$valueNames>")
            return
        }

        val value = module.getValue(args[1])

        if (value == null) {
            chatSyntax("$moduleName <$valueNames>")
            return
        }

        if (value is BoolValue) {
            val newValue = !value.get()
            value.set(newValue)

            chat("§7${module.name} §8${args[1]}§7 was toggled §8${if (newValue) "on" else "off"}§7.")
            playEdit()
        } else {
            if (args.size < 3) {
                if (value is IntegerValue || value is FloatValue || value is TextValue)
                    chatSyntax("$moduleName ${args[1].toLowerCase()} <value>")
                else if (value is ListValue)
                    chatSyntax("$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}>")
                else if (value is MultiListValue)
                    chatSyntax("$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}> ...")
                return
            }

            try {
                when (value) {
                    is BlockValue -> {
                        var id: Int

                        id = try {
                            args[2].toInt()
                        } catch (exception: NumberFormatException) {
                            val tmpId = Block.getBlockFromName(args[2])?.let { Block.getIdFromBlock(it) }

                            if (tmpId == null || tmpId <= 0) {
                                chat("§7Block §8${args[2]}§7 does not exist!")
                                return
                            }

                            tmpId
                        }

                        value.set(id)
                        chat("§7${module.name} §8${args[1].toLowerCase()}§7 was set to §8${BlockUtils.getBlockName(id)}§7.")
                        playEdit()
                        return
                    }
                    is IntegerValue -> value.set(args[2].toInt())
                    is FloatValue -> value.set(args[2].toFloat())
                    is ListValue -> {
                        if (!value.contains(args[2])) {
                            chatSyntax("$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}>")
                            return
                        }

                        value.set(args[2])
                    }
                    is MultiListValue -> {
                        for (i in 2 until args.size) {
                            value.toggle(args[i])
                        }
                    }
                    is TextValue -> value.set(StringUtils.toCompleteString(args, 2))
                }

                if (value is MultiListValue) {
                    chat("§7${module.name} §8${args[1]}§7 was ${if (value.get().size > 0) "set to §8${value.get().joinToString(separator = ", ").lowercase()}§7" else "unset"}.")
                } else {
                    chat("§7${module.name} §8${args[1]}§7 was set to §8${value.get()}§7.")
                }
                playEdit()
            } catch (e: NumberFormatException) {
                chat("§8${args[2]}§7 cannot be converted to number!")
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> values
                .filter { it !is FontValue && it.name.startsWith(args[0], true) }
                .map { it.name.toLowerCase() }
            2 -> {
                when(module.getValue(args[0])) {
                    is BlockValue -> {
                        return Item.itemRegistry.keys
                                .map { it.resourcePath.toLowerCase() }
                                .filter { it.startsWith(args[1], true) }
                    }
                    is ListValue -> {
                        values.forEach { value ->
                            if (!value.name.equals(args[0], true))
                                return@forEach
                            if (value is ListValue)
                                return value.values.filter { it.startsWith(args[1], true) }
                        }
                        return emptyList()
                    }
                    is MultiListValue -> {
                        values.forEach { value ->
                            if (!value.name.equals(args[0], true))
                                return@forEach
                            if (value is MultiListValue)
                                return value.values.filter { it.startsWith(args[1], true) }
                        }
                        return emptyList()
                    }
                    else -> emptyList()
                }
            }
            else -> {
                when(module.getValue(args[0])) {
                    is MultiListValue -> {
                        values.forEach { value ->
                            if (!value.name.equals(args[0], true))
                                return@forEach
                            if (value is MultiListValue)
                                return value.values.filter { it.startsWith(args.last(), true) }
                        }
                        return emptyList()
                    }
                    else -> emptyList()
                }
            }
        }
    }
}
