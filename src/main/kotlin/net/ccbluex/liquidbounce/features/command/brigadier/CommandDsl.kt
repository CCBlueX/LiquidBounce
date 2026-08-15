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
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.fastutil.filterIsInstance
import net.ccbluex.liquidbounce.features.command.KAsyncCommand
import net.ccbluex.liquidbounce.lang.translation
import net.minecraft.network.chat.MutableComponent
import java.util.function.Predicate

internal object NoArgumentDefault

/**
 * Typed handle passed into an `argument` or `optional` continuation.
 *
 * The handle carries everything needed to attach and read one argument: its Brigadier
 * [type], its name and the fallback for omitted optional branches. Handlers retrieve the
 * parsed value through [get] without repeating name or type at the call site.
 *
 * @param T the resolved value type; nullable for optional arguments whose default is `null`
 */
class CmdArg<T> internal constructor(
    internal val name: String,
    internal val type: ArgumentType<T & Any>,
    private val default: Any?,
) {
    /**
     * Reads this argument's parsed value from [ctx], falling back to [default] when the
     * argument was omitted on an optional branch of the tree.
     *
     * The lookup passes [Any] as the raw class so Brigadier skips its assignability check
     * (which compares against the ArgumentType implementation class, not the value class)
     * and returns the parsed value verbatim; the cast to [T] is safe because the handle
     * is created next to its matching [ArgumentType].
     */
    @Suppress("UNCHECKED_CAST")
    internal fun resolve(ctx: CommandContext<ClientCommandSource>): T {
        if (default === NoArgumentDefault) {
            return ctx.getArgument(name, Any::class.java) as T
        }

        return try {
            ctx.getArgument(name, Any::class.java) as T
        } catch (_: IllegalArgumentException) {
            // The argument node was not parsed on this execution branch: optional omitted
            default as T
        }
    }
}

/** Reads a typed DSL argument directly from a Brigadier context. */
fun <T> CommandContext<ClientCommandSource>.get(argument: CmdArg<T>): T = argument.resolve(this)

/**
 * Marker annotation preventing nested DSL scopes from calling members of outer scopes
 * (e.g. attaching arguments to a parent literal from inside a handler block).
 */
@DslMarker
annotation class CommandDsl

/**
 * Shared i18n for [CmdLiteralScope] and [CmdChainScope]; helpers take this as receiver.
 */
interface CmdI18n {
    val path: String

    /**
     * Resolves [key] under the root command's namespace: only the first segment of [path]
     * is used, so [key] must carry the subcommand path itself (e.g. `"clear.noFriends"`
     * for `.friend clear` -> `liquidbounce.command.friend.clear.noFriends`).
     */
    fun t(key: String, vararg args: Any?): MutableComponent =
        translation("liquidbounce.command.${path.substringBefore('.')}.$key", *args)
}

/**
 * Root scope of a command declaration; receives the top-level nodes declared in the
 * block of `literal(...)`.
 *
 * Argument chains are declared inline with [argument]/[optional]; each call opens a
 * nested scope whose block declares the continuation, ending in exactly one executor
 * (`exec`/`execSuspend`) per chain.
 */
