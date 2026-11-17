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
package net.ccbluex.liquidbounce.gametest.harness

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ClientModule

fun GameTestScope.enable(module: ClientModule) = client { module.enabled = true }

fun GameTestScope.disable(module: ClientModule) = client { module.enabled = false }

@Suppress("UNCHECKED_CAST")
fun <T : Any> ValueGroup.setting(name: String) = containedValues.first { it.name == name } as Value<T>

/**
 * Sets values of [group] by name on the client thread. A text sets a value that holds something else the way a
 * config does, such as a mode by its name.
 */
fun GameTestScope.configure(group: ValueGroup, vararg values: Pair<String, Any>) = client {
    for ((name, value) in values) {
        val setting = group.setting<Any>(name)
        if (value is String && setting.get() !is String) {
            setting.setByString(value)
        } else {
            setting.set(value)
        }
    }
}

/** Runs a `.command` of the client; it never reaches the server. */
fun GameTestScope.command(line: String) = client { CommandManager.execute(line) }
