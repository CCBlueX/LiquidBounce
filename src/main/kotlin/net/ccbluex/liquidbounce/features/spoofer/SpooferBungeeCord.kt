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
package net.ccbluex.liquidbounce.features.spoofer

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable

object SpooferBungeeCord : ToggleableConfigurable(name = "BungeecordSpoofer", enabled = false) {

    val host by text("Host", "127.0.0.1")

    private object CustomUuid : ToggleableConfigurable(this, "CustomUUID", false) {
        val uuid by text("UUID", "85ac9d5ec3204e94933b3b0b8f6c512b")
    }

    init {
        tree(CustomUuid)
    }

    fun modifyHandshakeAddress(original: String): String {
        val uuidStr = CustomUuid.uuid.takeIf { enabled }
            ?: mc.session.uuidOrNull.toString().replace("-", "")

        // Format: "<originalAddress>\u0000<host>\u0000<uuid>"
        return "$original\u0000${host}\u0000$uuidStr"
    }

}
