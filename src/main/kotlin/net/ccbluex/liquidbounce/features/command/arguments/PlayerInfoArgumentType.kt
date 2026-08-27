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
package net.ccbluex.liquidbounce.features.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.utils.client.NullableBypass.mc
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

/**
 * Resolves a tab-list player by name at parse time: invalid names fail during parse instead of inside the handler,
 * and every consumer receives the same [PlayerInfo] instead of re-implementing the lookup.
 *
 * Suggestions are the online player names from the command source.
 */
object PlayerInfoArgumentType : ArgumentType<PlayerInfo> {

    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): PlayerInfo {
        val name = reader.readClientString()
        return mc()?.connection?.onlinePlayers?.firstOrNull { it.profile.name.equals(name, true) }
            ?: throw CommandErrors.NO_SUCH_PLAYER.createWithContext(reader, name)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val names = (context.source as? SharedSuggestionProvider)?.onlinePlayerNames ?: emptyList()
        return SharedSuggestionProvider.suggest(names, builder)
    }

    override fun getExamples(): Collection<String> = listOf("SenkJu")

}
