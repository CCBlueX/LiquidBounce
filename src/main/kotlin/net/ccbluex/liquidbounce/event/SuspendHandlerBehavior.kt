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
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
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
    data class Parallel(val start: CoroutineStart, val onCancellation: Runnable?) :
        SuspendHandlerBehavior {
        override fun <T : Event> EventListener.createEventHook(
            eventClass: Class<T>,
            wrappedContext: CoroutineContext,
            priority: Short,
            handler: SuspendableEventHandler<T>
        ): EventHook<T> = handler(eventClass, priority) { event ->
            eventListenerScope.launch(wrappedContext, start) {
                beginRun()
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
            val channelRef = atomic<Channel<T>?>(null)

            return handler(eventClass, priority) { event ->
                val channel = channelRef.updateAndGet { old ->
                    old ?: Channel<T>(
                        capacity = Channel.BUFFERED,
                        onBufferOverflow = BufferOverflow.SUSPEND,
                    ).also { channel ->
                        eventListenerScope.launch {
                            channel.consumeEach { asRun { handler(it) } }
                        }.invokeOnCompletion {
                            channelRef.getAndSet(null)?.close(it)
                        }
                    }
                }

                eventListenerScope.launch(wrappedContext) {
                    channel!!.send(event)
                }
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
                    beginRun()
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
        ): EventHook<T> = ExclusiveHandler(this, eventClass, wrappedContext, priority, onCancellation, handler).hook

        companion object {
            @JvmField
            val Default = DiscardLatest(null)
        }
    }
}

/**
 * Handles one event at a time: events arriving while a run is active are dropped.
 */
class ExclusiveHandler<T : Event>(
    private val listener: EventListener,
    eventClass: Class<T>,
    private val context: CoroutineContext,
    priority: Short,
    private val onCancellation: Runnable?,
    handler: SuspendableEventHandler<T>,
) {

    private val jobRef = atomic<Job?>(null)

    val hook: EventHook<T> = listener.handler(eventClass, priority) { event ->
        var newJob: Job? = null

        while (true) {
            val currentJob = jobRef.value
            if (currentJob?.isActive == true) break

            if (newJob == null) {
                newJob = newRun { handler(event) }
            }

            if (jobRef.compareAndSet(currentJob, newJob)) {
                newJob.start()
                break
            } else {
                newJob.cancel()
            }
        }
    }

    /** The last run queued with [runInstead]. */
    private var queued: Job? = null

    /**
     * Runs [block] once in place of the handler: the active handler run is cancelled, runs queued here before go
     * first. Events are dropped until the queue is empty.
     */
    @Synchronized
    fun runInstead(block: suspend CoroutineScope.() -> Unit) {
        val previous = queued?.takeIf { it.isActive }
        val job = newRun {
            if (previous != null) {
                previous.join()
                beginRun()
            }
            block()
        }
        queued = job
        val replaced = jobRef.getAndSet(job)
        if (previous == null) {
            replaced?.cancel()
        }
        job.start()
    }

    private fun newRun(block: suspend CoroutineScope.() -> Unit) =
        listener.eventListenerScope.launch(context, start = CoroutineStart.LAZY) {
            beginRun()
            block()
        }.onCancellation(onCancellation)

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
