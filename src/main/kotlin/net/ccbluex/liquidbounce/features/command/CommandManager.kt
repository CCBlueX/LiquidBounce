/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.tree.LiteralCommandNode
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.deepestExecutableContext
import net.ccbluex.liquidbounce.features.command.brigadier.offset
import net.ccbluex.liquidbounce.features.command.commands.client.CommandBind
import net.ccbluex.liquidbounce.features.command.commands.client.CommandBinds
import net.ccbluex.liquidbounce.features.command.commands.client.CommandClear
import net.ccbluex.liquidbounce.features.command.commands.client.CommandConfig
import net.ccbluex.liquidbounce.features.command.commands.client.CommandDebug
import net.ccbluex.liquidbounce.features.command.commands.client.CommandFriend
import net.ccbluex.liquidbounce.features.command.commands.client.CommandHelp
import net.ccbluex.liquidbounce.features.command.commands.client.CommandHide
import net.ccbluex.liquidbounce.features.command.commands.client.CommandLocalConfig
import net.ccbluex.liquidbounce.features.command.commands.client.CommandPanic
import net.ccbluex.liquidbounce.features.command.commands.client.CommandAddon
import net.ccbluex.liquidbounce.features.command.commands.client.CommandScript
import net.ccbluex.liquidbounce.features.command.commands.client.CommandTargets
import net.ccbluex.liquidbounce.features.command.commands.client.CommandToggle
import net.ccbluex.liquidbounce.features.command.commands.client.CommandValue
import net.ccbluex.liquidbounce.features.command.commands.client.client.CommandClient
import net.ccbluex.liquidbounce.features.command.commands.client.marketplace.CommandMarketplace
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.CommandModels
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandCenter
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandCoordinates
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandPing
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandRemoteView
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandSay
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandServerInfo
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandTps
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandUsername
import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.CommandItemEnchant
import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.CommandItemGive
import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.CommandItemRename
import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.CommandItemSkull
import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.CommandItemStack
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
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.levenshtein
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.CompletableFuture
import kotlin.math.min

/**
 * Contains routines for handling commands
 * and the command registry
 *
 * All commands are registered directly against a Brigadier [CommandDispatcher] (see the
 * `brigadier` package DSL); the legacy meta-model has been removed.
 *
 * @author superblaubeere27 (@team CCBlueX)
 */
@Suppress("detekt:TooManyFunctions")
object CommandManager : EventListener {

    object GlobalSettings : ValueGroup("Commands") {

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
        var prefix by text("Prefix", ".")

        /**
         * How many hints should we give for unknown commands?
         */
        val hintCount by int("HintCount", 5, 0..10)
    }

    init {
        CommandExecutor
    }

