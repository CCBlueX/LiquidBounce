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

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

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
    data class Parallel(val start: CoroutineStart, val onCancellation: Consumer<Throwable>?) :
        SuspendHandlerBehavior {
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
     * Handles one event at a time: an event arriving while a run is active does what [whenBusy] says.
     */
    @JvmRecord
    data class Exclusive(
        val whenBusy: WhenBusy,
        val onCancellation: Consumer<Throwable>?,
    ) : SuspendHandlerBehavior {

        override fun <T : Event> EventListener.createEventHook(
            eventClass: Class<T>,
            wrappedContext: CoroutineContext,
            priority: Short,
            handler: SuspendableEventHandler<T>
        ): EventHook<T> = ExclusiveHandler(
            this, eventClass, wrappedContext, priority, whenBusy, onCancellation, handler
        ).hook

        companion object {
            /**
             * Discards the new event.
             */
            @JvmField
            val Drop = Exclusive(WhenBusy.DROP, null)

            /**
             * Cancels the run of the event before, the new one takes over.
             */
            @JvmField
            val LatestWins = Exclusive(WhenBusy.CANCEL_PREVIOUS, null)
        }
    }

    enum class WhenBusy {
        /** The new event is discarded. */
        DROP,

        /** The run of the event before is cancelled. */
        CANCEL_PREVIOUS,
    }
}

/**
 * Handles one event at a time on the scope of [listener].
 */
private class ExclusiveHandler<T : Event>(
    listener: EventListener,
    eventClass: Class<T>,
    private val context: CoroutineContext,
    priority: Short,
    private val whenBusy: SuspendHandlerBehavior.WhenBusy,
    private val onCancellation: Consumer<Throwable>?,
    private val handler: SuspendableEventHandler<T>,
) {

    private val jobRef = atomic<Job?>(null)

    private val scope = listener.eventListenerScope

    val hook: EventHook<T> = listener.handler(eventClass, priority) { event ->
        when (whenBusy) {
            SuspendHandlerBehavior.WhenBusy.DROP -> {
                var newJob: Job? = null

                while (true) {
                    val currentJob = jobRef.value
                    // nothing to do, and no job worth creating
                    if (currentJob?.isActive == true) break

                    if (newJob == null) {
                        newJob = newRun(event)
                    }

                    if (jobRef.compareAndSet(currentJob, newJob)) {
                        newJob.start()
                        break
                    }

                    // lost the race against another event, this one was never started
                    newJob.cancel()
                }
            }

            SuspendHandlerBehavior.WhenBusy.CANCEL_PREVIOUS -> {
                jobRef.getAndSet(newRun(event).also { it.start() })?.cancel()
            }
        }
    }

    private fun newRun(event: T) = scope.launch(context, CoroutineStart.LAZY) {
        handler(event)
    }.onCancellation(onCancellation)

}

private fun Job.onCancellation(callback: Consumer<Throwable>?) = apply {
    callback ?: return@apply
    invokeOnCompletion { cause ->
        if (cause is CancellationException) {
            callback.accept(cause)
        }
    }
}
