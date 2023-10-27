package net.ccbluex.liquidbounce.utils.kotlin

import java.util.PriorityQueue
import kotlin.math.max

class RequestHandler<T> {
    private val activeRequests = PriorityQueue<Request<T>>(compareBy { -it.priority })

    fun tick(deltaTime: Int = 1) {
        this.activeRequests.forEach { it.expiresIn = max(it.expiresIn - deltaTime, 0) }
        this.activeRequests.removeIf { it.expiresIn <= 0 }
    }

    fun request(request: Request<T>) {
        this.activeRequests.add(request)
    }

    fun getActiveRequestValue(): T? {
        if (this.activeRequests.isEmpty())
            return null

        return this.activeRequests.peek().value
    }

    /**
     * @param priority higher = higher priority
     * @param expiresIn in how many time units should this request expire?
     */
    class Request<T>(
        var expiresIn: Int,
        val priority: Int,
        val value: T
    )
}