@CommandDsl
class CmdLiteralScope internal constructor(
    internal val name: String,
    override val path: String = name,
) : CmdI18n {

    private var requirement: Predicate<ClientCommandSource>? = null

    /** Direct executor of the literal itself (e.g. `.binds` listing all binds). */
    private var executor: CmdExecutorSpec? = null

    /** Nested literals and argument chains declared under this literal. */
    private val children = mutableListOf<CmdNode>()

    /** Gates execution of this command behind [predicate]. */
    fun requires(predicate: Predicate<ClientCommandSource>) {
        requirement = predicate
    }

    /**
     * Declares an executor directly on the literal: the handler runs when the input ends
     * here. Use [execSuspend] for suspending handlers instead.
     */
    fun exec(handler: Command<ClientCommandSource>) {
        check(executor == null) { "Duplicate executor on '$name'" }
        executor = CmdExecutorSpec(handler)
    }

    /**
     * Declares a suspending executor directly on the literal, scheduled like
     * `CommandExecutor.executesSuspend` (progress message and re-entrance guard included).
     */
    fun execSuspend(allowParallel: Boolean = false, handler: KAsyncCommand.Handler<ClientCommandSource>) {
        check(executor == null) { "Duplicate executor on '$name'" }
        executor = CmdExecutorSpec(allowParallel, handler)
    }

    /**
     * Declares a required argument and passes its typed handle into the continuation.
     *
     * If [type] consumes the reader's entire remaining input (greedy, e.g.
     * [com.mojang.brigadier.arguments.StringArgumentType.greedyString]),
     * it must be the last argument of the chain. The DSL only detects greediness for known types
     * ([com.mojang.brigadier.arguments.StringArgumentType] and
     * [net.ccbluex.liquidbounce.features.command.arguments.MultiTaggedArgumentType]);
     * custom greedy argument types are not verified and silently produce an
     * unmatchable tree when placed before other arguments.
     */
    fun <T : Any> argument(
        name: String,
        type: ArgumentType<T>,
        suggests: SuggestionProvider<ClientCommandSource>? = null,
        block: CmdChainScope.ArgContinuation<T>,
    ) {
        val handle = CmdArg<T>(name, type, NoArgumentDefault)
        addArgumentChain(handle, required = true, suggests = suggests, block = block)
    }

    /** Declares a nullable optional argument and passes its typed handle into the continuation. */
    fun <T : Any> optional(
        name: String,
        type: ArgumentType<T>,
        default: Nothing? = null,
        suggests: SuggestionProvider<ClientCommandSource>? = null,
        block: CmdChainScope.ArgContinuation<T?>,
    ) {
        val handle = CmdArg<T?>(name, type, default)
        addArgumentChain(handle, required = false, suggests = suggests, block = block)
    }

    /** Declares an optional argument with a non-null default and typed handle. */
    fun <T : Any> optional(
        name: String,
        type: ArgumentType<T>,
        default: T,
        suggests: SuggestionProvider<ClientCommandSource>? = null,
        block: CmdChainScope.ArgContinuation<T>,
    ) {
        val handle = CmdArg(name, type, default)
        addArgumentChain(handle, required = false, suggests = suggests, block = block)
    }

    /**
     * Declares a nested command literal. [aliases] are extra names registered as
     * redirecting siblings under the same parent (e.g. `.localconfig create` for `save`).
     */
    fun literal(
        name: String,
        aliases: List<String> = emptyList(),
        block: CmdLiteralScope.() -> Unit,
    ) {
        val names = buildList {
            add(name.lowercase())
            aliases.forEach { alias ->
                check(alias.isNotEmpty()) { "Literal '$name' cannot have an empty alias" }
                add(alias.lowercase())
            }
        }
        check(names.size == names.toSet().size) {
            "Literal '$name' has a duplicate name or alias"
        }
        children.add(CmdLiteralNode(name, "$path.$name", aliases, block))
    }

    private fun <T> addArgumentChain(
        handle: CmdArg<T>,
        required: Boolean,
        suggests: SuggestionProvider<ClientCommandSource>?,
        block: CmdChainScope.ArgContinuation<T>,
    ) {
        children.add(
            CmdArgChain(buildList {
                appendArgumentElement(this, handle, required, suggests, path, block)
            })
        )
    }

    /**
     * Compiles this declaration into a Brigadier literal builder.
     *
     * A chain whose arguments are all optional is also executable directly on the literal
     * (input may end before any argument), so the literal inherits that chain's leaf
     * executor. A direct executor and an all-optional chain cannot be declared together.
     */
    internal fun buildLiteral(): LiteralArgumentBuilder<ClientCommandSource> {
        val builder = literal(name)

        val parentExecutableChains = children
            .filterIsInstance<CmdArgChain>(CmdArgChain::canExecuteAtParent)
        check(parentExecutableChains.size <= 1) {
            "Literal '$name' cannot declare multiple all-optional argument chains"
        }

        val hasDirectExecutor = executor != null
        check(!hasDirectExecutor || parentExecutableChains.isEmpty()) {
            "Literal '$name' cannot declare a direct executor and an all-optional argument chain"
        }

        requirement?.let { builder.requires(it) }
        if (executor != null) {
            builder.executes(executor!!.command)
        } else {
            parentExecutableChains.firstOrNull()?.let { builder.executes(it.leafExecutor()) }
        }
        children.forEach { it.attachTo(builder) }

        return builder
    }
}

/**
 * Scope used to append the continuation of one argument chain.
 * Nested scopes share the same backing element list.
 */
@CommandDsl
class CmdChainScope internal constructor(
    private val elements: MutableList<ChainElement>,
    override val path: String,
) : CmdI18n {

    /**
     * Appends a required argument and passes its typed handle into the continuation.
     *
     * See the greedy-argument caveat on [CmdLiteralScope.argument]: it applies here too.
     */
    fun <T : Any> argument(
        name: String,
        type: ArgumentType<T>,
        suggests: SuggestionProvider<ClientCommandSource>? = null,
        block: ArgContinuation<T>,
    ) {
        val handle = CmdArg<T>(name, type, NoArgumentDefault)
        appendArgumentElement(elements, handle, required = true, suggests = suggests, path = path, block = block)
    }

    /**
     * Appends an optional argument. Omitted values are resolved from the declared default;
     * all following arguments must also be optional.
     */
    fun <T : Any> optional(
        name: String,
        type: ArgumentType<T>,
        default: Nothing? = null,
        suggests: SuggestionProvider<ClientCommandSource>? = null,
        block: ArgContinuation<T?>,
    ) {
        val handle = CmdArg<T?>(name, type, default)
        appendArgumentElement(elements, handle, required = false, suggests = suggests, path = path, block = block)
    }

    /** Appends an optional argument with a non-null default. */
    fun <T : Any> optional(
        name: String,
        type: ArgumentType<T>,
        default: T,
        suggests: SuggestionProvider<ClientCommandSource>? = null,
        block: ArgContinuation<T>,
    ) {
        val handle = CmdArg(name, type, default)
        appendArgumentElement(elements, handle, required = false, suggests = suggests, path = path, block = block)
    }

    /** Declares the executable leaf of this chain. */
    fun exec(handler: Command<ClientCommandSource>) {
        elements.add(CmdExecutorSpec(handler))
    }

    /** Declares a suspending executable leaf of this chain. */
    fun execSuspend(allowParallel: Boolean = false, handler: KAsyncCommand.Handler<ClientCommandSource>) {
        elements.add(CmdExecutorSpec(allowParallel, handler))
    }

    fun interface ArgContinuation<T> {
        operator fun CmdChainScope.invoke(arg: CmdArg<T>)
    }
}

private fun <T> appendArgumentElement(
    elements: MutableList<ChainElement>,
    handle: CmdArg<T>,
    required: Boolean,
    suggests: SuggestionProvider<ClientCommandSource>?,
    path: String,
    block: CmdChainScope.ArgContinuation<T>,
) {
    elements.add(CmdArgElement(handle, required, suggests))
    with(block) { CmdChainScope(elements, path).invoke(handle) }
}
