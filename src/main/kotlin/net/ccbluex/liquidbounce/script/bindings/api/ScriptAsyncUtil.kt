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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.util.Util
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Async utils including game-based tick scheduling and network requests.
 * JavaScript-only.
 *
 * @author MukjepScarlet
 */
class ScriptAsyncUtil(
    private val jsPromiseConstructor: Value
) {

    companion object TickScheduler : EventListener {

        /** Client async tasks */
        private val scriptFutures = mutableListOf<Future<*>>()

        private val currentTickTasks = arrayListOf<BooleanSupplier>()
        private val nextTickTasks = arrayListOf<BooleanSupplier>()

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
            currentTickTasks.removeIf { it.asBoolean }
            currentTickTasks += nextTickTasks
            nextTickTasks.clear()
        }

        private fun schedule(breakLoop: BooleanSupplier) {
            mc.execute { nextTickTasks += breakLoop }
        }

        fun clear() {
            mc.execute {
                currentTickTasks.clear()
                nextTickTasks.clear()
                scriptFutures.forEach {
                    it.cancel(true)
                }
                scriptFutures.clear()
            }
        }
    }

    /**
     * Converts a [CompletableFuture] to a JavaScript [Value] that represents a Promise.
     */
    @JvmName("completableFutureToPromise")
    @ScriptApiRequired
    fun <T> CompletableFuture<T>.toPromise(): Value =
        jsPromiseConstructor.newInstance(
            ProxyExecutable { (onResolve, onReject) ->
                scriptFutures += this
                this.thenAcceptAsync( { value ->
                    onResolve.executeVoid(value)
                }, mc).exceptionallyAsync( { e ->
                    onReject.executeVoid(e)
                    null
                }, mc)
            }
        )

    /**
     * `Promise.resolve(0)`
     */
    private val defaultPromise: Value =
        jsPromiseConstructor.invokeMember("resolve", 0)

    /**
     * Example: `await ticks(10)`
     *
     * @return `Promise<number>`
     */
    @ScriptApiRequired
    fun ticks(n: Int): Value {
        if (n <= 0) {
            return defaultPromise
        }

        var remains = n
        return until { --remains == 0 }
    }

    /**
     * Example: `const duration = await until(() => mc.player.isOnGround())`
     *
     * @return `Promise<number>`
     */
    @ScriptApiRequired
    fun until(
        condition: BooleanSupplier
    ): Value = jsPromiseConstructor.newInstance(
        ProxyExecutable { (onResolve, onReject) ->
            var waitingTick = 0
            schedule {
                waitingTick++
                try {
                    if (condition.asBoolean) {
                        onResolve.executeVoid(waitingTick)
                        true
                    } else {
                        false
                    }
                } catch (e: Throwable) {
                    onReject.executeVoid(e)
                    true
                }
            }

            null
        }
    )

    /**
     * Example: `const result = await conditional(20, () => mc.player.isOnGround())`
     *
     * @return `Promise<number>`
     */
    @ScriptApiRequired
    fun conditional(
        ticks: Int,
        breakLoop: BooleanSupplier
    ): Value {
        if (ticks <= 0) {
            return defaultPromise
        }

        var remains = ticks
        return until { --remains == 0 || breakLoop.asBoolean }
    }

    /**
     * Sends an HTTP request or websocket request asynchronously. (based on [okhttp3])
     * JS `Promise` result will be resolved or rejected on Render thread.
     *
     * Example: `const result = await request(builder => builder.url('http://localhost:15000'))`
     *
     * @return `Promise<okhttp3.Response>`
     */
    @ScriptApiRequired
    fun request(
        block: Consumer<okhttp3.Request.Builder>
    ): Value = launch(Util.getDownloadWorkerExecutor()) {
        val request = okhttp3.Request.Builder().apply(block::accept).build()
        HttpClient.client.newCall(request).execute()
    }

    /**
     * Starts an async task on [executor], returns a `Promise`.
     * JS `Promise` result will be resolved or rejected on Render thread.
     * You can use utils from [java.util.concurrent] to control your tasks.
     *
     * @return `Promise<T>`
     */
    @ScriptApiRequired
    fun <T> launch(
        executor: Executor,
        block: Supplier<T>,
    ): Value = CompletableFuture.supplyAsync(block, executor).toPromise()

    /**
     * Starts an async task on [Util.getMainWorkerExecutor()], returns a `Promise`.
     * JS `Promise` result will be resolved or rejected on Render thread.
     * You can use utils from [java.util.concurrent] to control your tasks.
     *
     * @return `Promise<T>`
     */
    @ScriptApiRequired
    fun <T> launch(
        block: Supplier<T>,
    ): Value = launch(Util.getMainWorkerExecutor(), block)

}
