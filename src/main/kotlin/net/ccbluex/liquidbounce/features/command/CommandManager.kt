/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.command.CommandManager.getSubCommand
import net.ccbluex.liquidbounce.features.command.commands.client.*
import net.ccbluex.liquidbounce.features.command.commands.client.client.CommandClient
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.CommandMarketplace
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.CommandModels
import net.ccbluex.liquidbounce.features.command.commands.ingame.*
import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.*
import net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer.CommandFakePlayer
import net.ccbluex.liquidbounce.features.command.commands.module.CommandAutoAccount
import net.ccbluex.liquidbounce.features.command.commands.module.CommandAutoDisable
import net.ccbluex.liquidbounce.features.command.commands.module.CommandInvsee
import net.ccbluex.liquidbounce.features.command.commands.module.CommandXRay
import net.ccbluex.liquidbounce.features.command.commands.module.teleport.CommandPlayerTeleport
import net.ccbluex.liquidbounce.features.command.commands.module.teleport.CommandTeleport
import net.ccbluex.liquidbounce.features.command.commands.module.teleport.CommandVClip
import net.ccbluex.liquidbounce.features.command.commands.translate.CommandAutoTranslate
import net.ccbluex.liquidbounce.features.command.commands.translate.CommandTranslate
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.levenshtein
import java.util.concurrent.CompletableFuture
import kotlin.math.min

/**
 * Key: Command name or alias
 * Value: Command
 */
private val rootCommandMap = Object2ObjectRBTreeMap<String, Command>(String.CASE_INSENSITIVE_ORDER)

/**
 * Command set. Sorted with name.
 */
private val commandSet = ObjectRBTreeSet<Command>(Comparator.comparing({ it.name }, String.CASE_INSENSITIVE_ORDER))

/**
 * Contains routines for handling commands
 * and the command registry
 *
 * @author superblaubeere27 (@team CCBlueX)
 */
object CommandManager : Collection<Command> by commandSet {

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

