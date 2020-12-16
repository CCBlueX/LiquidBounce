/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2020 CCBlueX
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

import net.ccbluex.liquidbounce.features.command.commands.FriendCommand
import net.ccbluex.liquidbounce.features.command.commands.ToggleCommand
import net.ccbluex.liquidbounce.event.Listenable

class CommandManager : Listenable {

    private var prefix = "."
    private val commands: ArrayList<Command> = ArrayList()

    init {
        // TODO: Add command execution
    }

    fun registerCommands() {
        commands.add(ToggleCommand.createCommand())
        commands.add(FriendCommand.createCommand())
    }

    /**
     * Returns the instance of the subcommand that would be executed by a command
     * e.g. `getSubCommand(".friend add Player137 &3superblaubeere27")`
     * would return the instance of `add`
     */
    fun getSubCommand(cmd: String): Command? {
        return getSubCommand(cmd.split(" "))
    }

    /**
     * Used for this implementation of [getSubCommand]
     *
     * @param args The input command split on spaces
     * @param currentCommand The current command that is being researched
     * @param idx The current index that is researched, only used for implementation
     */
    private fun getSubCommand(args: List<String>, currentCommand: Command? = null, idx: Int = 0): Command? {
        // Return the last command when there are no more arguments
        if (idx >= args.size)
            return currentCommand

        // If currentCommand is null, search in all commands
        val commandSupplier = currentCommand?.subcommands?.asIterable() ?: this.commands

        // Look if something matches the current index, if it does, look if there are further matches
        commandSupplier.firstOrNull { it.name.equals(args[idx], true) }?.let { return getSubCommand(args, it, idx + 1) }

        // If no match was found, currentCommand is the subcommand that we searched for
        return currentCommand
    }

    /**
     * Always listen to events
     */
    override fun handleEvents() = true

}
