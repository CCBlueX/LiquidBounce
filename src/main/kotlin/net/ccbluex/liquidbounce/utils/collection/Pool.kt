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

package net.ccbluex.liquidbounce.utils.collection

import net.minecraft.util.math.BlockPos
import org.joml.Vector3f
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A object pool for reusing mutable objects.
 *
 * @param T Type of objects to be pooled
 * @property queue The backend queue, determines if the instance is thread-safe.
 * @property initializer Function to create new instances when pool is empty
 * @property finalizer Function to reset object state before returning to pool
 */
class Pool<T : Any> @JvmOverloads constructor(
    private val queue: Queue<T>,
    private val initializer: Supplier<T>,
    private val finalizer: Consumer<T> = Consumer {},
) {

    /**
     * Retrieves an object from the pool or creates a new one if pool is empty
     *
     * @return Reusable object instance processed by finalizer
     */
    fun take(): T = queue.poll() ?: initializer.get()

    /**
     * Returns an object to the pool after processing it with the finalizer
     *
     * @param value Object to be returned to the pool
     */
    fun offer(value: T) {
        queue.add(value.apply(finalizer::accept))
    }

    /**
     * Returns objects to the pool after processing it with the finalizer
     *
     * @param values Objects to be returned to the pool
     */
    fun offerAll(values: Iterable<T>) {
        if (values is Collection) {
            queue.addAll(values.onEach(finalizer::accept))
        } else {
            values.forEach(::offer)
        }
    }

    /**
     * Scoped function that automatically returns the object to the pool after use
     *
     * @param action Function to process the object
     * @return Result of the action function
     */
    inline fun <R> use(action: (T) -> R): R {
        val value = take()
        try {
            return action(value)
        } finally {
            offer(value)
        }
    }

    companion object {
        @JvmField
        val Vec3f: Pool<Vector3f> = Pool(
            queue = ArrayDeque(), // Render thread only
            initializer = ::Vector3f,
        ) { it.set(0f, 0f, 0f) }

        @JvmField
        val MutableBlockPos: Pool<BlockPos.Mutable> = Pool(
            queue = ConcurrentLinkedQueue(),
            initializer = BlockPos::Mutable,
        ) { it.set(0, 0, 0) }

        @JvmField
        val StringBuilder: Pool<StringBuilder> = Pool(
            queue = ConcurrentLinkedQueue(),
            initializer = { StringBuilder(128) },
        ) { it.setLength(0) }

        /**
         * Use [Pool.StringBuilder] to build [String].
         */
        inline fun buildStringPooled(
            builderAction: StringBuilder.() -> Unit,
        ): String {
            val sb = StringBuilder.take()
            sb.builderAction()
            return sb.toString().also {
                StringBuilder.offer(sb)
            }
        }

        /**
         * Use [Pool.StringBuilder] to build [String].
         */
        inline fun buildStringPooled(
            capacity: Int,
            builderAction: StringBuilder.() -> Unit,
        ): String {
            val sb = StringBuilder.take().apply { ensureCapacity(capacity) }
            sb.builderAction()
            return sb.toString().also {
                StringBuilder.offer(sb)
            }
        }
    }

}
