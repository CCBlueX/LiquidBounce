/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.item.Item

/**
 * Module command
 *
 * @author SenkJu
 */
class ModuleCommand(val module: Module, val values: List<Value<*>> = module.values) : Command(module.name.lowercase()) {

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

        when (val value = module[args[1]]) {
            null -> chatSyntax("$moduleName <$valueNames>")

            is BoolValue -> {
                if (args.size != 2) {
                    chatSyntax("$moduleName ${args[1].lowercase()}")
                    return
                }

                val newValue = !value.get()
                value.set(newValue)

                chat("§7${module.getName()} §8${args[1]}§7 was toggled ${if (newValue) "§8on§7" else "§8off§7" + "."}")
                playEdit()
            }

            else -> {
                if (
                    if (value is TextValue) args.size < 3
                    else args.size != 3
                ) {
                    when (value) {
                        is IntegerValue, is FloatValue, is TextValue ->
                            chatSyntax("$moduleName ${args[1].lowercase()} <value>")

                        is ListValue ->
                            chatSyntax("$moduleName ${args[1].lowercase()} <${value.values.joinToString(separator = "/").lowercase()}>")
                    }

                    return
                }

                try {
                    val pair: Pair<Boolean, String> = when (value) {
                        is BlockValue -> {
                            val id = try {
                                args[2].toInt()
                            } catch (exception: NumberFormatException) {
                                val tmpId = Block.getBlockFromName(args[2])?.let { Block.getIdFromBlock(it) }

                                if (tmpId == null || tmpId <= 0) {
                                    chat("§7Block §8${args[2]}§7 does not exist!")
                                    return
                                }

                                tmpId
                            }

                            if (!value.set(id)) {
                                chatInvalid(id.toString(), value)
                                return
                            }

                            chat("§7${module.getName()} §8${args[1].lowercase()}§7 was set to §8${getBlockName(id)}§7.")
                            playEdit()

                            return
                        }
                        is IntegerValue -> value.set(args[2].toInt()) to args[2]
                        is FloatValue -> value.set(args[2].toFloat()) to args[2]
                        is ListValue -> {
                            if (args[2] !in value) {
                                chatSyntax("$moduleName ${args[1].lowercase()} <${value.values.joinToString(separator = "/").lowercase()}>")
                                return
                            }

                            value.set(args[2]) to args[2]
                        }
                        is TextValue -> {
                            val string = StringUtils.toCompleteString(args, 2)
                            value.set(string) to string
                        }

                        else -> return
                    }

                    // If value wasn't changed successfully, write that previous argument isn't valid
                    if (!pair.first) {
                        chatInvalid(pair.second, value)
                        return
                    }

                    chat("§7${module.getName()} §8${args[1]}§7 was set to §8${value.get()}§7.")
                    playEdit()
                } catch (e: NumberFormatException) {
                    chatInvalid(args[2], value, "cannot be converted to a number for")
                }
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> values
                .filter { it !is FontValue && it.isSupported() && it.name.startsWith(args[0], true) }
                .map { it.name.lowercase() }
            2 -> {
                when (module[args[0]]) {
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

    private fun chatInvalid(arg: String, value: Value<*>, reason: String? = null) {
        val finalReason = reason ?:
            if (value.get().toString().equals(arg, true)) "is already the value of"
            else "isn't a valid value for"

        chat("§8$arg§7 $finalReason §8${value.name}§7!")
    }

}
