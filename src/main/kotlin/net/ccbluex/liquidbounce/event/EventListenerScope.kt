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
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

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
                + Guards() // Copied empty into every coroutine launched on it
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
    eventListenerScopeHolder.remove(this)?.cancel(EventListenerNotListeningException(this))
}

/**
 * Wrap the [original] interceptor and make it auto-detect
 * the listener's running state and what the running coroutine [requires]
 * at suspension to determine whether to resume the continuation.
 */
fun EventListener.wrapContinuationInterceptor(
    original: ContinuationInterceptor? = null,
): ContinuationInterceptor = original as? EventListenerContinuationInterceptor
    ?: EventListenerContinuationInterceptor(original, this)

/**
 * Occurs when the running [Job] is canceled because [EventListener.running] is false
 */
class EventListenerNotListeningException(val eventListener: EventListener) :
    CancellationException("EventListener $eventListener is not running")

/**
 * Check [EventListener.running] and the coroutine's [Guards] on every resume.
 * If they hold, continue; otherwise the continuation gets the failure instead of the result.
 *
 * This means the cancellation will not be **immediate** like [Thread.interrupt].
 * It also means every suspension is covered, including ones no helper knows about.
 *
 * The check is handed to [original] instead of wrapping its result, so it runs on the thread the
 * continuation continues on and immediately before it, which lets it read game state.
 *
 * @param original The original [ContinuationInterceptor] such as a [CoroutineDispatcher],
 * because one [CoroutineContext] can only contain one value for a same key.
 *
 * @author MukjepScarlet
 */
private class EventListenerContinuationInterceptor(
    private val original: ContinuationInterceptor?,
    private val eventListener: EventListener,
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(
        continuation: Continuation<T>
    ): Continuation<T> {
        val checking = object : Continuation<T> {
            override val context get() = continuation.context

            override fun resumeWith(result: Result<T>) {
                if (result.isFailure) {
                    continuation.resumeWith(result)
                    return
                }

                // the listener or a requirement of the run may no longer hold, then the result is aborted
                continuation.resumeWith(
                    if (!eventListener.running) {
                        Result.failure(EventListenerNotListeningException(eventListener))
                    } else {
                        val broken = try {
                            continuation.context.activeGuards()?.broken()
                        } catch (e: Throwable) {
                            e
                        }
                        broken?.let { Result.failure(it) } ?: result
                    }
                )
            }
        }

        return original?.interceptContinuation(checking) ?: checking
    }
}
