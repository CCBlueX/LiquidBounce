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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.utils.client.Nameable
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

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
     * Allows you to cancel an event
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

fun KClass<out Event>.name(): String = this.findAnnotation<Nameable>()!!.name

/**
 * Retrieves the name that the event is supposed to be associated with in JavaScript.
 */
val KClass<out Event>.eventName: String
    get() = this.findAnnotation<Nameable>()!!.name
