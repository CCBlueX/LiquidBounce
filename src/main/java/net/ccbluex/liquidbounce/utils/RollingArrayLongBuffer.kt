/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

/**
 * A buffer which stores its contents in an array.
 * You can only add contents to it. If you add more elements than it can hold it will overflow and
 * overwrite the first element. Made to improve performance for time measurements.
 *
 * @author superblaubeere27
 */
class RollingArrayLongBuffer(length: Int) {
    /**
     * @return The contents of the buffer
     */
    var contents: LongArray
        private set

    private var currentIndex = 0

    init {
        contents = LongArray(length)
    }

    /**
     * Adds an element to the buffer
     *
     * @param l The element to be added
     */
    fun add(l: Long) {
        currentIndex = (currentIndex + 1) % contents.size
        contents[currentIndex] = l
    }

    /**
     * Gets the count of elements added in a row
     * which are higher than l
     *
     * @param l The threshold timestamp
     * @return The count
     */
    fun getTimestampsSince(l: Long): Int {
        for (i in contents.indices) {
            if (contents[if (currentIndex < i) contents.size - i + currentIndex else currentIndex - i] < l) {
                return i
            }
        }

        // If every element is lower than l, return the array length
        return contents.size
    }
}