    /**
     * Rebuilds the command tree on world join/leave: argument types capture the
     * registry access / feature flags at construction time (see [invalidate]).
     */
    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        invalidate()
    }

    fun registerInbuilt() {
        register(CommandPing)
        register(CommandTps)
        register(CommandUsername)
        register(CommandClear)
        register(CommandCoordinates)
        register(CommandHide)
        register(CommandPanic)
        register(CommandSay)
        register(CommandTranslate)
        register(CommandAutoTranslate)
        register(CommandItemRename)
        register(CommandMarketplace)
        register(CommandToggle)
        register(CommandTargets)
        register(CommandBinds)
        register(CommandAutoDisable)
        register(CommandInvsee)
        register(CommandXRay)
        register(CommandValue)
        register(CommandBind)
        register(CommandAutoAccount)
        register(CommandCenter)
        register(CommandHelp)
        register(CommandRemoteView)
        register(CommandDebug)
        register(CommandFriend)
        register(CommandClient)
        register(CommandConfig)
        register(CommandLocalConfig)
        register(CommandScript)
        register(CommandAddon)
        register(CommandFakePlayer)
        register(CommandItemGive)
        register(CommandItemSkull)
        register(CommandItemStack)
        register(CommandItemEnchant)
        register(CommandVClip)
        register(CommandTeleport)
        register(CommandPlayerTeleport)
        register(CommandServerInfo)
        register(CommandModels)
    }

    /**
     * Lazily built Brigadier command tree. Rebuilt whenever a command is registered or
     * unregistered (see [register] / [unregister] / [registerNodes] / [unregisterNodes]).
     */
    @Volatile
    private var brigadierDispatcher: CommandDispatcher<ClientCommandSource>? = null

    /**
     * Registration functions of commands written directly against the Brigadier tree
     * (see the [CommandRegistrar] interface); replayed whenever the dispatcher is rebuilt.
     */
    private val directCommandRegistrars = mutableListOf<CommandRegistrar>()

    /**
     * Dynamically provided command nodes (main nodes plus alias redirects), keyed by node name.
     * Replayed whenever the dispatcher is rebuilt; see [registerNodes].
     */
    private val dynamicCommandNodes =
        TreeMap<String, LiteralCommandNode<ClientCommandSource>>(String.CASE_INSENSITIVE_ORDER)

    /**
     * Registers a command written directly against the Brigadier tree.
     *
     * The registrar is recorded so it is replayed whenever the dispatcher is rebuilt
     * (see [getDispatcher]). If a dispatcher is already cached, the registrar is applied
     * to it once; if the cache is empty, the next [getDispatcher] rebuild includes it.
     */
    fun register(registrar: CommandRegistrar) {
        directCommandRegistrars.add(registrar)
        brigadierDispatcher?.let { registrar.register(it) }
    }

    /**
     * Removes a previously registered [CommandRegistrar], used when an add-on is torn down.
     *
     * Brigadier cannot remove a node from a built dispatcher, so the cache is dropped and
     * [getDispatcher] replays the remaining registrars instead.
     */
    fun unregister(registrar: CommandRegistrar) {
        if (directCommandRegistrars.remove(registrar)) {
            brigadierDispatcher = null
        }
    }

    /**
     * Registers command nodes built at runtime rather than by a [CommandRegistrar].
     *
     * All nodes are replayed whenever the dispatcher is rebuilt. Any node name already
     * taken on the dispatcher root - by a built-in command or another provider - fails
     * the whole registration, mirroring the previous `addCommand` duplicate-name check.
     * Without this, Brigadier would silently merge the node onto the existing root child,
     * overriding its command or grafting grandchildren into it.
     */
    fun registerNodes(nodes: Collection<LiteralCommandNode<ClientCommandSource>>) {
        // Case-insensitive on purpose: Brigadier merges children by exact name but matches
        // literals case-insensitively, so 'Toggle' must not slip past 'toggle'. Validating
        // everything up front keeps the registry untouched on conflict (no orphans).
        val taken = getDispatcher().root.children.mapTo(hashSetOf()) { it.name.lowercase() }
        val validated = nodes.onEach { node ->
            check(taken.add(node.name.lowercase())) {
                "Command '${node.name}' is already registered"
            }
        }

        validated.forEach { dynamicCommandNodes[it.name] = it }
        brigadierDispatcher = null
        getDispatcher()
    }

    /**
     * Unregisters dynamically provided command nodes by name, rebuilding the dispatcher.
     */
    fun unregisterNodes(names: Set<String>) {
        dynamicCommandNodes.keys.removeAll(names)
        brigadierDispatcher = null
    }

    /**
     * Drops the cached dispatcher so it is rebuilt with fresh argument state on next use.
     *
     * Called on world join/leave ([net.ccbluex.liquidbounce.event.events.WorldChangeEvent]):
     * argument types capture the registry access / feature flags at construction time
     * (e.g. `itemArgument()`, `resourceArgument()`), so a world change invalidates them.
     */
    fun invalidate() {
        brigadierDispatcher = null
    }

    /**
     * The literal nodes registered on the root of the current dispatcher, exposing the
     * command names (and aliases as redirecting literals) to consumers such as the help command.
     */
    internal val rootCommandNodes: Collection<LiteralCommandNode<ClientCommandSource>>
        get() = getDispatcher().root.children.filterIsInstance<LiteralCommandNode<ClientCommandSource>>()

    /**
     * Root literals that are real commands (not redirecting aliases), sorted by name.
     * Used by `.help` and unknown-command hints.
     */
    internal val mainCommandNodes: List<LiteralCommandNode<ClientCommandSource>>
        get() = rootCommandNodes.filter { it.redirect == null }.sortedBy { it.name }

    /**
     * Returns the lazily built [CommandDispatcher], rebuilding it whenever the command
     * registry changed (see [register] / [unregister] / [registerNodes] / [unregisterNodes]).
     */
    private fun getDispatcher(): CommandDispatcher<ClientCommandSource> {
        brigadierDispatcher?.let { return it }

        val dispatcher = CommandDispatcher<ClientCommandSource>()

        directCommandRegistrars.forEach { it.register(dispatcher) }
        dynamicCommandNodes.values.forEach { dispatcher.root.addChild(it) }

        brigadierDispatcher = dispatcher
        return dispatcher
    }

    /**
     * Counts how many leading tokens of [tokens] form the command path (root command name
     * plus subcommand names), by walking the literal children of the current node
     * case-insensitively. The first token that does not match any literal child starts
     * the argument part.
     */
    private fun resolvePathTokenCount(tokens: List<String>): Int {
        var node: com.mojang.brigadier.tree.CommandNode<ClientCommandSource> = getDispatcher().root
        var pathTokenCount = 0

        for (token in tokens) {
            val child = node.children.firstOrNull {
                it is LiteralCommandNode && it.name.equals(token, ignoreCase = true)
            } ?: break

            node = child
            pathTokenCount++
        }

        return pathTokenCount
    }

    /**
     * Executes a command.
     *
     * @param cmd The command. If there is no command in it (it is empty or only whitespaces), this method is a no op
     */
    @ScriptApiRequired
    @JvmName("execute")
    fun execute(cmd: String) {
        val normalized = normalizeCommandSpaces(cmd.trim())
        val tokens = tokenizeCommand(normalized).tokens

        // Prevent bugs
        if (tokens.isEmpty()) {
            return
        }

        // Lower-case only the command path (root command and subcommand names) to preserve
        // the case-insensitive behaviour of command paths; argument values stay untouched.
        val pathTokenCount = resolvePathTokenCount(tokens)
        val lowered = lowercaseCommandPath(normalized, pathTokenCount)
        val parse = getDispatcher().parse(StringReader(lowered), ClientCommandSource)

        if (parse.reader.canRead()) {
            throw mapParseFailure(
                parse,
                tokens.first(),
                usage = buildUsage(parse.context.build(lowered)),
                unknownHints = unknownCommandHints(tokens.first()),
            )
        }

        val context = parse.context.build(lowered)
        val executorContext = context.deepestExecutableContext()
        val executor = executorContext?.command

        if (executorContext == null || executor == null) {
            // The path resolved to a command that is not executable (a hub command) and
            // there is no matching subcommand to delegate to.
            throw CommandException(
                translation("liquidbounce.commandManager.invalidUsage", tokens.first()),
                usageInfo = buildUsage(context)
            )
        }

        executor.run(executorContext)
    }

    /**
     * Builds the list of "did you mean" hints for unknown commands, sorted by
     * Levenshtein distance to the typed command name.
     */
    private fun unknownCommandHints(argument: String): List<Component> {
        val mainNodes = mainCommandNodes

        if (mainNodes.isEmpty() || GlobalSettings.hintCount == 0) {
            return emptyList()
        }

        return mainNodes.sortedBy { node ->
            var distance = levenshtein(argument, node.name)
            val aliases = rootCommandNodes.filter { it.redirect === node }
            if (aliases.isNotEmpty()) {
                distance = min(
                    distance,
                    aliases.minOf { levenshtein(argument, it.name) }
                )
            }
            distance
        }.take(GlobalSettings.hintCount).map { node ->
            val aliases = rootCommandNodes.filter { it.redirect === node }.map { it.name }
            if (aliases.isEmpty()) {
                node.name.asPlainText()
            } else {
                net.ccbluex.liquidbounce.utils.text.textOf(
                    node.name.asPlainText(),
                    " (".asPlainText(ChatFormatting.DARK_GRAY),
                    aliases.joinToText(", ".asPlainText(ChatFormatting.DARK_GRAY)),
                    ")".asPlainText(ChatFormatting.DARK_GRAY),
                )
            }
        }
    }

    /**
     * Builds the usage lines for a command context, based on the Brigadier tree
     * ([CommandDispatcher.getSmartUsage]).
     */
    private fun buildUsage(context: com.mojang.brigadier.context.CommandContext<ClientCommandSource>): List<Component> {
        val lastNode = context.nodes.lastOrNull()?.node ?: return emptyList()
        val commandPath = context.nodes.joinToString(" ") { it.node.name }

        return getDispatcher().getSmartUsage(lastNode, ClientCommandSource)
            .values
            .map { usage -> "$commandPath $usage".asPlainText() }
    }

    /**
     * Tokenizes the [line].
     *
     * For example: `.friend add "Senk Ju"` -> [[`.friend`, `add`, `Senk Ju`]]
     *
     * @return A pair of the tokenized command and the starting indices of the tokens
     */
    fun tokenizeCommand(line: String): TokenizationResult {
        val output = ArrayList<String>()
        val outputIndices = IntArrayList()
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
                '"' -> {
                    quote = !quote
                    stringBuilder.append(c) // Don't throw quotes out
                }
                ' ' if !quote -> {
                    // Is the buffer not empty? Also ignore stuff like .friend   add SenkJu
                    if (stringBuilder.isNotBlank()) {
                        output.add(stripOuterQuotes(stringBuilder))

                        // Reset string buffer
                        stringBuilder.setLength(0)
                        outputIndices.add(idx)
                    }
                }
                else -> stringBuilder.append(c)
            }
        }

        // Is there something left in the buffer?
        if (stringBuilder.isNotBlank()) output.add(stripOuterQuotes(stringBuilder))

        return TokenizationResult(output, outputIndices)
    }

    data class TokenizationResult(val tokens: List<String>, val tokenStartIndices: IntList)

    private fun stripOuterQuotes(token: CharSequence): String {
        if (token.length >= 2 && token.startsWith('"') && token.endsWith('"')) {
            return token.substring(1, token.length - 1)
        }
        return token.toString()
    }

    fun autoComplete(origCmd: String, start: Int): CompletableFuture<Suggestions> {
        if (HideAppearance.isDestructed) {
            return Suggestions.empty()
        }

        if (start < GlobalSettings.prefix.length) {
            return Suggestions.empty()
        }

        try {
            val body = origCmd.substring(GlobalSettings.prefix.length, start)
            val tokens = tokenizeCommand(body).tokens
            val pathTokenCount = resolvePathTokenCount(tokens)

            val lowered = lowercaseCommandPath(body, pathTokenCount)
            val dispatcher = getDispatcher()
            val parse = dispatcher.parse(StringReader(lowered), ClientCommandSource)

            return dispatcher.getCompletionSuggestions(parse, lowered.length)
                .thenApply { suggestions -> shiftSuggestionRanges(suggestions, GlobalSettings.prefix.length) }
        } catch (e: Exception) {
            logger.error("Failed to supply autocompletion suggestions for '$origCmd'", e)

            return Suggestions.empty()
        }
    }

    /**
     * Translates suggestion ranges from the command body (without prefix) back into
     * the full input string (with prefix), which is what the Minecraft GUI expects.
     */
    private fun shiftSuggestionRanges(suggestions: Suggestions, offset: Int): Suggestions {
        if (offset == 0) {
            return suggestions
        }

        val shifted = suggestions.list.map { suggestion ->
            Suggestion(
                suggestion.range.offset(offset),
                suggestion.text
            )
        }

        return Suggestions(
            suggestions.range.offset(offset),
            shifted
        )
    }


}

