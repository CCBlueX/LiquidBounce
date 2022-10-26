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
        Command(module.name.lowercase()) {

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
            .joinToString(separator = "/") { it.name.lowercase() }

        val moduleName = module.name.lowercase()

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

            chat("§7${module.name} §8${args[1]}§7 was toggled ${if (newValue) "§8on§7" else "§8off§7" + "."}")
            playEdit()
        } else {
            if (args.size < 3) {
                if (value is IntegerValue || value is FloatValue || value is TextValue)
                    chatSyntax("$moduleName ${args[1].lowercase()} <value>")
                else if (value is ListValue)
                    chatSyntax("$moduleName ${args[1].lowercase()} <${value.values.joinToString(separator = "/").lowercase()}>")
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
                        chat("§7${module.name} §8${args[1].lowercase()}§7 was set to §8${BlockUtils.getBlockName(id)}§7.")
                        playEdit()
                        return
                    }
                    is IntegerValue -> value.set(args[2].toInt())
                    is FloatValue -> value.set(args[2].toFloat())
                    is ListValue -> {
                        if (!value.contains(args[2])) {
                            chatSyntax("$moduleName ${args[1].lowercase()} <${value.values.joinToString(separator = "/").lowercase()}>")
                            return
                        }

                        value.set(args[2])
                    }
                    is TextValue -> value.set(StringUtils.toCompleteString(args, 2))
                }

                chat("§7${module.name} §8${args[1]}§7 was set to §8${value.get()}§7.")
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
                .map { it.name.lowercase() }
            2 -> {
                when(module.getValue(args[0])) {
                    is BlockValue -> {
                        return Item.itemRegistry.keys
                                .map { it.resourcePath.lowercase() }
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
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
