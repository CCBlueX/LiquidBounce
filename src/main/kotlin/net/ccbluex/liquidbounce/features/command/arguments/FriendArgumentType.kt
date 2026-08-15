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
package net.ccbluex.liquidbounce.features.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.features.misc.FriendManager
import java.util.concurrent.CompletableFuture

/**
 * Resolves a friend by name at parse time: unknown names fail during parse instead of
 * inside the handler (mirrors [PlayerInfoArgumentType]), and every consumer receives the
 * same [FriendManager.Friend] instead of re-implementing the lookup.
 *
 * Suggestions are the names on the friend list.
 */
object FriendArgumentType : ArgumentType<FriendManager.Friend> {

    override fun parse(reader: StringReader): FriendManager.Friend {
        val name = reader.readClientString()

        return FriendManager.friends.firstOrNull { it.name.equals(name, true) }
            ?: throw CommandErrors.NOT_A_FRIEND.createWithContext(reader, name)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val prefix = builder.remaining

        FriendManager.friends.filter { it.name.startsWith(prefix, true) }
            .forEach { builder.suggest(it.name) }

        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = listOf("SenkJu")

}
