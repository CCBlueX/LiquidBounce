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
import net.minecraft.client.player.AbstractClientPlayer
import java.util.concurrent.CompletableFuture

/**
 * Another player in the loaded world, by name, whether or not the tab list shows them.
 *
 * Suggestions are the names of the other players in the world.
 */
object WorldPlayerArgumentType : ArgumentType<AbstractClientPlayer> {

    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): AbstractClientPlayer {
        val name = reader.readClientString()
        return otherPlayers().firstOrNull { it.gameProfile.name.equals(name, true) }
            ?: throw CommandErrors.NO_SUCH_PLAYER.createWithContext(reader, name)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val prefix = builder.remaining
        otherPlayers().map { it.gameProfile.name }
            .filter { it.startsWith(prefix, true) }
            .forEach(builder::suggest)

        return builder.buildFuture()
    }

    private fun otherPlayers(): List<AbstractClientPlayer> {
        val mc = mc() ?: return emptyList()
        val player = mc.player
        return mc.level?.players()?.filter { it !== player }.orEmpty()
    }

    override fun getExamples(): Collection<String> = listOf("SenkJu")

}
