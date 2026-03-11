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
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import java.util.function.Predicate
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * Registers an event hook for events of type [T] and launches a sequence
 */
inline fun <reified T : Event> EventListener.sequenceHandler(
    priority: Short = 0,
    dispatcher: CoroutineDispatcher? = null,
    onCancellation: Runnable? = null,
    crossinline eventHandler: SuspendableEventHandler<T>,
) = suspendHandler<T>(
    context = wrapContinuationInterceptor(dispatcher),
    priority = priority,
    behavior = SuspendHandlerBehavior.Parallel(CoroutineStart.UNDISPATCHED, onCancellation),
) {
    eventHandler(it)
}

/**
 * Registers a repeatable sequence which repeats the execution of code on [GameTickEvent].
 */
inline fun EventListener.tickHandler(
    dispatcher: CoroutineDispatcher? = null,
    onCancellation: Runnable? = null,
    crossinline eventHandler: suspend CoroutineScope.() -> Unit,
) = suspendHandler<GameTickEvent>(
    context = wrapContinuationInterceptor(dispatcher),
    behavior = SuspendHandlerBehavior.DiscardLatest(onCancellation)
) {
    eventHandler()
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

suspend fun <T : Event> EventListener.waitMatches(
    eventClass: Class<T>,
    priority: Short,
    predicate: Predicate<T>,
): T {
    lateinit var eventHook: EventHook<T>
    lateinit var continuation: CancellableContinuation<T>
    fun resumeAndUnregister(result: Result<T>) {
        EventManager.unregisterEventHook(eventClass, eventHook)
        if (continuation.isActive) {
            continuation.resumeWith(result)
        }
    }
    eventHook = newEventHook(priority) { event ->
        try {
            if (predicate.test(event)) {
                resumeAndUnregister(Result.success(event))
            }
        } catch (e: Throwable) {
            resumeAndUnregister(Result.failure(e))
        }
    }

    return suspendCancellableCoroutine { cont ->
        continuation = cont
        cont.invokeOnCancellation {
            EventManager.unregisterEventHook(eventClass, eventHook)
        }
        EventManager.registerEventHook(eventClass, eventHook)
    }
}

/**
 * Wait an event of type [T] which matches given [predicate].
 *
 * The continuation resumes on the event handler thread. For example:
 * - [net.ccbluex.liquidbounce.event.events.PacketEvent]: client Netty IO (EventLoopGroup)
 * - [net.ccbluex.liquidbounce.event.events.GameTickEvent]: client render thread
 *
 * @param priority The priority of the event hook.
 * @param predicate The predicate to match the event.
 * If it throws a [Throwable], the continuation will be resumed with [Result.failure].
 */
suspend inline fun <reified T : Event> EventListener.waitMatches(
    priority: Short = 0,
    predicate: Predicate<T>,
): T = waitMatches(T::class.java, priority, predicate)

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
    predicate: Predicate<T>,
): T? = withTimeoutOrNull(timeout) { waitMatches(priority, predicate) }
