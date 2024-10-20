/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.features.module.Module
import java.util.*

class RequestHandler<T> {
    private var currentTick = 0

    private val activeRequests = PriorityQueue<Request<T>>(compareBy { -it.priority })

    fun tick(deltaTime: Int = 1) {
        currentTick += deltaTime
    }

    fun request(request: Request<T>) {
        // we remove all requests provided by module on new request
        activeRequests.removeAll { it.provider == request.provider }
        request.expiresIn += currentTick
        this.activeRequests.add(request)
    }

    fun getActiveRequestValue(): T? {
        // we remove all outdated requests here
        while ((this.activeRequests.peek() ?: return null).expiresIn <= currentTick ||
            !this.activeRequests.peek().provider.enabled
        ) {
            this.activeRequests.remove()
        }
        return this.activeRequests.peek().value
    }

    fun clear() {
        activeRequests.clear()
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
        var expiresIn: Int, val priority: Int, val provider: Module, val value: T
    )
}
