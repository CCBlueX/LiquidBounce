package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler

object Sprint : Listenable {
    private val requestHandler = RequestHandler<Boolean>()

    /**
     * You cannot set this manually. Use [requestSprintState] instead.
     */
    val sprintState: Boolean?
        get() = requestHandler.getActiveRequestValue()

    val tickHandler = handler<GameTickEvent> {
        requestHandler.tick()
    }

    /**
     * Requests a sprint state change. If another module requests with a higher priority,
     * the other module is prioritized.
     */
    fun requestSprintState(sprintingState: Boolean, priority: Priority, resetAfterTicks: Int = 1) {
        requestHandler.request(RequestHandler.Request(resetAfterTicks, priority.priority, sprintingState))
    }
}
