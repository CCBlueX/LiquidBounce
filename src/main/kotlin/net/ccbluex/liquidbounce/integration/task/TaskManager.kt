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

package net.ccbluex.liquidbounce.integration.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.integration.task.type.Task
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages asynchronous tasks and their progress
 */
class TaskManager(private val scope: CoroutineScope) {

    private val tasks = ConcurrentHashMap<String, Task>()

    val progress: Float
        get() {
            if (tasks.isEmpty()) {
                return 0f
            }

            return tasks.values.sumOf { task -> task.progress.toDouble() }.toFloat() / tasks.size
        }

    val isCompleted: Boolean
        get() = tasks.values.all { task -> task.isCompleted }

    /**
     * Creates a new task
     */
    fun createTask(name: String): Task {
        val task = Task(name)
        tasks[name] = task
        return task
    }

    /**
     * Launches a task within the task manager's scope
     */
    fun <T> launch(
        taskName: String,
        action: suspend (Task) -> T
    ): Task {
        val task = createTask(taskName)
        val job = scope.launch {
            // The action can start before launch returns, e.g. with an unconfined dispatcher.
            task.job = coroutineContext[Job]
            task.progress = 0f
            action(task)
        }
        // Also expose the Job when the coroutine is cancelled before its body starts.
        task.job = job
        // Also runs on failure or cancellation, including cancellation before the action starts.
        job.invokeOnCompletion { complete(task) }
        return task
    }

    /**
     * Marks a task as completed. This will also mark all subtasks as completed.
     */
    fun complete(taskName: String) {
        if (taskName.isEmpty()) {
            return
        }

        tasks[taskName]?.let(::complete)
    }

    private fun complete(task: Task) {
        for (subTask in task.subTasks.values) {
            complete(subTask)
        }

        task.progress = 1.0f
        task.isCompleted = true
    }

    /**
     * Cancels a task and all descendant jobs, then finishes their progress tracking.
     */
    fun cancel(taskName: String) {
        val task = tasks[taskName] ?: return
        cancelJobs(task)
        complete(task)
    }

    private fun cancelJobs(task: Task) {
        for (subTask in task.subTasks.values) {
            cancelJobs(subTask)
        }
        task.job?.cancel()
    }

    /**
     * Gets all active tasks
     */
    fun getActiveTasks() = tasks.values.filter { task -> !task.isCompleted }

}
