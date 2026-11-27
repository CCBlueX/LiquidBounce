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

import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Collections

// TODO: send by request parameter
object SseSessionManager {

    private val sessions: MutableSet<ServerSSESession> =
        Collections.synchronizedSet(ReferenceOpenHashSet())

    fun add(session: ServerSSESession) {
        sessions.add(session)
    }

    fun remove(session: ServerSSESession) {
        sessions.remove(session)
    }

    fun broadcast(eventName: String, eventJson: String, onError: (ServerSSESession, Throwable) -> Unit) {
        val snapshot = sessions.toTypedArray().ifEmpty { return }
        val frame = ServerSentEvent(data = eventJson, event = eventName)
        for (session in snapshot) {
            session.launch {
                try {
                    session.send(frame)
                } catch (_: CancellationException) {
                    remove(session)
                } catch (t: Throwable) {
                    remove(session)
                    onError(session, t)
                }
            }
        }
    }
}
