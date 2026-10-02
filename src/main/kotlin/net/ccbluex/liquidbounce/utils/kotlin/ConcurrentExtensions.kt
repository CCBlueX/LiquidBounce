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
@file:Suppress("NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.utils.kotlin

import kotlinx.coroutines.Job
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.util.Util
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.reflect.KProperty

inline operator fun <T> ThreadLocal<T>.getValue(receiver: Any?, property: KProperty<*>): T = get()

inline operator fun <T> ThreadLocal<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) = set(value)

suspend inline fun Array<out Job>.joinAll() = forEach { it.join() }

/**
 * A [PreparableReloadListener] with a simplified async reload entry point.
 *
 * Minecraft's [reload] signature is verbose and its `preparationBarrier` is only meaningful
 * while wired into the reload pipeline, so this interface additionally exposes a plain
 * [reload] taking just the executor pair. The barrier-aware overload adapts it: it waits on
 * [PreparableReloadListener.PreparationBarrier.wait] before delegating to the simplified one.
 */
interface SimpleReloadListener : PreparableReloadListener {
    /**
     * Reloads this listener asynchronously.
     *
     * @param taskExecutor executor for CPU/GPU preparation work (shader compile, ...)
     * @param reloadExecutor executor for the final apply step that touches render-thread state
     */
    fun reload(
        taskExecutor: Executor = Util.backgroundExecutor(),
        reloadExecutor: Executor = mc,
    ): CompletableFuture<Void>

    override fun reload(
        currentReload: PreparableReloadListener.SharedState,
        taskExecutor: Executor,
        preparationBarrier: PreparableReloadListener.PreparationBarrier,
        reloadExecutor: Executor,
    ): CompletableFuture<Void> {
        // Unit.INSTANCE is a pure barrier signal: the returned future only completes once every
        // listener's preparation phase has finished AND all preceding listeners in the reload
        // chain have completed (SimpleReloadInstance chains listeners through the barrier).
        return preparationBarrier.wait(net.minecraft.util.Unit.INSTANCE)
            .thenCompose { this.reload(taskExecutor, reloadExecutor) }
    }

    /**
     * A [SimpleReloadListener] that reloads its [children] in parallel and reports completion
     * through [onFinished] once all of them have finished.
     */
    fun interface Sequenced : SimpleReloadListener {
        fun children(): List<SimpleReloadListener>

        fun onFinished(futures: List<*>) {}

        override fun reload(taskExecutor: Executor, reloadExecutor: Executor): CompletableFuture<Void> {
            return Util.sequence(children().map { it.reload(taskExecutor, reloadExecutor) })
                .thenAccept(this::onFinished)
        }
    }
}

