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
package net.ccbluex.liquidbounce.utils.kotlin

@Suppress("UNCHECKED_CAST")
class DoubleBuffer<T>(front: T, back: T) {
    private val buffers = arrayOf<Any?>(front, back)
    private var swap = false

    private val frontBufferIndex: Int
        get() = if (this.swap) 1 else 0

    private var front: T
        get() = this.buffers[frontBufferIndex] as T
        set(value) {
            this.buffers[frontBufferIndex] = value
        }

    private var back: T
        get() = this.buffers[1 - frontBufferIndex] as T
        set(value) {
            this.buffers[1 - frontBufferIndex] = value
        }

    fun swap() {
        this.swap = !this.swap
    }
}
