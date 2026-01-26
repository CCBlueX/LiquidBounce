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

package net.ccbluex.liquidbounce.render.engine.font

import org.joml.Vector2i
import java.awt.Dimension
import java.awt.Point

class DynamicAtlasAllocator(
    val dimension: Dimension,
    /**
     * In order to reduce the fragmentation the allocator will cut the texture into slices.
     */
    val verticalCutSize: Int,
    /**
     * The minimal dimension of a slice. If a cut would be smaller than this, it will be made available.
     */
    val minDimension: Dimension
) {
    val availableSlices = HashSet<AtlasSlice>()

    init {
        var currY = 0

        while (currY < dimension.height) {
            val maxY = (currY + verticalCutSize).coerceAtMost(dimension.height)

            val height = maxY - currY

            if (height >= minDimension.height) {
                availableSlices.add(AtlasSlice(0, currY, dimension.width, height))
            }

            currY += verticalCutSize
        }
    }

    fun allocate(dimension: Dimension): AtlasSliceHandle? {
        val usedSlice = this.availableSlices
            .filter { it.width >= dimension.width && it.height >= dimension.height }
            .minByOrNull {
                val dW = it.width - dimension.width
                val dH = it.height - dimension.height

                dW * dW + dH * dH
            } ?: return null

        assert(!usedSlice.isAllocated)
        assert(usedSlice.children.isEmpty())

        // Mark the slice as used
        this.availableSlices.remove(usedSlice)
        usedSlice.isAllocated = true

        // Try to cut the slice into smaller slices
        val cutSlices = tryCutSlice(usedSlice, dimension)

        if (cutSlices == null) {
            return AtlasSliceHandle(usedSlice)
        } else {
            usedSlice.children.addAll(cutSlices)

            cutSlices.forEach { it.parent = usedSlice }

            // Add the new slices to the available slices
            this.availableSlices.addAll(cutSlices.subList(1, cutSlices.size))

            val internalSlice = cutSlices[0]

            internalSlice.isAllocated = true

            return AtlasSliceHandle(internalSlice)
        }
    }

    fun free(handle: AtlasSliceHandle) {
        handle.setFreed()

        val slice = handle.internalSlice

        assert(slice.isAllocated)
        assert(slice.children.isEmpty())

        slice.isAllocated = false

        val highestUnallocatedParent = updateParentAllocationStatusRecursively(slice)

        if (highestUnallocatedParent != null) {
            removeChildrenRecursively(highestUnallocatedParent)

            this.availableSlices.add(highestUnallocatedParent)
        } else {
            this.availableSlices.add(slice)
        }
    }

    private fun removeChildrenRecursively(highestUnallocatedParent: AtlasSlice) {
        highestUnallocatedParent.children.forEach {
            removeChildrenRecursively(it)

            it.parent = null

            this.availableSlices.remove(it)
        }

        highestUnallocatedParent.children.clear()
    }

    fun updateParentAllocationStatusRecursively(parent: AtlasSlice): AtlasSlice? {
        val allUnallocated = parent.children.none { it.isAllocated }

        if (allUnallocated) {
            parent.isAllocated = false

            val parentsParent = parent.parent

            return if (parentsParent == null) parent else updateParentAllocationStatusRecursively(parentsParent)
        } else {
            return null
        }
    }

    /**
     * Tries to cut the slice into four slices. If it cannot be cut, it will return null.
     *
     * The slice at index 0 is the slice with the given dimension
     */
    private fun tryCutSlice(slice: AtlasSlice, dimension: Dimension): List<AtlasSlice>? {
        val brotherSlice = Vector2i(slice.width - dimension.width, slice.height - dimension.height)

        // All four slices are big enough
        when {
            brotherSlice.x >= minDimension.height && brotherSlice.y >= minDimension.height -> {
                return listOf(
                    AtlasSlice(slice.x, slice.y, dimension.width, dimension.height),
                    AtlasSlice(slice.x + dimension.width, slice.y + dimension.height, brotherSlice.x, brotherSlice.y),
                    // below
                    AtlasSlice(slice.x, slice.y + dimension.height, slice.width - brotherSlice.x, brotherSlice.y),
                    // right
                    AtlasSlice(slice.x + dimension.width, slice.y, brotherSlice.x, slice.height - brotherSlice.y),
                )
            }
            brotherSlice.x >= minDimension.width -> {
                return listOf(
                    AtlasSlice(slice.x, slice.y, dimension.width, slice.height),
                    AtlasSlice(slice.x + dimension.width, slice.y, brotherSlice.x, slice.height),
                )
            }
            brotherSlice.y >= minDimension.height -> {
                return listOf(
                    AtlasSlice(slice.x, slice.y, slice.width, dimension.height),
                    AtlasSlice(slice.x, slice.y + dimension.height, slice.width, brotherSlice.y),
                )
            }
            else -> {
                return null
            }
        }
    }
}


class AtlasSliceHandle(
    val internalSlice: AtlasSlice
) {
    var wasFreed = false
        private set

    fun setFreed() {
        wasFreed = true
    }

    fun requireNotFreed() {
        check(!this.wasFreed) { "The slice was already freed!" }
    }

    val pos: Point
        get() {
            requireNotFreed()

            return Point(internalSlice.x, internalSlice.y)
        }
    val dimension: Point
        get() {
            requireNotFreed()

            return Point(internalSlice.width, internalSlice.height)
        }
}

class AtlasSlice(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    var parent: AtlasSlice? = null
    val children = ArrayList<AtlasSlice>()

    var isAllocated: Boolean = false
}
