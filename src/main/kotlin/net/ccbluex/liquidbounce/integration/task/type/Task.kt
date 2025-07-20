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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.task.type

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * An asynchronous task with progress tracking
 */
open class Task(val name: String) {

    var progress: Float = 0f
        get() = if (subTasks.isNotEmpty()) calculateProgress() else field
    var isCompleted: Boolean = false
        get() = field && if (subTasks.isNotEmpty()) areAllSubTasksCompleted() else true
    var startTime: Long = System.currentTimeMillis()
    var job: Job? = null
    val subTasks = ConcurrentHashMap<String, Task>()

    /**
     * Creates or gets an existing sub-task
     */
    fun getOrCreateTask(subTaskName: String): Task {
        return subTasks.getOrPut(subTaskName) { Task(subTaskName) }
    }

    /**
     * Creates or gets an existing download sub-task
     */
    fun getOrCreateFileTask(subTaskName: String): ResourceTask {
        return subTasks.getOrPut(subTaskName) { ResourceTask(subTaskName) } as ResourceTask
    }

    /**
     * Calculates aggregate progress of all subtasks
     */
    fun calculateProgress(): Float {
        if (subTasks.isEmpty()) return progress
        return subTasks.values.sumOf { it.progress.toDouble() }.toFloat() / subTasks.size
    }

    /**
     * Checks if all subtasks are completed
     */
    fun areAllSubTasksCompleted(): Boolean = subTasks.isEmpty() ||
        subTasks.values.all { task -> task.isCompleted }

}
