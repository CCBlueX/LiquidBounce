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

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource

/**
 * Client commands are Brigadier trees over [ClientCommandSource]. Build one with [literal] and
 * [argument], then register it through
 * [net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon.registerCommand].
 */
object ClientCommands {

    @JvmStatic
    val prefix: String
        get() = CommandManager.GlobalSettings.prefix

    @JvmStatic
    fun literal(name: String): LiteralArgumentBuilder<ClientCommandSource> = LiteralArgumentBuilder.literal(name)

    @JvmStatic
    fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<ClientCommandSource, T> =
        RequiredArgumentBuilder.argument(name, type)

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
