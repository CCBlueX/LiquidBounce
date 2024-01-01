package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import java.util.*
import kotlin.math.max

class RequestHandler<T> {
    private val activeRequests = PriorityQueue<Request<T>>(compareBy { -it.priority })

    fun tick(deltaTime: Int = 1) {
        this.activeRequests.forEach { it.expiresIn = max(it.expiresIn - deltaTime, 0) }
        this.activeRequests.removeIf { it.expiresIn <= 0 }
    }

    fun request(request: Request<T>) {
        // we remove all requests provided by module on new request
        activeRequests.removeAll { it.provider == request.provider }
        this.activeRequests.add(request)
    }

    fun getActiveRequestValue(): T? {
        if (this.activeRequests.isEmpty())
            return null

        return this.activeRequests.peek().value
    }

    fun remove(element: Request<T>) {
        if (this.activeRequests.isNotEmpty()) {
            this.activeRequests.remove(element)
        }
    }

    fun removeActive() {
        if (this.activeRequests.isNotEmpty()) {
            this.activeRequests.remove(this.activeRequests.peek())
        }
    }

    /**
     * @param expiresIn in how many time units should this request expire?
     * @param priority higher = higher priority
     * @param provider module which requested value
     */
    class Request<T>(
        var expiresIn: Int,
        val priority: Int,
        val provider: Module,
        val value: T
    )
}
