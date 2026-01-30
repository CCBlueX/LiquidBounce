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

package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.config.types.list.Tagged

data class ScriptDebugOptions(
    val enabled: Boolean = false,
    val protocol: DebugProtocol = DebugProtocol.INSPECT,
    val suspendOnStart: Boolean = false,
    val inspectInternals: Boolean = false,
    val port: Int = 4242
)

enum class DebugProtocol(override val tag: String) : Tagged {
    DAP("DAP"),
    INSPECT("INSPECT"),
}
