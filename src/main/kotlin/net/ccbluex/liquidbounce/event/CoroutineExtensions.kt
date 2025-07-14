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
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val RenderThreadDispatcher = mc.asCoroutineDispatcher()

/**
 * Simple cache.
 */
private val eventListenerScopeHolder = ConcurrentHashMap<EventListener, CoroutineScope>()

/**
 * Get the related [CoroutineScope] of receiver [EventListener].
 *
 * All tasks will check [EventListener.running] on suspend.
 */
val EventListener.eventListenerScope: CoroutineScope
    get() = eventListenerScopeHolder.computeIfAbsent(this) {
        CoroutineScope(
            SupervisorJob() // Prevent exception canceling
            + CoroutineExceptionHandler { ctx, throwable -> // logging
                logger.error("Exception occurred in CoroutineScope of $it", throwable)
            }
            + CoroutineName(it.toString()) // Name
            // Render thread + Auto cancel on not listening
            + it.continuationInterceptor(RenderThreadDispatcher)
        )
    }

/**
 * Start a [Job] on event.
 *
 * It's fully async, so modifying the [Event] instance makes no sense.
 */
inline fun <reified T : Event> EventListener.suspendHandler(
    context: CoroutineContext = EmptyCoroutineContext,
    priority: Short = 0,
    crossinline handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> {
    // Support auto-cancel
    val context = context[ContinuationInterceptor]?.let { context + continuationInterceptor(it) } ?: context
    return handler<T>(priority) { event ->
        eventListenerScope.launch(context) {
            handler(event)
        }
    }
}

/**
 * Wrap the [original] interceptor and make it auto-detect
 * the listener's running state at suspension
 * to determine whether to resume the continuation.
 */
fun EventListener.continuationInterceptor(original: ContinuationInterceptor? = null): ContinuationInterceptor =
    original as? EventListenerRunningContinuationInterceptor
        ?: EventListenerRunningContinuationInterceptor(original, this)

/**
 * Remove cached scope and cancel it.
 *
 * Remember to do this!
 */
fun EventListener.removeEventListenerScope() {
    eventListenerScopeHolder.remove(this)?.cancel(EventListenerNotListeningException(this))
}

/**
 * Occurs when the running [Job] is canceled because [EventListener.running] is false
 */
class EventListenerNotListeningException(val eventListener: EventListener) :
    CancellationException("EventListener $eventListener is not running")

/**
 * Check [EventListener.running] on suspend.
 * If true, continue.
 * If false, cancel the job.
 *
 * This means the cancellation will not be **immediate** like [Thread.interrupt].
 *
 * @param original The original [ContinuationInterceptor] such as a [kotlinx.coroutines.CoroutineDispatcher],
 * because one [CoroutineContext] can only contain one value for a same key.
 *
 * @author MukjepScarlet
 */
private class EventListenerRunningContinuationInterceptor(
    private val original: ContinuationInterceptor?,
    private val eventListener: EventListener,
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(
        continuation: Continuation<T>
    ): Continuation<T> {
        // Process with original interceptor
        val delegate = original?.interceptContinuation(continuation) ?: continuation

        return object : Continuation<T> {
            override val context get() = continuation.context

            override fun resumeWith(result: Result<T>) {
                // if the event listener is no longer active, abort the result
                val result = if (eventListener.running) {
                    result
                } else {
                    Result.failure(EventListenerNotListeningException(eventListener))
                }
                delegate.resumeWith(result)
            }
        }
    }
}
