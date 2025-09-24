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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*
import kotlin.time.Duration

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
                if (throwable is EventListenerNotListeningException) {
                    logger.debug("{} is not listening, job cancelled", throwable.eventListener)
                } else {
                    logger.error("Exception occurred in CoroutineScope of $it", throwable)
                }
            }
            + CoroutineName(it.toString()) // Name
            // Render thread + Auto cancel on not listening
            + it.continuationInterceptor(MinecraftDispatcher)
        )
    }

/**
 * Start a [Job] on event.
 *
 * It's fully async, so modifying the [Event] instance makes no sense.
 *
 * @param context the coroutine context to use for the job, defaults to [EmptyCoroutineContext].
 * @param priority the priority of the event hook, defaults to 0.
 * @param behavior the behavior of the event handler, defaults to [SuspendHandlerBehavior.PARALLEL].
 */
inline fun <reified T : Event> EventListener.suspendHandler(
    context: CoroutineContext = EmptyCoroutineContext,
    priority: Short = 0,
    behavior: SuspendHandlerBehavior = SuspendHandlerBehavior.PARALLEL,
    noinline handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> = `@internal-suspendHandler`(T::class.java, context, priority, behavior, handler)

/**
 * To prevent bytecode explosion, we use this method to register event hooks.
 */
@Suppress("FunctionName") // Exclude from normal auto-completion
fun <T : Event> EventListener.`@internal-suspendHandler`(
    eventClass: Class<T>,
    context: CoroutineContext,
    priority: Short,
    behavior: SuspendHandlerBehavior,
    handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> {
    // Support auto-cancel
    val context = context[ContinuationInterceptor]?.let { context + continuationInterceptor(it) } ?: context
    return when (behavior) {
        SuspendHandlerBehavior.PARALLEL -> suspendHandlerParallel(eventClass, context, priority, handler)
        SuspendHandlerBehavior.SUSPEND -> suspendHandlerSuspend(eventClass, context, priority, handler)
        SuspendHandlerBehavior.CANCEL_PREVIOUS -> suspendHandlerCancelPrevious(eventClass, context, priority, handler)
        SuspendHandlerBehavior.DISCARD_LATEST -> suspendHandlerDiscardLatest(eventClass, context, priority, handler)
    }
}

private fun <T : Event> EventListener.suspendHandlerParallel(
    eventClass: Class<T>,
    wrappedContext: CoroutineContext,
    priority: Short,
    handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> = handler(eventClass, priority) { event ->
    eventListenerScope.launch(wrappedContext) {
        handler(event)
    }
}

private fun <T : Event> EventListener.suspendHandlerSuspend(
    eventClass: Class<T>,
    wrappedContext: CoroutineContext,
    priority: Short,
    handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> {
    var channel: Channel<T>? = null

    fun restartReceiver() {
        channel = Channel(Channel.BUFFERED) // Default buffered
        eventListenerScope.launch(wrappedContext) {
            for (event in channel ?: error("Channel is null")) {
                handler(event)
            }
        }.invokeOnCompletion {
            channel?.close(it) ?: error("Channel is null")
            channel = null
        }
    }

    restartReceiver()

    // Producer
    return handler(eventClass, priority) { event ->
        channel?.let {
            eventListenerScope.launch(wrappedContext) {
                it.send(event)
            }
        } ?: restartReceiver() // Old consumer has been closed
    }
}

private fun <T : Event> EventListener.suspendHandlerCancelPrevious(
    eventClass: Class<T>,
    wrappedContext: CoroutineContext,
    priority: Short,
    handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> {
    val jobRef = AtomicReference<Job?>(null)
    return handler(eventClass, priority) { event ->
        jobRef.getAndSet(eventListenerScope.launch(wrappedContext) {
            handler(event)
        })?.cancel()
    }
}

private fun <T : Event> EventListener.suspendHandlerDiscardLatest(
    eventClass: Class<T>,
    wrappedContext: CoroutineContext,
    priority: Short,
    handler: suspend CoroutineScope.(T) -> Unit
): EventHook<T> {
    val jobRef = AtomicReference<Job?>(null)
    return handler(eventClass, priority) { event ->
        var newJob: Job? = null

        while (true) {
            val currentJob = jobRef.get()
            if (currentJob?.isActive == true) break

            if (newJob == null) {
                newJob = eventListenerScope.launch(wrappedContext, start = CoroutineStart.LAZY) {
                    handler(event)
                }
            }

            if (jobRef.compareAndSet(currentJob, newJob)) {
                newJob.start()
                break
            } else {
                newJob.cancel()
            }
        }
    }
}

enum class SuspendHandlerBehavior {
    /**
     * Starts a new job for each event.
     */
    PARALLEL,

    /**
     * Suspends the new event if a job is active. Thus, all events will be handled one by one.
     */
    SUSPEND,

    /**
     * Cancels the previous job if it's active.
     */
    CANCEL_PREVIOUS,

    /**
     * Discards the new event if a job is active.
     */
    DISCARD_LATEST,
}

/**
 * Wait an event of type [T] which matches given [predicate].
 *
 * The continuation resumes on the event handler thread. For example:
 * - [net.ccbluex.liquidbounce.event.events.PacketEvent]:  client Netty IO (EventLoopGroup)
 * - [net.ccbluex.liquidbounce.event.events.GameTickEvent]: client render thread
 *
 * @param priority The priority of the event hook.
 * @param predicate The predicate to match the event.
 * If it throws a [Throwable], the continuation will be resumed with [Result.failure].
 */
suspend inline fun <reified T : Event> EventListener.waitMatches(
    priority: Short = 0,
    crossinline predicate: (T) -> Boolean,
): T = suspendCancellableCoroutine { continuation ->
    val eventClass = T::class.java
    lateinit var eventHook: EventHook<T>
    fun resumeAndUnregister(result: Result<T>) {
        EventManager.unregisterEventHook(eventClass, eventHook)
        if (continuation.isActive) {
            continuation.resumeWith(result)
        }
    }
    eventHook = EventHook(this, handler = { event ->
        try {
            if (predicate(event)) {
                resumeAndUnregister(Result.success(event))
            }
        } catch (e: Throwable) {
            resumeAndUnregister(Result.failure(e))
        }
    }, priority)
    continuation.invokeOnCancellation {
        EventManager.unregisterEventHook(eventClass, eventHook)
    }
    EventManager.registerEventHook(eventClass, eventHook)
}

/**
 * Wait an event of type [T] which matches given [predicate].
 * If the timeout is exceeded, return null.
 *
 * This is exactly a shortcut of:
 * ```kotlin
 * withTimeoutOrNull(timeout) { waitMatches(priority, predicate) }
 * ```
 *
 * @param timeout The timeout duration.
 * @param priority The priority of the event hook.
 * @param predicate The predicate to match the event.
 * If it throws a [Throwable], the continuation will be resumed with [Result.failure].
 */
suspend inline fun <reified T : Event> EventListener.waitMatchesWithTimeout(
    timeout: Duration,
    priority: Short = 0,
    crossinline predicate: (T) -> Boolean,
): T? = withTimeoutOrNull(timeout) { waitMatches(priority, predicate) }

/**
 * Wrap the [original] interceptor and make it auto-detect
 * the listener's running state at suspension
 * to determine whether to resume the continuation.
 */
internal fun EventListener.continuationInterceptor(
    original: ContinuationInterceptor? = null,
): ContinuationInterceptor = original as? EventListenerRunningContinuationInterceptor
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
 * @param original The original [ContinuationInterceptor] such as a [CoroutineDispatcher],
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
