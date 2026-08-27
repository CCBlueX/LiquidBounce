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

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.KAsyncCommand
import net.ccbluex.liquidbounce.features.command.arguments.MultiTaggedArgumentType

/** Internal command-tree node compiled into a Brigadier builder. */
@CommandDsl
internal sealed interface CmdNode {

    fun toBuilder(): ArgumentBuilder<ClientCommandSource, *>

    fun attachTo(parent: LiteralArgumentBuilder<ClientCommandSource>) {
        parent.then(toBuilder().build())
    }
}

/**
 * One argument inside a [CmdArgChain]: its handle (name + type + default), whether a
 * value must be provided and an optional suggestion provider.
 */
internal class CmdArgElement(
    val handle: CmdArg<*>,
    val required: Boolean,
    val suggests: SuggestionProvider<ClientCommandSource>? = null,
) : ChainElement

/**
 * An argument chain attached to a command literal: a linear sequence of arguments ending
 * in exactly one executor declared through [CmdChainScope.exec]/[CmdChainScope.execSuspend].
 *
 * Compiles to nested Brigadier argument builders; every node whose remaining tail is
 * optional receives the shared executor, so omitted trailing values resolve to defaults.
 */
internal class CmdArgChain(
    private val elements: List<ChainElement>,
) : CmdNode {

    init {
        validateChain(elements)
    }

    private fun arg(index: Int): CmdArgElement = requireNotNull(argOrNull(index))

    private fun argOrNull(index: Int): CmdArgElement? =
        if (index >= 0 && index < elements.size - 1) elements[index] as CmdArgElement else null

    /**
     * True when the literal itself may execute this chain's leaf, which is only possible
     * when every argument in the chain is optional.
     */
    internal fun canExecuteAtParent(): Boolean =
        !arg(0).required

    /** The executor declared as the chain's validated leaf. */
    internal fun leafExecutor(): Command<ClientCommandSource> = leafExecutorOf(elements.last())

    override fun toBuilder(): ArgumentBuilder<ClientCommandSource, *> =
        buildFrom(from = 0)

    private fun buildFrom(from: Int): ArgumentBuilder<ClientCommandSource, *> {
        val node = arg(from)

        val argBuilder = argument(node.handle.name, node.handle.type)
        node.suggests?.let { argBuilder.suggests(it) }

        // The input may end at this depth when every deeper argument is optional; the
        // executor then runs with defaults for the omitted tail. A required argument
        // further down forces the input to continue instead.
        val next = argOrNull(from + 1)
        if (next == null || !next.required) {
            argBuilder.executes(leafExecutor())
        }
        if (next != null) {
            // Attach the continuation through the raw node so `executes` set above is not
            // overwritten by the child builder's own (empty) command during build().
            argBuilder.then(buildFrom(from + 1).build())
        }

        return argBuilder
    }

}

private fun validateChain(elements: List<ChainElement>) {
    val executorCount = elements.count { it is CmdExecutorSpec }
    check(executorCount == 1) {
        "Argument chain must declare exactly one executor"
    }
    check(elements.last() is CmdExecutorSpec) {
        "Argument chain executor must be the final declaration"
    }

    var optionalSeen = false
    val arguments = elements.filterIsInstance<CmdArgElement>()
    arguments.forEach { element ->
        if (optionalSeen) {
            check(!element.required) {
                "Required argument '${element.handle.name}' cannot follow an optional argument"
            }
        }
        optionalSeen = optionalSeen || !element.required
    }

    arguments.forEachIndexed { index, element ->
        check(!isGreedy(element.handle.type) || index == arguments.lastIndex) {
            "Greedy argument '${element.handle.name}' must be the last argument of its chain"
        }
    }
}

/**
 * An element of one argument chain: an argument or the executable leaf.
 */
internal sealed interface ChainElement

/** A nested literal compiled as one Brigadier literal builder, plus redirecting aliases. */
internal class CmdLiteralNode(
    private val name: String,
    private val path: String,
    private val aliases: List<String>,
    private val block: CmdLiteralScope.() -> Unit,
) : CmdNode {

    init {
        check(name.isNotBlank() && aliases.none { it.isBlank() }) {
            "Command node name and aliases must be not blank"
        }
    }

    override fun toBuilder(): LiteralArgumentBuilder<ClientCommandSource> =
        CmdLiteralScope(name, path).apply(block).buildLiteral()

    override fun attachTo(parent: LiteralArgumentBuilder<ClientCommandSource>) {
        val main = toBuilder().build()
        parent.then(main)
        aliases.forEach { alias -> parent.then(redirectingAlias(main, alias)) }
    }
}

/**
 * A command executor declared by either [CmdLiteralScope.exec] or
 * [CmdChainScope.exec].
 */
internal class CmdExecutorSpec(@JvmField val command: Command<ClientCommandSource>) : ChainElement {
    constructor(allowParallel: Boolean, handler: KAsyncCommand.Handler<ClientCommandSource>) :
        this(CommandExecutor.wrapSuspend(allowParallel, handler))
}

private fun leafExecutorOf(leaf: ChainElement): Command<ClientCommandSource> =
    (leaf as? CmdExecutorSpec)?.command
        ?: throw IllegalStateException("Argument chain must end with exec/execSuspend")

/**
 * Heuristic greediness check for the chain-tail validation: only known greedy types are
 * recognized. A custom [ArgumentType] that consumes the whole remaining input is NOT
 * detected here - such types must be declared as the last argument of their chain
 * manually (documented on [CmdLiteralScope.argument]).
 */
private fun isGreedy(type: ArgumentType<*>): Boolean = when (type) {
    is StringArgumentType -> type.type == StringArgumentType.StringType.GREEDY_PHRASE
    is MultiTaggedArgumentType<*> -> true
    else -> false
}
