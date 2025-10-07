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

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.MinecraftDispatcher
import java.util.concurrent.ConcurrentHashMap
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
                + it.wrapContinuationInterceptor(MinecraftDispatcher)
        )
    }

/**
 * Start a [Job] on event.
 *
 * It's fully async, so modifying the [Event] instance makes no sense.
 *
 * @param context the coroutine context to use for the job, defaults to [EmptyCoroutineContext].
 * @param priority the priority of the event hook, defaults to 0.
 * @param behavior the behavior of the event handler, defaults to [SuspendHandlerBehavior.Parallel].
 */
inline fun <reified T : Event> EventListener.suspendHandler(
    context: CoroutineContext = EmptyCoroutineContext,
    priority: Short = 0,
    behavior: SuspendHandlerBehavior = SuspendHandlerBehavior.Parallel.Default,
    noinline handler: SuspendableEventHandler<T>
): EventHook<T> {
    // Support auto-cancel
    val context = context[ContinuationInterceptor]?.let { context + wrapContinuationInterceptor(it) } ?: context
    return with(behavior) {
        createEventHook(T::class.java, context, priority, handler)
    }
}

sealed interface SuspendHandlerBehavior {

    fun <T : Event> EventListener.createEventHook(
        eventClass: Class<T>,
        wrappedContext: CoroutineContext,
        priority: Short,
        handler: SuspendableEventHandler<T>
    ): EventHook<T>

    /**
     * Starts a new job for each event.
     */
    @JvmRecord
    data class Parallel(val start: CoroutineStart, val onCancellation: Runnable?) : SuspendHandlerBehavior {
        override fun <T : Event> EventListener.createEventHook(
            eventClass: Class<T>,
            wrappedContext: CoroutineContext,
            priority: Short,
            handler: SuspendableEventHandler<T>
        ): EventHook<T> = handler(eventClass, priority) { event ->
            eventListenerScope.launch(wrappedContext, start) {
                handler(event)
            }.onCancellation(onCancellation)
        }

        companion object {
            @JvmField
            val Default = Parallel(CoroutineStart.DEFAULT, null)
        }
    }

    /**
     * Suspends the new event if a job is active. Thus, all events will be handled one by one.
     */
    object Suspend : SuspendHandlerBehavior {
        override fun <T : Event> EventListener.createEventHook(
            eventClass: Class<T>,
            wrappedContext: CoroutineContext,
            priority: Short,
            handler: SuspendableEventHandler<T>
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
    }

    /**
     * Cancels the previous job if it's active.
     */
    object CancelPrevious : SuspendHandlerBehavior {
        override fun <T : Event> EventListener.createEventHook(
            eventClass: Class<T>,
            wrappedContext: CoroutineContext,
            priority: Short,
            handler: SuspendableEventHandler<T>
        ): EventHook<T> {
            val jobRef = atomic<Job?>(null)
            return handler(eventClass, priority) { event ->
                jobRef.getAndSet(eventListenerScope.launch(wrappedContext) {
                    handler(event)
                })?.cancel()
            }
        }
    }

    /**
     * Discards the new event if a job is active.
     */
    @JvmRecord
    data class DiscardLatest(val onCancellation: Runnable? = null) : SuspendHandlerBehavior {
        override fun <T : Event> EventListener.createEventHook(
            eventClass: Class<T>,
            wrappedContext: CoroutineContext,
            priority: Short,
            handler: SuspendableEventHandler<T>
        ): EventHook<T> {
            val jobRef = atomic<Job?>(null)
            return handler(eventClass, priority) { event ->
                var newJob: Job? = null

                while (true) {
                    val currentJob = jobRef.value
                    if (currentJob?.isActive == true) break

                    if (newJob == null) {
                        newJob = eventListenerScope.launch(wrappedContext, start = CoroutineStart.LAZY) {
                            handler(event)
                        }.onCancellation(onCancellation)
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

        companion object {
            @JvmField
            val Default = DiscardLatest(null)
        }
    }
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
fun EventListener.wrapContinuationInterceptor(
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

private fun Job.onCancellation(runnable: Runnable?) = apply {
    runnable?.let {
        this.invokeOnCompletion { t ->
            if (t is CancellationException) {
                it.run()
            }
        }
    }
}
