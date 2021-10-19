/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.ChatSendEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.client.*
import net.ccbluex.liquidbounce.features.command.commands.creative.CommandItemGive
import net.ccbluex.liquidbounce.features.command.commands.creative.CommandItemRename
import net.ccbluex.liquidbounce.features.command.commands.creative.CommandItemSkull
import net.ccbluex.liquidbounce.features.command.commands.utility.CommandPosition
import net.ccbluex.liquidbounce.features.command.commands.utility.CommandUsername
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.outputString
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import java.util.concurrent.CompletableFuture

class CommandException(val text: TranslatableText, cause: Throwable? = null, val usageInfo: List<String>? = null) :
    Exception(text.outputString(), cause)

/**
 * Links minecraft with the command engine
 */
object CommandExecutor : Listenable {

    /**
     * Handles command execution
     */
    val chatEventHandler = handler<ChatSendEvent> {
        if (it.message.startsWith(CommandManager.Options.prefix)) {
            try {
                CommandManager.execute(it.message.substring(CommandManager.Options.prefix.length))
            } catch (e: CommandException) {
                chat(e.text.styled { it.withColor(Formatting.RED) })
                chat("§cUsage: ")

                if (e.usageInfo != null) {
                    var first = true

                    // Zip the usage info together, e.g.
                    //  .friend add <name> [<alias>]
                    //  OR .friend remove <name>
                    e.usageInfo.forEach { usage ->
                        chat("§c ${if (first) "" else "OR "}.$usage")

                        if (first) {
                            first = false
                        }
                    }
                }
            } catch (e: Exception) {
                chat(
                    TranslatableText("liquidbounce.commandManager.exceptionOccurred", e).styled {
                        it.withColor(
                            Formatting.RED
                        )
                    }
                )
            }

            it.cancelEvent()
        }
    }

}

/**
 * Contains routines for handling commands
 * and the command registry
 *
 * @author superblaubeere27 (@team CCBlueX)
 */
object CommandManager : Iterable<Command> {

    internal val commands = mutableListOf<Command>()

    object Options : Configurable("Commands") {
        /**
         * The prefix of the commands.
         *
         * ```
         * .friend add "Senk Ju"
         * ^
         * ------
         * prefix (.)
         * ```
         */
        var prefix by text("prefix", ".")

    }

    init {
        ConfigSystem.root(Options)

        // Initialize the executor
        CommandExecutor
    }

    fun registerInbuilt() {
        // client commands
        addCommand(CommandClient.createCommand())
        addCommand(CommandFriend.createCommand())
        addCommand(CommandToggle.createCommand())
        addCommand(CommandBind.createCommand())
        addCommand(CommandHelp.createCommand())
        addCommand(CommandBinds.createCommand())
        addCommand(CommandPrefix.createCommand())
        addCommand(CommandClear.createCommand())
        addCommand(CommandHide.createCommand())
        addCommand(CommandPanic.createCommand())
        addCommand(CommandValue.createCommand())
        addCommand(CommandPing.createCommand())
        addCommand(CommandRemoteView.createCommand())

        // creative commands
        addCommand(CommandItemRename.createCommand())
        addCommand(CommandItemGive.createCommand())
        addCommand(CommandItemSkull.createCommand())

        // utility commands
        addCommand(CommandUsername.createCommand())
        addCommand(CommandPosition.createCommand())
    }

    fun addCommand(command: Command) {
        commands.add(command)
    }

    /**
     * Returns the instance of the subcommand that would be executed by a command
     * e.g. `getSubCommand(".friend add Player137 &3superblaubeere27")`
     * would return the instance of `add`
     *
     * @return A [Pair] of the subcommand and the index of the tokenized [cmd] it is in, if none was found, null
     */
    fun getSubCommand(cmd: String): Pair<Command, Int>? {
        return getSubCommand(tokenizeCommand(cmd).first)
    }

