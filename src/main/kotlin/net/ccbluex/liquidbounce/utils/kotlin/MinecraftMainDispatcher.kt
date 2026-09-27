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

package net.ccbluex.liquidbounce.utils.kotlin

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.internal.MainDispatcherFactory
import net.ccbluex.liquidbounce.utils.client.NullableBypass.mc
import kotlin.coroutines.CoroutineContext

/**
 * Minecraft Render thread
 */
inline val Dispatchers.Minecraft: MainCoroutineDispatcher
    get() = MinecraftMainDispatcher.instance

/**
 * [MainCoroutineDispatcher] backed by the Minecraft render thread, exposed as [Dispatchers.Main]
 * through [Factory].
 *
 * [kotlinx.coroutines.Delay] is deliberately left unimplemented: Minecraft's executor cannot schedule, and borrowing
 * another scheduler would make kotlinx-coroutines adopt this dispatcher as the process-wide default
 */
@OptIn(InternalCoroutinesApi::class)
open class MinecraftMainDispatcher private constructor(
    private val invokeImmediately: Boolean
) : MainCoroutineDispatcher() {

    companion {
        val instance = MinecraftMainDispatcher(false)
    }

    private object Immediate : MinecraftMainDispatcher(true) {
        override val immediate: MainCoroutineDispatcher get() = this
        override fun toString() = "Dispatchers.Main.immediate[Minecraft Render thread]"
    }

    override val immediate: MainCoroutineDispatcher =
        if (invokeImmediately) this else Immediate

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !invokeImmediately || !RenderSystem.isOnRenderThread()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        checkNotNull(mc()) {
            "Minecraft client not launched!"
        }.execute(block)
    }

    override fun toString() = "Dispatchers.Main[Minecraft Render thread]"

    class Factory : MainDispatcherFactory {
        override val loadPriority: Int = 1
        override fun createDispatcher(
            allFactories: List<MainDispatcherFactory>
        ): MainCoroutineDispatcher = instance

        override fun hintOnError(): String =
            "Minecraft render-thread Main dispatcher failed to load"
    }
}