/**
 * Lower-cases only the leading [tokenCount] space-separated tokens of [cmd] (the command
 * path: root command name and subcommand names), leaving all argument values untouched.
 *
 * This preserves the previous case-insensitive behaviour of command paths while keeping
 * parameter values (e.g. `.rename MyItem`) intact.
 */
internal fun lowercaseCommandPath(cmd: String, tokenCount: Int): String {
    if (tokenCount <= 0) {
        return cmd
    }

    val builder = StringBuilder(cmd.length)
    var tokenIndex = 0
    var index = 0

    while (index < cmd.length && tokenIndex < tokenCount) {
        if (cmd[index] == ' ') {
            builder.append(cmd[index])
            index++
            continue
        }

        val start = index
        while (index < cmd.length && cmd[index] != ' ') {
            index++
        }

        builder.append(cmd.substring(start, index).lowercase(Locale.ROOT))
        tokenIndex++
    }

    builder.append(cmd.substring(index))

    return builder.toString()
}

/**
 * Collapses runs of whitespace outside of quoted strings into a single space, mirroring
 * the previous tokenizer behaviour that ignored repeated spaces (`.cmd a   b` was parsed
 * as two tokens). Content inside quotes is preserved verbatim.
 */
internal fun normalizeCommandSpaces(input: String): String {
    val builder = StringBuilder(input.length)
    var inQuote = false
    var escaped = false
    var lastWasSpace = false

    for (c in input) {
        if (escaped) {
            builder.append(c)
            escaped = false
            lastWasSpace = false
            continue
        }

        when {
            c == '\\' -> {
                builder.append(c)
                escaped = true
                lastWasSpace = false
            }
            c == '"' -> {
                inQuote = !inQuote
                builder.append(c)
                lastWasSpace = false
            }
            c == ' ' && !inQuote -> {
                if (!lastWasSpace) {
                    builder.append(c)
                }
                lastWasSpace = true
            }
            else -> {
                builder.append(c)
                lastWasSpace = false
            }
        }
    }

    return builder.toString()
}

