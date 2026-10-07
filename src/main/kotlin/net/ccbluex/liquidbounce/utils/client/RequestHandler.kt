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

package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.NullableBypass.mc
import java.util.Comparator.comparingInt
import java.util.concurrent.PriorityBlockingQueue

class RequestHandler<T> {

    private var currentTick = 0L

    private class ActiveRequest<T>(val request: Request<T>, val expiresAt: Long)

    private val activeRequests = PriorityBlockingQueue<ActiveRequest<T>>(11, comparingInt { it.request.priority.inv() })

    fun tick(deltaTime: Int = 1) {
        currentTick += deltaTime
    }

    fun clear() {
        activeRequests.clear()
        currentTick = 0L
    }

    fun request(request: Request<T>) {
        // we remove all requests provided by module on new request
        activeRequests.removeIf { it.request.provider === request.provider }
        activeRequests.add(ActiveRequest(request, currentTick + request.expiresIn))
    }

    fun getActiveRequestValue(): T? {
        var top = activeRequests.peek() ?: return null

        if (mc()?.isSameThread != false) {
            // we remove all outdated requests here
            while (top.expiresAt <= currentTick || !top.request.provider.running) {
                activeRequests.remove()
                top = activeRequests.peek() ?: return null
            }
        }

        return top.request.value
    }

    /**
     * A requested state of the system.
     *
     * Note: A request is deleted when its corresponding module is disabled.
     *
     * @param expiresIn in how many ticks units should this request expire?
     * @param priority higher = higher priority
     * @param provider module which requested value
     */
    class Request<T>(
        val expiresIn: Int, val priority: Int, val provider: EventListener, val value: T
    )
}
