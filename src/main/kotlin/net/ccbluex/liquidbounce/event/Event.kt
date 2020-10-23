/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

/**
 * A callable event
 */
open class Event

/**
 * A cancellable event
 */
open class CancellableEvent : Event() {

    /**
     * Let you know if the event is cancelled
     *
     * @return state of cancel
     */
    var isCancelled: Boolean = false
        private set

    /**
     * Allows you to cancel a event
     */
    fun cancelEvent() {
        isCancelled = true
    }

}

/**
 * State of event. Might be PRE or POST.
 */
enum class EventState(val stateName: String) {
    PRE("PRE"), POST("POST")
}