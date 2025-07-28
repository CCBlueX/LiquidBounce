/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.annotations.InbuiltEvent

/**
 * A callable event.
 *
 * All implementations should have annotation [InbuiltEvent].
 *
 * @see net.ccbluex.liquidbounce.annotations.InbuiltEvent
 */
abstract class Event

/**
 * A cancellable event. Always with state so implementations shouldn't be singleton (`object`).
 *
 * @see net.ccbluex.liquidbounce.annotations.InbuiltEvent
 */
abstract class CancellableEvent : Event() {
    /**
     * Let you know if the event is cancelled
     *
     * @return state of cancel
     */
    var isCancelled: Boolean = false
        private set

    /**
     * Allows you to cancel an event
     */
    fun cancelEvent() {
        isCancelled = true
    }

}

/**
 * MixinEntityRenderState of event. Might be PRE or POST.
 */
enum class EventState(val stateName: String) {
    PRE("PRE"), POST("POST")
}

private val EVENT_CLASS_NAME_LOOKUP = EventManager.allEventClasses.associateWith {
    it.getAnnotation(InbuiltEvent::class.java)!!.name
}

/** Uses [String.Companion.CASE_INSENSITIVE_ORDER] */
internal val EVENT_NAME_CLASS_LOOKUP = EventManager.allEventClasses.associateByTo(
    sortedMapOf(String.CASE_INSENSITIVE_ORDER)
) {
    it.getAnnotation(InbuiltEvent::class.java)!!.name
}

/**
 * Retrieves the name that the event is supposed to be associated with in JavaScript.
 */
val Class<out Event>.eventName: String
    get() = EVENT_CLASS_NAME_LOOKUP[this]!!
