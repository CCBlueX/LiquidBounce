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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskManagerTest {

    @Test
    fun `an action that starts immediately can cancel its own tracked job`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        try {
            val manager = TaskManager(scope)
            var actionJob: Job? = null
            var trackedJob: Job? = null
            val task = manager.launch("Download") { task ->
                actionJob = currentCoroutineContext()[Job]
                trackedJob = task.job
                manager.cancel(task.name)
                awaitCancellation()
            }

            assertSame(assertNotNull(actionJob), trackedJob)
            assertTrue(assertNotNull(task.job).isCancelled)
            assertTrue(task.isCompleted)
            assertTrue(manager.getActiveTasks().isEmpty())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `successful jobs finish their tracked task and subtasks`() = runTest {
        val manager = TaskManager(this)
        val task = manager.launch("Download") { task ->
            task.progress = 0.5f
            task.getOrCreateTask("File").progress = 0.25f
        }

        runCurrent()

        assertTrue(task.isCompleted)
        assertTrue(task.subTasks.getValue("File").isCompleted)
        assertEquals(1f, manager.progress)
        assertTrue(manager.isCompleted)
        assertTrue(manager.getActiveTasks().isEmpty())
    }

    @Test
    fun `failed jobs finish tracking and still report the original exception`() = runTest {
        val failures = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, failure -> failures += failure },
        )
        try {
            val manager = TaskManager(scope)
            val failure = IllegalStateException("Download failed")
            val task = manager.launch("Download") { task ->
                task.getOrCreateTask("File").progress = 0.5f
                throw failure
            }

            runCurrent()

            assertSame(failure, failures.single())
            assertTrue(assertNotNull(task.job).isCancelled)
            assertTrue(task.isCompleted)
            assertEquals(1f, manager.progress)
            assertTrue(manager.getActiveTasks().isEmpty())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `cancelling a running job finishes its tracked task`() = runTest {
        val manager = TaskManager(this)
        val task = manager.launch("Download") { task ->
            task.getOrCreateTask("File").progress = 0.5f
            awaitCancellation()
        }
        runCurrent()
        assertFalse(task.isCompleted)

        assertNotNull(task.job).cancelAndJoin()

        assertTrue(task.isCompleted)
        assertEquals(1f, manager.progress)
        assertTrue(manager.isCompleted)
    }

    @Test
    fun `manager cancellation before dispatch prevents the action from starting`() = runTest {
        val manager = TaskManager(this)
        var ran = false
        val task = manager.launch("Download") { ran = true }

        manager.cancel("Download")
        runCurrent()

        assertFalse(ran)
        assertTrue(assertNotNull(task.job).isCancelled)
        assertTrue(task.isCompleted)
        assertEquals(1f, manager.progress)
    }

    @Test
    fun `scope cancellation before dispatch finishes tracking without running the action`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val manager = TaskManager(scope)
        var ran = false
        val task = manager.launch("Download") { ran = true }

        scope.cancel()
        runCurrent()

        assertFalse(ran)
        assertTrue(assertNotNull(task.job).isCancelled)
        assertTrue(manager.isCompleted)
        assertEquals(1f, manager.progress)
    }

    @Test
    fun `launching in an already cancelled scope leaves no active task`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        scope.cancel()
        val manager = TaskManager(scope)
        var ran = false

        val task = manager.launch("Download") { ran = true }
        runCurrent()

        assertFalse(ran)
        assertTrue(assertNotNull(task.job).isCancelled)
        assertTrue(task.isCompleted)
        assertTrue(manager.getActiveTasks().isEmpty())
    }

    @Test
    fun `an older job cannot complete a replacement task with the same name`() = runTest {
        val manager = TaskManager(this)
        val firstRelease = CompletableDeferred<Unit>()
        val secondRelease = CompletableDeferred<Unit>()
        val first = manager.launch("Download") { firstRelease.await() }
        val second = manager.launch("Download") { secondRelease.await() }
        runCurrent()

        try {
            firstRelease.complete(Unit)
            runCurrent()

            assertTrue(first.isCompleted)
            assertFalse(second.isCompleted)
            assertFalse(manager.isCompleted)
            assertEquals(listOf(second), manager.getActiveTasks())
        } finally {
            firstRelease.complete(Unit)
            secondRelease.complete(Unit)
        }
        runCurrent()

        assertTrue(second.isCompleted)
        assertTrue(manager.isCompleted)
    }

    @Test
    fun `completion includes nested subtasks`() {
        val manager = TaskManager(CoroutineScope(SupervisorJob()))
        val task = manager.createTask("Download")
        val child = task.getOrCreateTask("Archive")
        val grandchild = child.getOrCreateTask("File")

        manager.complete("Download")

        assertTrue(grandchild.isCompleted)
        assertTrue(child.isCompleted)
        assertTrue(task.isCompleted)
        assertEquals(1f, manager.progress)
        assertTrue(manager.getActiveTasks().isEmpty())
    }

    @Test
    fun `cancelling a task without a job completes all nested manual progress`() = runTest {
        val manager = TaskManager(this)
        val task = manager.createTask("Download")
        val archive = task.getOrCreateTask("Archive")
        val file = archive.getOrCreateTask("File")
        file.progress = 0.25f
        val unrelated = manager.createTask("Other")

        manager.cancel("Missing")
        assertFalse(task.isCompleted)
        manager.cancel("Download")

        assertTrue(file.isCompleted)
        assertTrue(archive.isCompleted)
        assertTrue(task.isCompleted)
        assertEquals(1f, task.progress)
        assertFalse(unrelated.isCompleted)
        assertEquals(listOf(unrelated), manager.getActiveTasks())
    }

}
