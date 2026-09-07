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

package net.ccbluex.liquidbounce.script.bindings.features

import com.mojang.brigadier.Command
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.argument
import net.ccbluex.liquidbounce.features.command.brigadier.literal
import net.ccbluex.liquidbounce.features.command.brigadier.redirectingAlias
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.readClientString
import net.ccbluex.liquidbounce.features.command.arguments.readGreedyTokens
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.asArray
import org.graalvm.polyglot.Value

/**
 * A script-provided validator rejected the input; `%s` is the error detail returned by
 * the script, kept as a dynamic passthrough (script messages are not translatable).
 */
private val SCRIPT_VALIDATION_FAILED = DynamicCommandExceptionType { detail ->
    translation("liquidbounce.commandManager.scriptValidationFailed", detail)
}

/**
 * Builds a Brigadier command tree from a JavaScript command object.
 *
 * The object still uses `name`, `aliases`, `subcommands`, `parameters`, and `onExecute`.
 * `hub` is unused: a command without `onExecute` is only a parent for subcommands.
 * A parameter whose `required` is missing, `undefined`, or `null` is optional, matching
 * the previous script contract (`if (parameter.required)`).
 * Parameter chains follow the same optional-tail rules as the in-tree DSL
 * ([net.ccbluex.liquidbounce.features.command.brigadier.CmdArgChain]): omitting a
 * trailing optional still runs `onExecute`. Nested `aliases` are redirecting sibling
 * literals of that subcommand, not extra root commands.
 *
 * - A `vararg` without a custom `validate` is a single greedy string and reaches
 *   `onExecute` as one `String`. A `vararg` with `validate` keeps per-token parsing
 *   and arrives as an `Array`.
 * - `getCompletions` is adapted into a Brigadier [com.mojang.brigadier.suggestion.SuggestionProvider].
 */
class ScriptCommandBuilder(private val commandObject: Value) {

    /**
     * Returns the main command node plus one redirecting literal per alias; all of them
     * must be registered (and later unregistered by name) on the [net.ccbluex.liquidbounce.features.command.CommandManager].
     */
    fun build(): List<LiteralCommandNode<ClientCommandSource>> {
        return createCommand(commandObject, emptyList()).allNodes
    }

    private data class BuiltCommand(
        val mainNode: LiteralCommandNode<ClientCommandSource>,
        val allNodes: List<LiteralCommandNode<ClientCommandSource>>,
    )

    private fun createCommand(commandObject: Value, path: List<String>): BuiltCommand {
        val name = commandObject.getMember("name").asString()
        val aliases = if (commandObject.hasMember("aliases")) {
            commandObject.getMember("aliases").asArray<String>()
        } else {
            emptyArray()
        }

        val builder = literal(name)
        val commandPath = path + name

        if (commandObject.hasMember("subcommands")) {
            val subcommands = commandObject.getMember("subcommands").asArray<Value>()
            validateSubcommandNames(subcommands)

            for (subcommand in subcommands) {
                createCommand(subcommand, commandPath).allNodes.forEach { builder.then(it) }
            }
        }

        val parameters = if (commandObject.hasMember("parameters")) {
            commandObject.getMember("parameters").asArray<Value>().asList()
        } else {
            emptyList()
        }
        validateParameters(parameters)

        if (commandObject.hasMember("onExecute")) {
            val handler = commandObject.getMember("onExecute")
            val execution = Command<ClientCommandSource> { ctx ->
                val args = parameters.mapNotNull { param -> collectArgument(param, ctx) }.toTypedArray()

                @Suppress("SpreadOperator")
                handler.execute(*args)
                1
            }

            if (parameters.isEmpty()) {
                builder.executes(execution)
            } else {
                builder.then(attachParameterChain(parameters, 0, commandPath, execution))
                if (!isRequired(parameters.first())) {
                    builder.executes(execution)
                }
            }
        }

        val mainNode = builder.build()
        val aliasNodes = aliases.map { alias -> redirectingAlias(mainNode, alias) }

        return BuiltCommand(mainNode, listOf(mainNode) + aliasNodes)
    }

