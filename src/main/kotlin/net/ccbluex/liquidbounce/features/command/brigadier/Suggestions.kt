/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of
 * the License, or (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.command.brigadier

import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

/** Suggests the names of the players currently online (tab list), vanilla-filtered. */
fun <S> onlinePlayers() = suggestions<S> { ClientCommandSource.onlinePlayerNames }

/**
 * Appends all elements from [strings] as suggestion, filtered by the vanilla
 * [SharedSuggestionProvider] matching (case-insensitive prefix / substring).
 */
fun <S> suggestions(vararg strings: String) = SuggestionProvider<S> { _, builder ->
    SharedSuggestionProvider.suggest(strings, builder)
}

/**
 * Appends all elements from [strings] as suggestion, filtered by the vanilla
 * [SharedSuggestionProvider] matching (case-insensitive prefix / substring).
 */
fun <S> suggestions(strings: Iterable<String>) = SuggestionProvider<S> { _, builder ->
    SharedSuggestionProvider.suggest(strings, builder)
}

/**
 * Appends all elements from [strings] as suggestion, filtered by the vanilla
 * [SharedSuggestionProvider] matching (case-insensitive prefix / substring).
 */
fun <S> suggestions(strings: () -> Iterable<String>?) = SuggestionProvider<S> { _, builder ->
    strings()?.let { SharedSuggestionProvider.suggest(it, builder) } ?: builder.buildFuture()
}

/**
 * Appends all elements from [strings] as suggestion.
 * The supplier function is invoked on [executor]; the resulting future completes on the
 * Minecraft main thread, mirroring vanilla where every suggestion future.
 */
fun <S> suggestions(
    executor: Executor,
    strings: Supplier<Iterable<String>?>,
) = SuggestionProvider<S> { _, builder ->
    CompletableFuture.supplyAsync(strings, executor)
        .handle { candidates, error ->
            if (error != null) {
                logger.error("Failed to collect command suggestions", error)
                null
            } else {
                candidates
            }
        }
        .thenApplyAsync({ candidates ->
            candidates?.let { SharedSuggestionProvider.suggest(it, builder) }
            builder.build()
        }, mc)
}
