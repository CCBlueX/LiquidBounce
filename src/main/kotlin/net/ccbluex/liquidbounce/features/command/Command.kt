/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.command

import java.util.*
import kotlin.collections.ArrayList

typealias CommandHandler = (Array<Any>) -> Boolean

class Command(
    val name: String,
    val aliases: Array<out String>,
    val description: String?,
    val parameters: Array<Parameter<*>>,
    val subcommands: Array<Command>,
    val executable: Boolean,
    val handler: CommandHandler?,
    var parentCommand: Command? = null
) {

    init {
        subcommands.forEach {
            if (it.parentCommand != null)
                throw IllegalStateException("Subcommand already has parent command")

            it.parentCommand = this
        }
    }

    /**
     * Returns the name of the command with the name of it's parent classes
     */
    fun getFullName(): String {
        val parent = this.parentCommand

        return if (parent == null)
            this.name
        else
            parent.getFullName() + " " + this.name
    }

    /**
     * Returns the formatted usage information of this command
     *
     * e.g. <code>command_name subcommand_name <required_arg> [[<optional_vararg>]...</code>
     */
    fun usage(): List<String> {
        val output = ArrayList<String>()

        // Don't show non-executable commands as executable
        if (executable) {
            val joiner = StringJoiner(" ")

            for (parameter in parameters) {
                var name = parameter.name

                name = if (parameter.required)
                    "<$name>"
                else
                    "[<$name>]"

                if (parameter.vararg)
                    name += "..."

                joiner.add(name)
            }

            output.add(this.name + " " + joiner.toString())
        }

        for (subcommand in subcommands) {
            for (subcommandUsage in subcommand.usage()) {
                output.add(this.name + " " + subcommandUsage)
            }
        }

        return output
    }
}
