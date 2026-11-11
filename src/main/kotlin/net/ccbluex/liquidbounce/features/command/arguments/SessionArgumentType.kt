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
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.features.session.ClientSession
import net.ccbluex.liquidbounce.features.session.SessionManager
import java.util.concurrent.CompletableFuture

/**
 * An open session, named by the account it plays as. Accounts can be open more than once,
 * so the id of the session works as well.
 */
object SessionArgumentType : ArgumentType<ClientSession> {

    override fun parse(reader: StringReader): ClientSession {
        val name = reader.readClientString()

        return SessionManager.sessions.firstOrNull { it.user.name.equals(name, true) }
            ?: name.toIntOrNull()?.let(SessionManager::byId)
            ?: throw CommandErrors.NO_SUCH_SESSION.createWithContext(reader, name)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val prefix = builder.remaining

        SessionManager.sessions.map { it.user.name }
            .filter { it.startsWith(prefix, true) }
            .forEach(builder::suggest)

        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = listOf("Notch", "1")

}
