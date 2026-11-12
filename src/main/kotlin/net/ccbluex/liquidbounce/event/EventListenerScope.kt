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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.features.client.ownClientState
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * The scopes of the current client. A listener has one in every client it launched something in.
 * A simulated client launches in its own, which end with it.
 */
private val scopeState = ownClientState { ConcurrentHashMap<EventListener, CoroutineScope>() }
private val scopes by scopeState

/**
 * Get the related [CoroutineScope] of receiver [EventListener] in the current client.
 *
 * All tasks will check [EventListener.running] on suspend, and stay in the client they were launched in.
 */
val EventListener.eventListenerScope: CoroutineScope
    get() = scopes.computeIfAbsent(this) {
        CoroutineScope(
            SupervisorJob() // Prevent exception canceling
                + CoroutineExceptionHandler { _, throwable -> // logging
                when (throwable) {
                    is EventListenerNotListeningException ->
                        logger.debug("{} is not listening, job cancelled", throwable.eventListener)
                    is ReportedException ->
                        ErrorHandler.fatal(throwable, additionalMessage = "CoroutineScope of $it")
                    else -> logger.error("Exception occurred in CoroutineScope of $it", throwable)
                }
            }
                + CoroutineName(it.toString()) // Name
                + ClientContext.Element(ClientContext.current)
                // Render thread + Auto cancel on not listening
                + it.wrapContinuationInterceptor(Dispatchers.Main)
        )
    }

/**
 * Remove cached scope and cancel it.
 *
 * Remember to do this!
 */
fun EventListener.removeEventListenerScope() {
    scopes.remove(this)?.cancel(EventListenerNotListeningException(this))
}

/**
 * Cancels whatever the listeners still have running in the current client.
 */
internal fun cancelEventListenerScopes() {
    for ((listener, scope) in scopes) {
        scope.cancel(EventListenerNotListeningException(listener))
    }
    scopes.clear()
}

/**
 * Cancels what listeners launched in the simulated [client], if anything.
 */
internal fun cancelEventListenerScopes(client: Client) {
    if (scopeState.isSetIn(client)) {
        ClientContext.within(client) { cancelEventListenerScopes() }
    }
}

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
                // Whoever resumes may be on a thread of another client, like the one that sent a packet
                val client = context[ClientContext.Element]?.client
                if (client == null) {
                    resumeIfRunning(result)
                } else {
                    ClientContext.within(client) { resumeIfRunning(result) }
                }
            }

            private fun resumeIfRunning(result: Result<T>) {
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
