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

package net.ccbluex.liquidbounce.api.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private typealias TestException = IOException

class RetryingTest {

    @Test
    fun `should succeed on first attempt`() = runTest {
        var attempt = 0
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 3
        ) {
            attempt++
            "Success Data"
        }

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Success<String>>(finalState)
        assertEquals("Success Data", finalState.value)
        assertEquals(1, finalState.retryCount)
        assertEquals(1, attempt)
        assertEquals("Success Data", retryingJob.getNow())
    }

    @Test
    fun `should retry on failure and succeed eventually`() = runTest {
        var attempt = 0
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 5,
        ) {
            delay(100.milliseconds) // Delay for Init collecting
            attempt++
            if (attempt < 3) { // 2x fail, 1x success
                throw TestException("Temporary failure")
            }
            "Success after retries"
        }

        // Verify state transition
        val states = mutableListOf<RetryingJob.State<String>>()
        launch {
            retryingJob.stateFlow.collect {
                states += it
                if (it is RetryingJob.State.Final) {
                    cancel()
                }
            }
        }.join()

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Success<String>>(finalState)
        assertEquals("Success after retries", finalState.value)
        assertEquals(3, finalState.retryCount)
        assertEquals(3, attempt)
        assertEquals("Success after retries", retryingJob.getNow())
        assertEquals(finalState, states.last())

        // Verify state transition contains Init, Loading, and Success
        assertTrue(states.any { it is RetryingJob.State.Init })
        assertTrue(states.any { it is RetryingJob.State.Loading })
        assertTrue(states.any { it is RetryingJob.State.Success })
    }

    @Test
    fun `should stop after max retries exceeded`() = runTest {
        var attempt = 0
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 3
        ) {
            attempt++
            throw TestException("Persistent failure")
        }

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Stopped>(finalState)
        assertEquals(3, attempt)
        assertNull(retryingJob.getNow())
    }

    @Test
    fun `should handle cancellation properly`() = runTest {
        val retryingJob = retrying(
            interval = 1000.milliseconds, // Long interval to ensure we can cancel before execution
            name = "test-data",
            maxRetries = 5
        ) {
            delay(1000) // Simulate long-running operation
            "This should not be reached"
        }

        // Cancel operation immediately
        retryingJob.producerJob.cancel()

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Cancelled>(finalState)
        assertNull(retryingJob.getNow())
    }

    @Test
    fun `should handle exceptions properly`() = runTest {
        val testException = Exception("Test exception")
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 1
        ) {
            throw testException
        }

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Stopped>(finalState)
        assertNull(retryingJob.getNow())
    }

    @Test
    fun `should get intermediate states correctly`() = runTest {
        var attempt = 0
        val states = mutableListOf<RetryingJob.State<String>>()

        val retryingJob = retrying(
            interval = 50.milliseconds,
            name = "test-data",
            maxRetries = 5
        ) {
            delay(100.milliseconds) // Delay for Init collecting
            attempt++
            if (attempt < 3) {
                throw TestException("Temporary failure")
            }
            "Success at last"
        }

        // Collect state changes
        launch {
            retryingJob.stateFlow.collect {
                states += it
                if (it is RetryingJob.State.Final) {
                    cancel()
                }
            }
        }.join()

        // Wait for completion
        assertEquals(retryingJob.getFinalState(), states.last())

        // Verify state sequence
        assertEquals(RetryingJob.State.Init, states[0])
        assertIs<RetryingJob.State.Loading>(states[1])
        assertIs<RetryingJob.State.Loading>(states[2])
        assertIs<RetryingJob.State.Success<String>>(states[3])
    }

    @Test
    fun `should handle zero max retries correctly`() = runTest {
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 0
        ) {
            "This should not be executed"
        }

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Stopped>(finalState)
        assertNull(retryingJob.getNow())
    }

    @Test
    fun `should handle immediate cancellation`() = runTest {
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 3,
            start = CoroutineStart.LAZY // Lazy start
        ) {
            "This should not be executed"
        }

        // Immediate cancellation of the job before it starts
        retryingJob.producerJob.cancel()

        // Manually start the job
        retryingJob.producerJob.start()

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Cancelled>(finalState)
        assertNull(retryingJob.getNow())
    }

    @Test
    fun `getNow should return null before completion`() = runTest {
        val retryingJob = retrying(
            interval = 1000.milliseconds,
            name = "test-data",
            maxRetries = 3
        ) {
            delay(1000) // Simulate long-running operation
            "Result"
        }

        // Immediate check, should return null
        assertNull(retryingJob.getNow())

        // Cancel operation, avoid test timeout
        retryingJob.producerJob.cancel()
    }

    @Test
    fun `cancel normally Cancelled state`() = runTest {
        val retryingJob = retrying(
            interval = 100.milliseconds,
            name = "test-data",
            maxRetries = 3
        ) {
            delay(1000.milliseconds)
        }

        retryingJob.producerJob.cancel()

        // Wait for completion
        val finalState = retryingJob.getFinalState()

        // Verify result
        assertIs<RetryingJob.State.Cancelled>(finalState)
        assertNull(retryingJob.getNow())
    }

}