        /**
         * How many hints should we give for unknown commands?
         */
        val hintCount by int("HintCount", 5, 0..10)
    }

    init {
        ConfigSystem.root(Options)

        // Initialize the executor
        CommandExecutor
    }

    fun registerInbuilt() {
        val commands = arrayOf(
            CommandClient,
            CommandFriend,
            CommandToggle,
            CommandBind,
            CommandCenter,
            CommandHelp,
            CommandBinds,
            CommandClear,
            CommandHide,
            CommandInvsee,
            CommandItems,
            CommandPanic,
            CommandValue,
            CommandPing,
            CommandRemoteView,
            CommandXRay,
            CommandTargets,
            CommandConfig,
            CommandLocalConfig,
            CommandAutoDisable,
            CommandScript,
            CommandContainers,
            CommandSay,
            CommandFakePlayer,
            CommandAutoAccount,
            CommandDebug,
            CommandItemRename,
            CommandItemGive,
            CommandItemSkull,
            CommandItemStack,
            CommandItemEnchant,
            CommandUsername,
            CommandCoordinates,
            CommandVClip,
            CommandTeleport,
            CommandPlayerTeleport,
            CommandTps,
            CommandServerInfo,
            CommandModels,
            CommandTranslate,
            CommandAutoTranslate,
            CommandMarketplace
        )

        commands.forEach {
            addCommand(it.createCommand())
        }
    }

    fun addCommand(command: Command) {
        if (!commandSet.add(command)) {
            error("Command '${command.name}' already exists")
        }
        rootCommandMap.putCommand(command)
    }

    fun removeCommand(command: Command) {
        if (!commandSet.remove(command) ||
            rootCommandMap.remove(command.name) !== command ||
            command.aliases.any { rootCommandMap.remove(it) !== command }
        ) {
            error("Command '${command.name}' does not exist")
        }
    }

    /**
     * Returns the instance of the subcommand that would be executed by a command
     * e.g. `getSubCommand(".friend add Player137 &3superblaubeere27")`
     * would return the instance of `add`
     *
     * @return A [Pair] of the subcommand and the index of the tokenized [cmd] it is in, if none was found, null
     */
    private fun getSubCommand(cmd: String): Pair<Command, Int>? {
        return getSubCommand(tokenizeCommand(cmd).first)
    }

    /**
     * Used for this implementation of [getSubCommand] and other command parsing methods
     *
     * @param args The input command split on spaces
     * @param currentCommand The current command that is being researched
     * @param idx The current index that is researched only used for implementation
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
        val commandMap = currentCommand?.first?.subcommandMap ?: rootCommandMap

        // Look if something matches the current index, if it does, look if there are further matches
        commandMap[args[idx]]?.let {
            return getSubCommand(args, Pair(it, idx), idx + 1)
        }

        // If no match was found, currentCommand is the subcommand that we searched for
        return currentCommand
    }

    /**
     * Executes a command.
     *
     * @param cmd The command. If there is no command in it (it is empty or only whitespaces), this method is a no op
     */
    @ScriptApiRequired
    @JvmName("execute")
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
            translation(
                "liquidbounce.commandManager.unknownCommand",
                args[0]
            ),
            usageInfo = if (rootCommandMap.isEmpty() || Options.hintCount == 0) {
                null
            } else {
                commandSet.sortedBy { command ->
                    var distance = levenshtein(args[0], command.name)
                    if (command.aliases.isNotEmpty()) {
                        distance = min(
                            distance,
                            command.aliases.minOf { levenshtein(args[0], it) }
                        )
                    }
                    distance
                }.take(Options.hintCount).map { command ->
                    buildString {
                        append(command.name)
                        if (command.aliases.isNotEmpty()) {
                            command.aliases.joinTo(this, separator = "/", prefix = " (", postfix = ")")
                        }
                    }
                }
            }
        )
        val command = pair.first

        // If the command is not executable, don't allow it to be executed
        if (!command.executable) {
            throw CommandException(
                translation("liquidbounce.commandManager.invalidUsage", args[0]),
                usageInfo = command.usage()
            )
        }

        // The index the command is in
        val idx = pair.second

        // If there are more arguments for a command that takes no parameters
        if (command.parameters.isEmpty() && idx != args.size - 1) {
            throw CommandException(
                translation("liquidbounce.commandManager.commandTakesNoParameters"),
                usageInfo = command.usage()
            )
        }

        // If there is a required parameter after the supply of arguments ends, it is absent
        if (args.size - idx - 1 < command.parameters.size && command.parameters[args.size - idx - 1].required) {
            throw CommandException(
                translation(
                    "liquidbounce.commandManager.parameterRequired",
                    command.parameters[args.size - idx - 1].name
                ),
                usageInfo = command.usage()
            )
        }

        // The values of the parameters. One for each parameter
        val parsedParameters = arrayOfNulls<Any>(args.size - idx - 1)

        // If the last parameter is a vararg, there might be no argument for it.
        // In this case, its value might be null, which is against the specification.
        // To fix this, if the last parameter is a vararg, initialize it with an empty array
        if (command.parameters.lastOrNull()?.vararg == true && command.parameters.size > args.size - idx) {
            parsedParameters[command.parameters.size - 1] = emptyArray<Any>()
        }

        for (i in (idx + 1) until args.size) {
            val paramIndex = i - idx - 1

            // Check if there is a parameter for this index
            if (paramIndex >= command.parameters.size) {
                throw CommandException(
                    translation("liquidbounce.commandManager.unknownParameter", args[i]),
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

        @Suppress("UNCHECKED_CAST")
        val ctx = Command.Handler.Context(command, parsedParameters as Array<out Any>)
        with(command.handler!!) { ctx() }
    }

    /**
     * The routine that handles the parsing of a single parameter
     */
    private fun parseParameter(command: Command, argument: String, parameter: Parameter<*>): Any {
        if (parameter.verifier == null) {
            return argument
        }

        when (val validationResult = parameter.verifier.verifyAndParse(argument)) {
            is Parameter.Verificator.Result.Ok -> {
                return validationResult.mappedResult
            }
            is Parameter.Verificator.Result.Error -> {
                throw CommandException(
                    translation(
                        "liquidbounce.commandManager.invalidParameterValue",
                        parameter.name,
                        argument,
                        validationResult.errorMessage
                    ),
                    usageInfo = command.usage()
                )
            }
        }
    }

    /**
     * Tokenizes the [line].
     *
     * For example: `.friend add "Senk Ju"` -> [[`.friend`, `add`, `Senk Ju`]]
     *
     * @return A pair of the tokenized command and the starting indices of the tokens
     */
    fun tokenizeCommand(line: String): Pair<List<String>, List<Int>> {
        val output = ArrayList<String>()
        val outputIndices = ArrayList<Int>()
        val stringBuilder = StringBuilder()

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

            when (c) {
                // Is the current char an escape char?
                '\\' -> escaped = true // Enable escape for the next character
                '"' -> quote = !quote
                ' ' if !quote -> {
                    // Is the buffer not empty? Also ignore stuff like .friend   add SenkJu
                    if (stringBuilder.isNotBlank()) {
                        output.add(stringBuilder.toString())

                        // Reset string buffer
                        stringBuilder.setLength(0)
                        outputIndices.add(idx)
                    }
                }
                else -> stringBuilder.append(c)
            }
        }

        // Is there something left in the buffer?
        if (stringBuilder.isNotBlank()) {
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

    fun autoComplete(origCmd: String, start: Int): CompletableFuture<Suggestions> {
        if (HideAppearance.isDestructed) {
            return Suggestions.empty()
        }

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
                val arg = args[0]
                // get all commands that start with the argument
                rootCommandMap.subMap(
                    arg,
                    arg + Char.MAX_VALUE,
                ).values.forEach { command ->
                    builder.suggest(command.name)
                }

                return builder.buildFuture()
            }

            if (pair == null) {
                return Suggestions.empty()
            }

            pair.first.autoComplete(builder, tokenized, pair.second, nextParameter)

            return builder.buildFuture()
        } catch (e: Exception) {
            logger.error("Failed to supply autocompletion suggestions for '$origCmd'", e)

            return Suggestions.empty()
        }
    }


}
