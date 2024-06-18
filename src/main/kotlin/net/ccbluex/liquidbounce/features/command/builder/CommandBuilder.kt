/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandHandler
import net.ccbluex.liquidbounce.features.command.Parameter

class CommandBuilder private constructor(val name: String) {

    private var aliases: Array<out String> = emptyArray()
    private var parameters: ArrayList<Parameter<*>> = ArrayList()
    private var subcommands: ArrayList<Command> = ArrayList()
    private var handler: CommandHandler? = null
    private var executable = true

    companion object {
        fun begin(name: String): CommandBuilder = CommandBuilder(name)
    }

    fun alias(vararg aliases: String): CommandBuilder {
        this.aliases = aliases

        return this
    }

    fun parameter(parameter: Parameter<*>): CommandBuilder {
        this.parameters.add(parameter)

        return this
    }

    fun subcommand(subcommand: Command): CommandBuilder {
        this.subcommands.add(subcommand)

        return this
    }

    fun handler(handler: CommandHandler): CommandBuilder {
        this.handler = handler

        return this
    }

    /**
     * If a command is marked as a hub command, it is impossible to execute it.
     *
     * For example: <code>.friend</code>
     *
     * The command _friend_ would not be executable since it just acts as a
     * hub for its subcommands
     */
    fun hub(): CommandBuilder {
        this.executable = false

        return this
    }

    fun build(): Command {
        require(executable || this.handler == null) {
            "The command is marked as not executable (hub), but no handler was specified"
        }
        require(!executable || this.handler != null) {
            "The command is marked as executable, but no handler was specified."
        }

        var wasOptional = false
        var wasVararg = false

        for (x in this.parameters) {
            require(!x.required || !wasOptional) {
                "Optional parameters are only allowed at the end"
            }
            require(!x.required || !wasVararg) {
                "VarArgs are only allowed at the end"
            }

            wasOptional = !x.required
            wasVararg = x.vararg
        }

        return Command(
            this.name,
            this.aliases,
            this.parameters.toArray(emptyArray()),
            this.subcommands.toArray(
                emptyArray()
            ),
            executable,
            this.handler
        )
    }

}
