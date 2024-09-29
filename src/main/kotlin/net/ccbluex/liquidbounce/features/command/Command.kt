/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.minecraft.text.MutableText
import java.util.*

typealias CommandHandler = (Command, Array<Any>) -> Unit

class Command(
    val name: String,
    val aliases: Array<out String>,
    val parameters: Array<Parameter<*>>,
    var subcommands: Array<Command>,
    val hub: Boolean,
    val handler: CommandHandler?,
    private var parentCommand: Command? = null
) : QuickImports {
    val translationBaseKey: String
        get() = "liquidbounce.command.${getParentKeys(this, name)}"

    val description: String
        get() = translation("$translationBaseKey.description").convertToString()

    init {
        subcommands.forEach {
            check(it.parentCommand == null) {
                "Subcommand already has parent command"
            }

            it.parentCommand = this
        }

        parameters.forEach {
            check(it.command == null) {
                "Parameter already has a command"
            }

            it.command = this
        }
    }

    private fun getParentKeys(currentCommand: Command?, current: String): String {
        val parentName = currentCommand?.parentCommand?.name

        return if (parentName != null) {
            getParentKeys(currentCommand.parentCommand, "$parentName.subcommand.$current")
        } else {
            current
        }
    }

    fun result(key: String, vararg args: Any): MutableText {
        return translation("$translationBaseKey.result.$key", *args)
    }

    fun resultWithTree(key: String, vararg args: Any): MutableText {
        var parentCommand = this.parentCommand
        if (parentCommand != null) {
            // Keep going until parent command is null
            while (parentCommand?.parentCommand != null) {
                parentCommand = parentCommand.parentCommand
            }

            return parentCommand!!.result(key, *args)
        }

        return translation("$translationBaseKey.result.$key", *args)
    }

    /**
     * Returns the name of the command with the name of its parent classes
     */
    private fun getFullName(): String {
        val parent = this.parentCommand

        return if (parent == null) {
            this.name
        } else {
            parent.getFullName() + " " + this.name
        }
    }

    /**
     * Returns the formatted usage information of this command
     *
     * e.g. <code>command_name subcommand_name <required_arg> [[<optional_vararg>]...</code>
     */
    fun usage(): List<String> {
        val output = ArrayList<String>()

        // Don't show non-executable commands as executable
        if (handler != null) {
            val joiner = StringJoiner(" ")

            for (parameter in parameters) {
                var name = parameter.name

                name = if (parameter.required) {
                    "<$name>"
                } else {
                    "[<$name>]"
                }

                if (parameter.vararg) {
                    name += "..."
                }

                joiner.add(name)
            }

            output.add(this.name + " " + joiner.toString())
        }

        for (subcommand in subcommands) {
            for (subcommandUsage in subcommand.usage()) {
                output.add(this.name + " " + subcommandUsage)
            }
        }

        return output
    }

    fun autoComplete(
        builder: SuggestionsBuilder,
        tokenizationResult: Pair<List<String>, List<Int>>,
        commandIdx: Int,
        isNewParameter: Boolean
    ) {
        val args = tokenizationResult.first

        val offset = args.size - commandIdx - 1

        val isAtSecondParameterBeginning = offset == 0 && isNewParameter
        val isInSecondParameter = offset == 1 && !isNewParameter

        // Handle Subcommands
        if (isAtSecondParameterBeginning || isInSecondParameter) {
            val comparedAgainst = if (!isNewParameter) args[offset] else ""

            this.subcommands.forEach { subcommand ->
                if (subcommand.name.startsWith(comparedAgainst, true)) {
                    builder.suggest(subcommand.name)
                }

                subcommand.aliases.filter { it.startsWith(comparedAgainst, true) }.forEach { builder.suggest(it) }
            }
        }

        var paramIdx = args.size - commandIdx - 2

        if (isNewParameter) {
            paramIdx++
        }

        if (paramIdx < 0) {
            return
        }

        val idx = commandIdx + paramIdx + 1

        val parameter = if (paramIdx >= parameters.size) {
            val lastParameter = this.parameters.lastOrNull()

            if (lastParameter?.vararg != true) {
                return
            }

            lastParameter
        } else {
            this.parameters[paramIdx]
        }

        val handler = parameter.autocompletionHandler ?: return

        for (s in handler(args.getOrElse(idx) { "" }, args)) {
            builder.suggest(s)
        }
    }
}
