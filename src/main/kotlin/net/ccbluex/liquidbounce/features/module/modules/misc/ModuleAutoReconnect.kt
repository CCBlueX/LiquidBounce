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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitSeconds
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.ServerObserver

/**
 * AutoReconnect module
 *
 * Automatically reconnects to the last server after a configurable delay when
 * the connection is lost.
 */
object ModuleAutoReconnect : ClientModule(
    "AutoReconnect",
    ModuleCategories.MISC
) {

    private val delay by int("Delay", 5, 1..60, "s")

    @Suppress("unused")
    private val disconnectHandler = sequenceHandler<DisconnectEvent> {
        // ServerObserver.serverInfo is intentionally preserved on disconnect
        // (see ServerObserver.disconnectHandler) so we can reconnect to it.
        if (ServerObserver.serverInfo == null) return@sequenceHandler

        waitSeconds(delay)

        // Guard against the user having reconnected manually during the delay.
        // mc.connection is an O(1) field read; catching the reconnect() exception
        // would use exception-as-control-flow and is therefore avoided.
        if (mc.connection == null) {
            ServerObserver.reconnect()
        }
    }

}
