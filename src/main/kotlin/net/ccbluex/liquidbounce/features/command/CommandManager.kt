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

import net.ccbluex.liquidbounce.event.ChatSendEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.FriendCommand
import net.ccbluex.liquidbounce.features.command.commands.ToggleCommand
import net.ccbluex.liquidbounce.utils.chat

class CommandException(message: String, cause: Throwable? = null, val usageInfo: List<String>? = null) :
    Exception(message, cause)

/**
 * Links minecraft with the command engine
 */
object CommandExecutor : Listenable {

    /**
     * Handles command execution
     */
    val chatEventHandler = handler<ChatSendEvent> {
        if (it.message.startsWith(CommandManager.prefix)) {
            try {
                CommandManager.execute(it.message.substring(CommandManager.prefix.length))
            } catch (e: CommandException) {
                chat("§c${e.message}")
                chat("§cUsage: ")

                if (e.usageInfo != null) {
                    var first = true

                    // Zip the usage info together, e.g.
                    //  .friend add <name> [<alias>]
                    //  OR .friend remove <name>
                    e.usageInfo.forEach { usage ->
                        chat("§c ${if (first) "" else "OR "}.${usage}")

                        if (first) {
                            first = false
                        }
                    }
                }
            } catch (e: Exception) {
                chat("§cAn exception occurred while executing the command: $e")
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
object CommandManager {

    private val commands = mutableListOf<Command>()

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
    val prefix = "."

    fun registerInbuilt() {
        addCommand(FriendCommand.createCommand())
        addCommand(ToggleCommand.createCommand())
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
        return getSubCommand(tokenize(cmd))
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
        if (idx >= args.size)
            return currentCommand

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
        val args = tokenize(cmd)

        // Prevent bugs
        if (args.isEmpty())
            return

        // getSubcommands will only return null if it returns on the first index.
        // since the first index must contain a valid command, it is reported as
        // unknown
        val pair = getSubCommand(args) ?: throw CommandException("Unknown command ${args[0]}")
        val command = pair.first

        // If the command is not executable, don't allow it to be executed
        if (!command.executable)
            throw CommandException("Invalid usage of command ${args[0]}", usageInfo = command.usage())

        // The index the command is in
        val idx = pair.second

        // If there are more arguments for a command that takes no parameters
        if (command.parameters.isEmpty() && idx != args.size - 1)
            throw CommandException("This command takes no parameters", usageInfo = command.usage())

        // If there is a required parameter after the supply of arguments ends, it is absent
        if (args.size - idx - 1 < command.parameters.size && command.parameters[args.size - idx - 1].required) {
            throw CommandException(
                "Parameter ${command.parameters[args.size - idx - 1].name} is required",
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
                throw CommandException("Unknown parameter \"${args[i]}\"", usageInfo = command.usage())
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
            if (parameter.vararg)
                break
        }

        if (!command.executable)
            throw CommandException("The command ${command.name} is not executable.", usageInfo = command.usage())

        @Suppress("UNCHECKED_CAST")
        command.handler!!.invoke(parsedParameters as Array<Any>)
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
                    "Invalid parameter value for parameter ${parameter.name} (\"$argument\"): ${validationResult.errorMessage}",
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
    fun tokenize(line: String): List<String> {
        val output = ArrayList<String>(10)
        val stringBuilder = StringBuilder(40)

        var escaped = false
        var quote = false

        for (c in line.toCharArray()) {
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

        return output
    }

}
