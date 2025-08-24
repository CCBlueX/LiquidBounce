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
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A thread-safe object pool for reusing mutable objects.
 *
 * @param T Type of objects to be pooled
 * @property initializer Function to create new instances when pool is empty
 * @property finalizer Function to reset object state before returning to pool
 */
class ObjectPool<T : Any> @JvmOverloads constructor(
    private val initializer: () -> T,
    private val finalizer: (T) -> Unit = {},
) {

    private val pool = ConcurrentLinkedQueue<T>()

    /**
     * Retrieves an object from the pool or creates a new one if pool is empty
     *
     * @return Reusable object instance processed by finalizer
     */
    fun take(): T = pool.poll() ?: initializer().apply(finalizer)

    /**
     * Returns an object to the pool after processing it with the finalizer
     *
     * @param value Object to be returned to the pool
     * @return True if object was successfully added to the pool (always true)
     */
    fun offer(value: T): Boolean = pool.add(value.apply(finalizer))

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
        val MutableBlockPos = ObjectPool(BlockPos::Mutable) { it.set(0, 0, 0) }
    }

}
