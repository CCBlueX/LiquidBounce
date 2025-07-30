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

@CommandBuilderDsl
class CommandBuilderScope private constructor(
    private val name: String,
    private vararg val aliases: String,
    private var parent: Command? = null,
) {

//    private val paramters =

    var requiresInGame: Boolean = false

    private var execute: (CommandExecutionScope.() -> Unit)? = null

//    fun <T> parameter()

//    fun sub

    fun onExecute(function: CommandExecutionScope.() -> Unit) {
        execute = function
    }

    companion object {
        fun create(name: String, vararg aliases: String) = CommandBuilderScope(name, aliases = aliases)
    }

}
