/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.name
import net.ccbluex.liquidbounce.features.module.Module
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Useful for sending actions to other events.
 */
object EventScheduler : Listenable {

    private val actions = CopyOnWriteArrayList<ScheduleInfo>()

    fun schedule(
        module: Module,
        eventClass: KClass<out Event>,
        id: Int,
        allowDuplicates: Boolean = true,
        action: (Event) -> Unit
    ): Boolean {
        if (allowDuplicates || !isScheduled(id, module)) {
            actions += ScheduleInfo(module, eventClass, id, action)
            return true
        }

        return false
    }

    fun isScheduled(id: Int, module: Module) = actions.any { it.module == module && it.id == id }

    fun clear(module: Module) = actions.removeIf { it.module == module }

    /**
     * TODO: Find a proper way to detect events
     */
    fun process(event: Event) {
        // Each to their own events
        for (action in actions) {
            // All events have an annotation paired with them. Shouldn't cause exceptions
            if (event::class.name() == action.eventClass.name()) {
                action.action(event)
                actions.remove(action)
            }
        }
    }

    // An event here that detects world change then clears the list

    data class ScheduleInfo(
        val module: Module,
        val eventClass: KClass<out Event>,
        val id: Int,
        val action: (Event) -> Unit
    )
}