    /**
     * Used for this implementation of [getSubCommand] and other command parsing methods
     *
     * @param args The input command split on spaces
     * @param currentCommand The current command that is being researched
     * @param idx The current index that is researched, only used for implementation
     *
     * @return A [Pair] of the subcommand and the index of [args] it is in, if none was found, null
     */
    private fun getSubCommand(
        args: List<String>,
        currentCommand: Pair<Command, Int>? = null,
        idx: Int = 0
    ): Pair<Command, Int>? {
        // Return the last command when there are no more arguments
        if (idx >= args.size) {
            return currentCommand
        }

        // If currentCommand is null, idx must be 0, so search in all commands
        val commandSupplier = currentCommand?.first?.subcommands?.asIterable() ?: commands

        // Look if something matches the current index, if it does, look if there are further matches
        commandSupplier
            .firstOrNull {
                it.name.equals(args[idx], true) || it.aliases.any { alias ->
                    alias.equals(
                        args[idx],
                        true
                    )
                }
            }
            ?.let { return getSubCommand(args, Pair(it, idx), idx + 1) }

        // If no match was found, currentCommand is the subcommand that we searched for
        return currentCommand
    }

    /**
     * Executes a command.
     *
     * @param cmd The command. If there is no command in it (it is empty or only whitespaces), this method is a no op
     */
    fun execute(cmd: String) {
        val args = tokenizeCommand(cmd).first

        // Prevent bugs
        if (args.isEmpty()) {
            return
        }

        // getSubcommands will only return null if it returns on the first index.
        // since the first index must contain a valid command, it is reported as
        // unknown
        val pair = getSubCommand(args) ?: throw CommandException(
            TranslatableText(
                "liquidbounce.commandManager.unknownCommand",
                args[0]
            )
        )
        val command = pair.first

        // If the command is not executable, don't allow it to be executed
        if (!command.executable) {
            throw CommandException(
                TranslatableText("liquidbounce.commandManager.invalidUsage", args[0]),
                usageInfo = command.usage()
            )
        }

        // The index the command is in
        val idx = pair.second

        // If there are more arguments for a command that takes no parameters
        if (command.parameters.isEmpty() && idx != args.size - 1) {
            throw CommandException(
                TranslatableText("liquidbounce.commandManager.commandTakesNoParameters"),
                usageInfo = command.usage()
            )
        }

        // If there is a required parameter after the supply of arguments ends, it is absent
        if (args.size - idx - 1 < command.parameters.size && command.parameters[args.size - idx - 1].required) {
            throw CommandException(
                TranslatableText(
                    "liquidbounce.commandManager.parameterRequired",
                    command.parameters[args.size - idx - 1].name
                ),
                usageInfo = command.usage()
            )
        }

        // The values of the parameters. One for each parameter
        val parsedParameters = arrayOfNulls<Any>(args.size - idx)

        // If the last parameter is a vararg, there might be no argument for it.
        // In this case it's value might be null which is against the specification.
        // To fix this, if the last parameter is a vararg, initialize it with an empty array
        if (command.parameters.lastOrNull()?.vararg == true) {
            parsedParameters[command.parameters.size - 1] = emptyArray<Any>()
        }

        for (i in (idx + 1) until args.size) {
            val paramIndex = i - idx - 1

            // Check if there is a parameter for this index
            if (paramIndex >= command.parameters.size) {
                throw CommandException(
                    TranslatableText("liquidbounce.commandManager.unknownParameter", args[i]),
                    usageInfo = command.usage()
                )
            }

            val parameter = command.parameters[paramIndex]

            // Special treatment for varargs
            val parameterValue = if (parameter.vararg) {
                val outputArray = arrayOfNulls<Any>(args.size - i)

                for (j in i until args.size) {
                    outputArray[j - i] = parseParameter(command, args[j], parameter)
                }

                outputArray
            } else {
                parseParameter(command, args[i], parameter)
            }

            // Store the parsed value in the parameter array
            parsedParameters[paramIndex] = parameterValue

            // Varargs can only occur at the end and the following args shouldn't be treated
            // as parameters, so we can end
            if (parameter.vararg) {
                break
            }
        }

        if (!command.executable) {
            throw CommandException(
                TranslatableText("liquidbounce.commandManager.commandNotExecutable", command.name),
                usageInfo = command.usage()
            )
        }

        @Suppress("UNCHECKED_CAST")
        command.handler!!(command, parsedParameters as Array<Any>)
    }

    /**
     * The routine that handles the parsing of a single parameter
     */
    private fun parseParameter(command: Command, argument: String, parameter: Parameter<*>): Any {
        return if (parameter.verifier == null) {
            argument
        } else {
            val validationResult = parameter.verifier.invoke(argument)

            if (validationResult.errorMessage != null) {
                throw CommandException(
                    TranslatableText(
                        "liquidbounce.commandManager.invalidParameterValue",
                        parameter.name,
                        argument,
                        validationResult.errorMessage
                    ),
                    usageInfo = command.usage()
                )
            }

            val mappedResult = validationResult.mappedResult

            mappedResult!!
        }
    }

