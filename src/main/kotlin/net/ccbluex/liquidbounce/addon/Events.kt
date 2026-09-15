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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

/**
 * Listening happens on the listener itself: `on(PacketEvent.class, handler)`, `onTick(task)`, `after(ticks, task)` and
 * `every(ticks, task)` on any module, mode or add-on, or `handler<PacketEvent> {}` from Kotlin. This is the rest.
 */
object Events {

    /** Runs before everything else, e.g. to cancel. */
    const val PRIORITY_FIRST: Short = EventPriorityConvention.FIRST_PRIORITY

    /** Runs after everything else; the event is in its final state. */
    const val PRIORITY_LAST: Short = EventPriorityConvention.READ_FINAL_STATE

    @JvmStatic
    fun <E : Event> post(event: E): E = EventManager.callEvent(event)

}
