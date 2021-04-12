/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.utils

import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

typealias ScheduledRunnable<T> = () -> T

/**
 * A manager for a single thread that will *not* be terminated. Used for the Ultralight implementation.
 */
class SingleThreadTaskScheduler : AutoCloseable {
    private val lock = ReentrantLock()
    private val wakeupCondition = lock.newCondition()

    private val thread: Thread
    private val queue = LinkedBlockingQueue<ExecutionPromise<*>>()

    init {
        thread = thread(true) {
            try {
                while (true) {
                    val size = this.queue.size

                    if (size > 0) {
                        val task = this.queue.poll()!!

                        val result = try {
                            task.runnable()
                        } catch (e: Throwable) {
                            RuntimeException("Task failed execution", e).printStackTrace()

                            null
                        }

                        synchronized(task) {
                            task.done(result)
                        }

                        // Wake waiting threads up
                        task.lock.withLock {
                            task.wakeupCondition.signalAll()
                        }

                        // Don't wait if there are still tasks waiting in the queue
                        if (size > 1) {
                            continue
                        }
                    }

                    lock.withLock {
                        this.wakeupCondition.await()
                    }
                }
            } catch (e: InterruptedException) {
                // Thread interrupted, just return the method
            }
        }
    }

    /**
     * Runs the scheduled runnable on the thread. If the current thread
     * is already the thread, this function will execute the [task] in place.
     */
    fun <T> schedule(task: ScheduledRunnable<T>): Future<T> {
        // Don't schedule it if it is already executed in the current thread
        if (this.thread == Thread.currentThread())
            return DummyFuture(task)

        val future = ExecutionPromise(task)

        // Add the future to the execution queue
        this.queue.add(future)

        // Wake the thread up
        this.lock.withLock {
            this.wakeupCondition.signalAll()
        }

        return future
    }

    fun <T> scheduleBlocking(task: ScheduledRunnable<T>): T {
        // todo: fix freezes (@superblaubeere)
        // return this.schedule(task).get()
        return task()
    }

    override fun close() {
        this.thread.interrupt()
        this.thread.join()
    }
}

/**
 * An already finished future, just to wrap
 */
private class DummyFuture<T>(runnable: ScheduledRunnable<T>) : Future<T> {
    val result: T = runnable()

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

    override fun isCancelled(): Boolean = false

    override fun isDone(): Boolean = true

    override fun get(): T = this.result

    override fun get(timeout: Long, unit: TimeUnit): T = this.get()
}

/**
 * A non-cancellable [Future] implementation for [SingleThreadTaskScheduler]
 */
private class ExecutionPromise<T>(val runnable: ScheduledRunnable<T>) : Future<T> {
    val lock = ReentrantLock()
    val wakeupCondition = lock.newCondition()

    var done = false
    var value: T? = null

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        throw NotImplementedError("Not supported")
    }

    override fun isCancelled(): Boolean = false

    override fun isDone(): Boolean = synchronized(this) { this.done }

    override fun get(): T? {
        var value: T?

        run {
            while (true) {
                // Check if the task finished, if it is break...
                synchronized(this) {
                    if (this.done) {
                        value = this.value

                        return@run
                    }
                }

                lock.withLock {
                    // ...otherwise wait to be awaken.
                    wakeupCondition.await()
                }
            }
        }

        return value
    }

    override fun get(timeout: Long, unit: TimeUnit): T {
        throw NotImplementedError("Not supported")
    }

    fun done(any: Any?) {
        this.done = true
        this.value = any as T?
    }

}
