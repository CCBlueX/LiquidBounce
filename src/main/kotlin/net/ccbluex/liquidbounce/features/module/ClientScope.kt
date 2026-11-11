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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.client.Client

/**
 * Which clients run a module, see [ClientModule.clientScope].
 */
@AddonApi
enum class ClientScope {

    /**
     * Only the main client, so the state of the module can be plain fields.
     */
    MAIN,

    /**
     * Every client, each with its own settings, enabled state and `clientState { }`. These modules also take part
     * in player simulations, which run in a copy of the client and throw it away afterwards.
     */
    EVERY,

    /**
     * The client on screen, whichever that is. For modules that only serve the window.
     */
    WINDOW;

    fun includes(client: Client) = when (this) {
        MAIN -> client.isMain
        EVERY -> true
        WINDOW -> client.isActive
    }

}
