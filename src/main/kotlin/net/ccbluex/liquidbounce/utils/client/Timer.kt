package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.minecraft.client.MinecraftClient

// Global minecraft timer
object Timer : Listenable {
    private val requestHandler = RequestHandler<Float>()

    /**
     * You cannot set this manually. Use [requestTimerSpeed] instead.
     */
    val timerSpeed: Float
        get() = requestHandler.getActiveRequestValue() ?: 1.0f

    val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        requestHandler.tick()
    }

    /**
     * Requests a timer speed change. If another module requests with a higher priority,
     * the other module is prioritized.
     */
    fun requestTimerSpeed(timerSpeed: Float, priority: Priority, resetAfterTicks: Int = 1) {
        requestHandler.request(RequestHandler.Request(resetAfterTicks + 1, priority.priority, timerSpeed))
    }
}

val MinecraftClient.timer
    get() = Timer
