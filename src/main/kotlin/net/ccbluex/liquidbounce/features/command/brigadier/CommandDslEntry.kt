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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode

/**
 * Builds a literal directly as a Brigadier builder using the typed command DSL.
 * Register the returned builder through [com.mojang.brigadier.CommandDispatcher.register].
 */
fun literal(
    name: String,
    block: CmdLiteralScope.() -> Unit,
): LiteralArgumentBuilder<ClientCommandSource> =
    CmdLiteralScope(name).apply(block).buildLiteral()

/**
 * Builds a literal directly as a Brigadier builder using the typed command DSL (shortcut)
 * Register the returned builder through [com.mojang.brigadier.CommandDispatcher.register].
 */
fun CommandDispatcher<ClientCommandSource>.register(
    name: String,
    aliases: Iterable<String> = emptyList(),
    block: CmdLiteralScope.() -> Unit,
): LiteralCommandNode<ClientCommandSource> {
    val main = register(literal(name, block))
    registerAliases(main, aliases)
    return main
}