/**
 * Maps leftover parse input to a [CommandException], mirroring vanilla
 * [CommandDispatcher.execute]: a single parse exception is surfaced, an empty
 * context range is an unknown command, and leftover tokens after a matched
 * command are invalid usage.
 *
 * Built-in Brigadier argument failures keep their raw message (a translatable
 * component is preserved as-is, so vanilla errors localize through the client
 * language system). When the failing exception carries cursor information (from `createWithContext`),
 * the Brigadier context string (`...input<--[HERE]`) is appended as a dim line,
 * mirroring vanilla's error rendering. Usage is attached here because argument
 * types do not see the command tree at parse time.
 */
internal fun mapParseFailure(
    parse: ParseResults<ClientCommandSource>,
    commandName: String,
    usage: List<Component>,
    unknownHints: List<Component> = emptyList(),
): CommandException {
    val single = parse.exceptions.values.singleOrNull()
    return when {
        single != null -> {
            val message = single.rawMessage.let { it as? Component }?.copy()
                ?: (single.message ?: single.rawMessage.string).asText()
            val context = single.context
            val usageWithErrorContext = if (context != null) {
                usage + translation("liquidbounce.commandManager.errorContext", context)
                    .withStyle(ChatFormatting.DARK_GRAY)
            } else {
                usage
            }
            CommandException(message, usageInfo = usageWithErrorContext)
        }
        parse.context.range.isEmpty ->
            CommandException(
                translation("liquidbounce.commandManager.unknownCommand", commandName),
                usageInfo = unknownHints,
            )
        else ->
            CommandException(
                translation("liquidbounce.commandManager.invalidUsage", commandName),
                usageInfo = usage,
            )
    }
}
