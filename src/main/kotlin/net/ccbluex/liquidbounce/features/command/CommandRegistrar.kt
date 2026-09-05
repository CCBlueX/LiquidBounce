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

package net.ccbluex.liquidbounce.features.command

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource

/**
 * A command that registers itself onto a Brigadier [CommandDispatcher].
 *
 * Every inbuilt command is an `object` implementing this interface; the [CommandManager]
 * registers them through [CommandManager.register]. Scripts register their command nodes
 * through [CommandManager.registerNodes] instead.
 */
fun interface CommandRegistrar {

    /**
     * Registers this command onto the [dispatcher].
     */
    fun register(dispatcher: CommandDispatcher<ClientCommandSource>)
}
