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

import net.ccbluex.liquidbounce.event.ALL_EVENT_CLASSES
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.Module
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Useful for sending actions to other events.
 */
object EventScheduler : Listenable {

    /**
     * Maps the event class to the scheduled tasks that currently wait for it.
     */
    private val eventActionsMap: Map<Class<out Event>, CopyOnWriteArrayList<ScheduleInfo>>

    init {
        eventActionsMap = ALL_EVENT_CLASSES.associate { Pair(it.java, CopyOnWriteArrayList<ScheduleInfo>()) }
    }

    /**
     * Schedules a task on the next moment an event is called.
     *
     * @param uniqueId Allows the caller to specify a unique id for the event. If an event is already scheduled for
     * that event with the same module and id, the task will not be scheduled again.
     * @return Whether the task was scheduled. A reason why a schedule failed is the `uniqueId` param
     * or the event does not exist.
     */
    inline fun <reified T : Event> schedule(
        module: Module,
        uniqueId: Int? = null,
        noinline action: (T) -> Unit
    ): Boolean {
        return schedule(module, T::class.java, uniqueId) { action(it as T) }
    }

    /**
     * @see schedule
     */
    fun schedule(
        module: Module,
        eventClass: Class<out Event>,
        uniqueId: Int?,
        action: (Event) -> Unit
    ): Boolean {
        val scheduledEvents = eventActionsMap[eventClass] ?: return false

        if (uniqueId != null) {
            val alreadyScheduled = scheduledEvents.any { it.module == module && it.id == uniqueId }

            if (alreadyScheduled) {
                return false
            }
        }

        scheduledEvents.add(ScheduleInfo(module, uniqueId, action))

        return true
    }


    fun clear(module: Module) {
        for (value in eventActionsMap.values) {
            value.removeIf { it.module == module }
        }
    }

    fun process(event: Event) {
        val scheduledTasks = eventActionsMap[event.javaClass] ?: return

        // DON'T CHANGE THIS! This prevents a race condition. I know it looks silly, but it prevents problems.
        scheduledTasks.removeIf { scheduledTask ->
            // The removeIf function gives us no guarantee that the predicate is executed only once...
            if (!scheduledTask.discarded) {
                scheduledTask.action(event)

                scheduledTask.discarded = true
            }

            true
        }
    }

    // An event here that detects world change then clears the list

    data class ScheduleInfo(
        val module: Module,
        val id: Int?,
        val action: (Event) -> Unit,
        var discarded: Boolean = false
    )
}
