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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude

/**
 * A callable event
 */
abstract class Event {
    @ProtocolExclude
    var isCompleted: Boolean = false
        internal set
}

/**
 * A cancellable event
 */
abstract class CancellableEvent : Event() {
    /**
     * Let you know if the event is canceled
     *
     * @return state of cancel
     */
    var isCancelled: Boolean = false
        private set

    /**
     * Allows you to cancel an event
     */
    fun cancelEvent() {
        require(!isCompleted) { "Cannot cancel an event that has already been completed." }

        isCancelled = true
    }

}

/**
 * MixinEntityRenderState of event. Might be PRE or POST.
 */
enum class EventState(val stateName: String) {
    PRE("PRE"), POST("POST")
}

/**
 * The name the event is associated with on the interop protocol, taken from its `@Tag`.
 *
 * Falls back to the simple class name for add-on events that carry no `@Tag`. Those never reach
 * the protocol, so the name is only ever used for logging.
 */
val Class<out Event>.eventName: String
    get() = EventManager.eventNameOrNull(this) ?: simpleName

