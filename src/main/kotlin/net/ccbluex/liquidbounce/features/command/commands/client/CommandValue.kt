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
package net.ccbluex.liquidbounce.features.command.commands.client

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.util.Locale
import java.util.TreeSet

/**
 * Value Command
 *
 * Allows you to change values by key path.
 */
@Suppress("SwallowedException")
object CommandValue : CommandRegistrar {
    @Suppress("detekt:LongMethod", "detekt:ThrowsCount")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("value") {
            literal("set") {
                argument(
                    "path",
                    ClientStringArgumentType.word(),
                    keyPathSuggestionProvider(ConfigSystem::valueKeySequence),
                ) { path ->
                    argument(
                        "value",
                        ClientStringArgumentType.string(),
                        valueSuggestionProvider,
                    ) { value ->
                        exec { ctx ->
                            val valueKey = ctx.get(path)
                            val valueString = ctx.get(value)

                            val configValue = ConfigSystem.findValueByKey(valueKey)
                                ?: throw CommandException(t("set.valueNotFound", valueKey))

                            try {
                                configValue.setByString(valueString)
                                ModuleClickGui.sync()
                            } catch (e: Exception) {
                                throw CommandException(t("set.valueError", valueKey, e.message ?: ""))
                            }

                            chat(
                                regular(t("set.success", variable(valueKey))),
                                metadata = MessageMetadata(id = "CValue#success${valueKey}")
                            )
                            1
                        }
                    }
                }
            }
            literal("reset") {
                argument(
                    "path",
                    ClientStringArgumentType.word(),
                    keyPathSuggestionProvider(ConfigSystem::valueKeySequence),
                ) { path ->
                    exec { ctx ->
                        val valueKey = ctx.get(path)

                        val configValue = ConfigSystem.findValueByKey(valueKey)
                            ?: throw CommandException(t("reset.valueNotFound", valueKey))

                        configValue.restore()
                        ModuleClickGui.sync()
                        chat(
                            regular(t("reset.resetSuccess", variable(valueKey))),
                            metadata = MessageMetadata(id = "CValue#reset${valueKey}")
                        )
                        1
                    }
                }
            }
            literal("reset-all") {
                argument(
                    "valueGroupPath",
                    ClientStringArgumentType.word(),
                    keyPathSuggestionProvider(ConfigSystem::valueGroupsKeySequence),
                ) { valueGroupPath ->
                    exec { ctx ->
                        val valueGroupKey = ctx.get(valueGroupPath)
                        val valueGroup = ConfigSystem.findValueGroupByKey(valueGroupKey)
                            ?: throw CommandException(t("reset-all.valueGroupNotFound", valueGroupKey))

                        valueGroup.collectValuesRecursively()
                            .filter { !it.name.equals("Bind", true) }
                            .forEach { it.restore() }
                        ModuleClickGui.sync()
                        chat(
                            regular(t("reset-all.resetAllSuccess", variable(valueGroupKey))),
                            metadata = MessageMetadata(id = "CValue#resetAll${valueGroupKey}")
                        )
                        1
                    }
                }
            }
        }
    }

    private val valueSuggestionProvider =
        SuggestionProvider<ClientCommandSource> { ctx, builder ->
            val value = ConfigSystem.findValueByKey(ctx.getArgument("path", String::class.java))
                ?: return@SuggestionProvider builder.buildFuture()

            value.valueType.completer.possible(value)
                .filter { it.startsWith(builder.remaining, true) }
                .forEach { builder.suggest(it) }
            builder.buildFuture()
        }

    private fun keyPathSuggestionProvider(
        keyProvider: (String) -> Sequence<String>,
    ): SuggestionProvider<ClientCommandSource> = SuggestionProvider { _, builder ->
        val query = buildKeySegmentQuery(builder.remaining)
        keyProvider(query.prefix)
            .map { it.lowercase(Locale.ROOT) }
            .filter { query.prefix.isBlank() || it.startsWith(query.prefix) }
            .map { it.split('.') }
            .filter { it.size > query.depth }
            .map { it[query.depth] }
            .filter { it.startsWith(query.typed, true) }
            .mapTo(TreeSet(String.CASE_INSENSITIVE_ORDER)) {
                formatSuggestion(query.prefix, it)
            }
            .forEach { builder.suggest(it) }

        builder.buildFuture()
    }

    private data class KeySegmentQuery(
        val prefix: String,
        val typed: String,
        val depth: Int,
    )

    private fun buildKeySegmentQuery(begin: String): KeySegmentQuery {
        val normalizedBegin = begin.lowercase()
        val effectiveBegin = addDefaultPrefixIfMissing(normalizedBegin)
        val (prefix, typed) = splitKeyPrefix(effectiveBegin)
        val depth = countSegments(prefix)
        return KeySegmentQuery(prefix, typed, depth)
    }

    private fun splitKeyPrefix(input: String): Pair<String, String> {
        val endsWithDot = input.endsWith('.')
        val lastDot = input.lastIndexOf('.')
        val prefix = if (lastDot >= 0) input.substring(0, lastDot + 1) else ""
        val typed = if (endsWithDot || lastDot < 0) input.substring(prefix.length) else input.substring(lastDot + 1)
        return prefix to typed
    }

    private fun countSegments(prefix: String): Int {
        return if (prefix.isBlank()) {
            0
        } else {
            prefix.dropLast(1).count { it == '.' } + 1
        }
    }

    private fun formatSuggestion(prefix: String, segment: String): String {
        val suggestion = "$prefix$segment"
        val defaultPrefix = "${ConfigSystem.KEY_PREFIX}."
        return suggestion.removePrefix(defaultPrefix)
    }

    private fun addDefaultPrefixIfMissing(input: String): String {
        val prefix = "${ConfigSystem.KEY_PREFIX}."
        return if (input.startsWith(prefix) || input == ConfigSystem.KEY_PREFIX) {
            input
        } else {
            prefix + input
        }
    }

}