    /**
     * Tokenizes the [line].
     *
     * For example: `.friend add "Senk Ju"` -> [[`.friend`, `add`, `Senk Ju`]]
     */
    fun tokenizeCommand(line: String): Pair<List<String>, List<Int>> {
        val output = ArrayList<String>(10)
        val outputIndices = ArrayList<Int>(10)
        val stringBuilder = StringBuilder(40)

        outputIndices.add(0)

        var escaped = false
        var quote = false

        var idx = 0

        for (c in line.toCharArray()) {
            idx++

            // Was this character escaped?
            if (escaped) {
                stringBuilder.append(c)

                escaped = false
                continue
            }

            // Is the current char an escape char?
            if (c == '\\') {
                escaped = true // Enable escape for the next character
            } else if (c == '"') {
                quote = !quote
            } else if (c == ' ' && !quote) {
                // Is the buffer not empty? Also ignore stuff like .friend   add SenkJu
                if (stringBuilder.trim().isNotEmpty()) {
                    output.add(stringBuilder.toString())

                    // Reset string buffer
                    stringBuilder.setLength(0)
                    outputIndices.add(idx)
                }
            } else {
                stringBuilder.append(c)
            }
        }

        // Is there something left in the buffer?
        if (stringBuilder.trim().isNotEmpty()) {
            // If a string was not closed, don't remove the quote
            // e.g. .friend add "SenkJu -> [.friend, add, "SenkJu]
            if (quote) {
                output.add('"' + stringBuilder.toString())
            } else {
                output.add(stringBuilder.toString())
            }
        }

        return Pair(output, outputIndices)
    }

    override fun iterator() = commands.iterator()

    fun autoComplete(origCmd: String, start: Int): CompletableFuture<Suggestions> {
        if (start < Options.prefix.length) {
            return Suggestions.empty()
        }

        try {
            val cmd = origCmd.substring(Options.prefix.length, start)
            val tokenized = tokenizeCommand(cmd)
            var args = tokenized.first

            if (args.isEmpty()) {
                args = listOf("")
            }

            val nextParameter = !args.last().endsWith(" ") && cmd.endsWith(" ")
            var currentArgStart = tokenized.second.lastOrNull()

            if (currentArgStart == null) {
                currentArgStart = 0
            }

            if (nextParameter) {
                currentArgStart = cmd.length
            }

            val builder = SuggestionsBuilder(origCmd, currentArgStart + Options.prefix.length)

            // getSubcommands will only return null if it returns on the first index.
            // since the first index must contain a valid command, it is reported as
            // unknown
            val pair = getSubCommand(args)

            if (args.size == 1 && (pair == null || !nextParameter)) {
                for (command in this.commands) {
                    if (command.name.startsWith(args[0], true)) {
                        builder.suggest(command.name)
                    }

                    command.aliases.filter { it.startsWith(args[0], true) }.forEach { builder.suggest(it) }
                }

                return builder.buildFuture()
            }

            if (pair == null) {
                return Suggestions.empty()
            }

            pair.first.autoComplete(builder, tokenized, pair.second, nextParameter)

            return builder.buildFuture()
        } catch (e: Exception) {
            e.printStackTrace()

            return Suggestions.empty()
        }

        //        val command = pair.first
//
//        // If the command is not executable, don't allow it to be executed
//        if (!command.executable) {
//            return Suggestions.empty()
//        }
//
//        // The index the command is in
//        val idx = pair.second
//
//        var paramIdx = command.parameters.size - idx
//
//        if ()
//            paramIdx++
//
//        val parameter = if (paramIdx >= args.size) {
//            val lastParameter = command.parameters.lastOrNull()
//
//            if (lastParameter?.vararg != true)
//                return Suggestions.empty()
//
//            lastParameter
//        } else {
//            command.parameters[paramIdx]
//        }
//
//        val handler = parameter.autocompletionHandler ?: return Suggestions.empty()
//
//        for (s in handler(args[paramIdx])) {
//            builder.suggest(s)
//        }
//
//        return builder.buildFuture()
    }

}
