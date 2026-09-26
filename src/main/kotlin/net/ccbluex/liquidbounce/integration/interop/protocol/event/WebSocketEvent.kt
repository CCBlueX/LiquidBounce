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
package net.ccbluex.liquidbounce.integration.interop.protocol.event

import com.google.gson.Gson
import net.ccbluex.liquidbounce.config.gson.interopGson

/**
 * Interface indicating that the marked event is designated for transmission through WebSocket communication.
 * This interface serves as a declarative marker for WebSocket-compatible events, signaling their eligibility for
 * propagation over WebSocket connections.
 */
interface WebSocketEvent {
    val serializer: Gson get() = interopGson

    /**
     * Indicates whether the event should be serialized asynchronously.
     * If set to true, the event will be serialized in a separate thread.
     *
     * The most common situation you need to set this to `false` is the event contains **mutable** states
     * that is modified after the event is created.
     */
    val serializeAsync: Boolean get() = true
}

