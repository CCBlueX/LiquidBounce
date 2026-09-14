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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.features.command.CommandManager

/**
 * Client commands. Register them through
 * [net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon.registerCommand].
 */
object Commands {

    @JvmStatic
    val prefix: String
        get() = CommandManager.GlobalSettings.prefix

    /**
     * Whether a command or alias named [name] exists, ignoring case like the dispatcher does.
     */
    @JvmStatic
    fun isRootTaken(name: String): Boolean = CommandManager.isRootTaken(name)

    /**
     * Runs [command] as if typed after the prefix.
     */
    @JvmStatic
    fun execute(command: String) = CommandManager.execute(command)

}
