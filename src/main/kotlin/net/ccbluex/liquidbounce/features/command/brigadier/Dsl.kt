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
package net.ccbluex.liquidbounce.features.command.brigadier

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.context.StringRange
import com.mojang.brigadier.tree.LiteralCommandNode

fun StringRange.offset(offset: Int): StringRange {
    return StringRange(this.start + offset, this.end + offset)
}

/**
 * Thin aliases that fix the source type of the Brigadier builders to [ClientCommandSource],
 * mirroring the convenience factories of Minecraft's `Commands` class
 * (`Commands.literal` / `Commands.argument`).
 */
fun literal(name: String): LiteralArgumentBuilder<ClientCommandSource> =
    LiteralArgumentBuilder.literal<ClientCommandSource>(name)

fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<ClientCommandSource, T> =
    RequiredArgumentBuilder.argument<ClientCommandSource, T>(name, type)

/**
 * Type-safe argument access, mirroring `ctx.getArgument(name, clazz)` as used by the
 * typed getters in vanilla commands (e.g. `ItemArgument.getItem(ctx, "item")`).
 */
inline fun <reified T> CommandContext<ClientCommandSource>.arg(name: String): T =
    getArgument(name, T::class.java)

/**
 * The context whose [CommandContext.command] should run for this parse.
 *
 * A redirecting alias copies the target's command onto the alias node so a bare alias
 * still executes. Brigadier then hangs that command on the **outer** context and puts
 * any remaining parse (arguments, subcommands) on a **child**. [CommandContext.getArgument]
 * does not look at children, so stopping at the first `command != null` would run the
 * alias layer with empty arguments, or the parent handler instead of a subcommand.
 *
 * The deepest context that actually has a command matches the consumed input. A bare
 * alias has no child and lands on the copied command.
 */
fun <S> CommandContext<S>.deepestExecutableContext(): CommandContext<S>? {
    var executable: CommandContext<S>? = null
    var current: CommandContext<S>? = this
    while (current != null) {
        if (current.command != null) {
            executable = current
        }
        current = current.child
    }
    return executable
}

/**
 * Registers [aliases] as redirecting literal nodes that share the subtree of [mainNode].
 *
 * This mirrors the alias handling of the legacy meta-model (an alias executes the exact
 * same command tree) while keeping a single copy of the tree in memory. The main node's
 * `requires` predicate is copied onto each alias so gating (e.g. `requiresIngame`) is not
 * bypassed through a redirect.
 */
internal fun CommandDispatcher<ClientCommandSource>.registerAliases(
    mainNode: LiteralCommandNode<ClientCommandSource>,
    aliases: Iterable<String>,
) {
    aliases.forEach { alias -> root.addChild(redirectingAlias(mainNode, alias)) }
}

/**
 * A sibling literal that redirects to [mainNode], copying `requires` and a no-argument
 * `command` so gating is not bypassed and a bare alias still executes.
 */
fun redirectingAlias(
    mainNode: LiteralCommandNode<ClientCommandSource>,
    alias: String,
): LiteralCommandNode<ClientCommandSource> {
    val builder = literal(alias)
        .requires(mainNode.requirement)
    // Copy the command so a no-argument alias still executes. Remaining input after
    // the alias is parsed as a child; [deepestExecutableContext] runs that child so
    // arguments and subcommands still bind.
    mainNode.command?.let { builder.executes(it) }
    return builder.redirect(mainNode).build()
}
