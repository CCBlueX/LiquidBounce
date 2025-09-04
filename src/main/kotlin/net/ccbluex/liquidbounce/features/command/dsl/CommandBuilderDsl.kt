/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.command.dsl

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder

@DslMarker
annotation class CommandBuilderDsl

inline fun buildCommand(name: String, block: CommandBuilder.() -> Unit): Command {
    return CommandBuilder.begin(name).apply(block).build()
}

inline fun commandFactory(
    name: String,
    crossinline block: CommandBuilder.() -> Unit,
): Command.Factory {
    return Command.Factory { buildCommand(name, block) }
}

context(context: Command.Handler.Context)
fun <T : Any> Parameter<T>.cast(): T {
    requireOwner(context.command)
    @Suppress("UNCHECKED_CAST")
    return context.args.getOrNull(index) as T?
        ?: requireNotNull(this.default) { "Parameter '$name' has no default value." }
}

context(context: Command.Handler.Context)
fun <T : Any> Parameter<T>.castVararg(): Array<out T> {
    requireOwner(context.command)
    @Suppress("UNCHECKED_CAST")
    return context.args[index] as Array<T>
}

context(context: Command.Handler.Context)
fun <T : Any> Parameter<T>.castNotRequired(): T? {
    requireOwner(context.command)
    @Suppress("UNCHECKED_CAST")
    return context.args.getOrNull(index) as T?
}

context(context: Command.Handler.Context)
fun <T : Any> Parameter<T>.castNotRequired(default: T): T {
    requireOwner(context.command)
    @Suppress("UNCHECKED_CAST")
    return context.args.getOrNull(index) as T? ?: default
}

private fun Parameter<*>.requireOwner(command: Command) {
    require(command.parameters[index] === this && command === this.command) {
        "Parameter is not part of command '${command.name}'"
    }
}