    /**
     * Builds the argument chain, one nested argument node per parameter.
     *
     * A node is executable when the remainder of the chain is optional, matching
     * [net.ccbluex.liquidbounce.features.command.brigadier.CmdArgChain].
     */
    @Suppress("detekt:CognitiveComplexMethod", "detekt:SwallowedException")
    private fun attachParameterChain(
        parameters: List<Value>,
        index: Int,
        path: List<String>,
        execution: Command<ClientCommandSource>,
    ): ArgumentBuilder<ClientCommandSource, *> {
        val param = parameters[index]
        val isLast = index == parameters.size - 1
        val vararg = param.hasMember("vararg") && param.getMember("vararg").asBoolean()
        val hasValidate = param.hasMember("validate")
        val name = param.getMember("name").asString()

        val argumentBuilder = if (hasValidate) {
            val validator = param.getMember("validate")
            val type: ArgumentType<*> = if (vararg) {
                ScriptValidatedMultiValueArgumentType(name, validator)
            } else {
                ScriptValidatedArgumentType(name, validator)
            }
            argument(name, type)
        } else if (vararg) {
            // Per user decision: a vararg without custom validation is a single greedy string.
            argument(name, StringArgumentType.greedyString())
        } else {
            argument(name, ClientStringArgumentType.word())
        }

        if (param.hasMember("getCompletions")) {
            val completions = param.getMember("getCompletions")
            val parsedArgumentNames = parameters.subList(0, index).map { it.getMember("name").asString() }

            argumentBuilder.suggests { ctx, builder ->
                val args = buildList {
                    addAll(path)
                    for (argumentName in parsedArgumentNames) {
                        val value = try {
                            ctx.getArgument(argumentName, Any::class.java)
                        } catch (e: IllegalArgumentException) {
                            // Missing (optional) parameter: all trailing arguments are absent
                            break
                        }

                        if (value is Array<*>) {
                            addAll(value.map { it.toString() })
                        } else {
                            add(value.toString())
                        }
                    }
                    if (builder.remaining.isNotEmpty()) {
                        add(builder.remaining)
                    }
                }

                completions.execute(builder.remaining, args).asArray<String>().forEach(builder::suggest)
                builder.buildFuture()
            }
        }

        if (!isLast) {
            argumentBuilder.then(attachParameterChain(parameters, index + 1, path, execution))
        }

        val next = parameters.getOrNull(index + 1)
        if (next == null || !isRequired(next)) {
            argumentBuilder.executes(execution)
        }

        return argumentBuilder
    }

    private fun isRequired(param: Value): Boolean {
        if (!param.hasMember("required")) {
            return false
        }
        val value = param.getMember("required")
        // JS `undefined` / `null` are both `isNull` in Graal; treat them as false.
        return !value.isNull && value.asBoolean()
    }

    private fun validateParameters(parameters: List<Value>) {
        var optionalSeen = false
        val names = hashSetOf<String>()
        parameters.forEachIndexed { index, param ->
            val name = param.getMember("name").asString()
            // Brigadier merges argument nodes by exact name, so a duplicate parameter name
            // would silently overwrite the previous node and its suggestions.
            check(names.add(name)) {
                "Duplicate parameter name '$name'"
            }
            val required = isRequired(param)
            check(!optionalSeen || !required) {
                "Required parameter '$name' cannot follow an optional parameter"
            }
            optionalSeen = optionalSeen || !required

            val vararg = param.hasMember("vararg") && param.getMember("vararg").asBoolean()
            check(!vararg || index == parameters.lastIndex) {
                "Vararg parameter '$name' must be the last parameter"
            }
        }
    }

    private fun validateSubcommandNames(subcommands: Array<Value>) {
        val taken = hashSetOf<String>()
        subcommands.forEach { subcommand ->
            val name = subcommand.getMember("name").asString()
            check(taken.add(name)) {
                "Duplicate subcommand '$name'"
            }

            if (subcommand.hasMember("aliases")) {
                subcommand.getMember("aliases").asArray<String>().forEach { alias ->
                    check(taken.add(alias)) {
                        "Subcommand alias '$alias' collides with another subcommand of the same parent"
                    }
                }
            }
        }
    }

    /**
     * Reads a parsed argument value from the context; returns null when the (optional)
     * parameter was omitted.
     */
    @Suppress("detekt:SwallowedException")
    private fun collectArgument(param: Value, ctx: CommandContext<ClientCommandSource>): Any? {
        val name = param.getMember("name").asString()

        return try {
            ctx.getArgument(name, Any::class.java)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun <T> toObject(v: Value): T {
        return if (v.isHostObject) {
            v.asHostObject()
        } else {
            v as T
        }
    }

    /**
     * Validates a single token through the script-provided `validate` function.
     */
    private inner class ScriptValidatedArgumentType(
        private val parameterName: String,
        private val validator: Value,
    ) : ArgumentType<Any> {

        override fun parse(reader: StringReader): Any {
            val sourceText = reader.readClientString()

            val result = validator.execute(sourceText)
            return if (result.getMember("accept").asBoolean()) {
                toObject<Any>(result.getMember("value"))
            } else {
                throw SCRIPT_VALIDATION_FAILED.createWithContext(
                    reader,
                    result.getMember("error").asString(),
                )
            }
        }
    }

    /**
     * Greedily consumes all remaining tokens, validating each one through the
     * script-provided `validate` function. The result is an [Array] of parsed values.
     */
    private inner class ScriptValidatedMultiValueArgumentType(
        private val parameterName: String,
        private val validator: Value,
    ) : ArgumentType<Array<Any>> {

        override fun parse(reader: StringReader): Array<Any> =
            readGreedyTokens(reader) { sourceText ->
                val result = validator.execute(sourceText)
                if (result.getMember("accept").asBoolean()) {
                    toObject<Any>(result.getMember("value"))
                } else {
                    throw SCRIPT_VALIDATION_FAILED.createWithContext(
                        reader,
                        result.getMember("error").asString(),
                    )
                }
            }.toTypedArray()
    }

}
