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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Retries a suspending function [producer] up to [maxRetries] times with a delay of [interval] between each retry.
 *
 * @param interval The delay between each retry.
 * @param name The name of the data being retried. Default is "data". Used for debug.
 * @param maxRetries The maximum number of retries. Default is [Int.MAX_VALUE].
 * @param start The coroutine start mode of producer job. Default is [CoroutineStart.DEFAULT].
 * @param producer The suspending function to retry. If it throws a [Throwable],
 * it will be retried up to [maxRetries] times.
 * @return A [RetryingJob] object.
 */
inline fun <T : Any> CoroutineScope.retrying(
    interval: Duration,
    name: String = "data",
    maxRetries: Int = Int.MAX_VALUE,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline producer: suspend CoroutineScope.() -> T,
) : RetryingJob<T> {
    val stateFlow = MutableStateFlow<RetryingJob.State<T>>(RetryingJob.State.Init)
    val producerJob = launch(
        context = CoroutineName("$name-producer"),
        start = start,
    ) {
        var attempt = 0

        while (attempt < maxRetries) {
            try {
                attempt++
                val value = producer()
                stateFlow.value = RetryingJob.State.Success(value, attempt)
                break
            } catch (t: Throwable) {
                logger.warn("Failed to get $name, attempt $attempt/$maxRetries", t)
                stateFlow.value = RetryingJob.State.Loading(t, attempt)
                delay(interval)
            }
        }

        if (attempt == maxRetries) {
            logger.error("Failed to get $name after $attempt attempts.")
            stateFlow.value = RetryingJob.State.Stopped
        }
    }

    producerJob.invokeOnCompletion {
         when (it) {
            null -> {} // Normal completion
            is CancellationException -> stateFlow.value = RetryingJob.State.Cancelled(it)
            else -> stateFlow.value = RetryingJob.State.Error(it)
        }
    }

    return RetryingJob(stateFlow, producerJob)
}

@JvmRecord
data class RetryingJob<T : Any>(
    val stateFlow: StateFlow<State<T>>,
    val producerJob: Job,
) {
    fun getNow(): T? {
        return (stateFlow.value as? State.Success)?.value
    }

    suspend fun getFinalState(): State.Final<T> {
        return stateFlow.first { it is State.Final } as State.Final<T>
    }

    sealed interface State<out T : Any> {
        sealed interface Final<out T : Any> : State<T>

        data object Init : State<Nothing>
        data class Loading(val t: Throwable, val retryCount: Int) : State<Nothing>

        data object Stopped: Final<Nothing>
        data class Success<out T : Any>(val value: T, val retryCount: Int) : Final<T>
        data class Cancelled(val t: CancellationException) : Final<Nothing>
        data class Error(val t: Throwable) : Final<Nothing>
    }
}
