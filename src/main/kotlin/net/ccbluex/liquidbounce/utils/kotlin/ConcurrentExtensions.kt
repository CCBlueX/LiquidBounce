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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import net.ccbluex.liquidbounce.utils.client.mc
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty

inline operator fun <T> ThreadLocal<T>.getValue(receiver: Any?, property: KProperty<*>): T = get()

inline operator fun <T> ThreadLocal<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) = set(value)

/**
 * Dispatches to the [mc] thread. The instance is resolved on first dispatch,
 * because the dispatcher can be captured in coroutine contexts (e.g. through
 * class initializers running on the registry bootstrap thread) before [mc]
 * exists.
 */
@JvmField
val MinecraftDispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {

    private val delegate by lazy { mc.asCoroutineDispatcher() }

    override fun isDispatchNeeded(context: CoroutineContext) = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) = delegate.dispatch(context, block)

}

inline val Dispatchers.Minecraft get() = MinecraftDispatcher

suspend inline fun Array<out Job>.joinAll() = forEach { it.join() }
