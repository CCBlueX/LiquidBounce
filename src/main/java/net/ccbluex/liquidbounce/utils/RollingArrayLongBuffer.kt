/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.minecraft.util.*

/**
 * A buffer which stores it's contents in an array. You can only add contents to it. If you add more elements than it can hold it will overflow and overwrite the first element. Made to improve performance for time measurements.
 *
 * @author superblaubeere27
 */
class RollingArrayLongBuffer(length: Int)
{
    /**
     * @return The contents of the buffer
     */
    val buffer: LongArray = try
    {
        LongArray(length)
    }
    catch (e: OutOfMemoryError)
    {
        logger.error("Can't allocate the buffer", e)
        throw e
    }

    /**
     * The offset of the buffer
     */
    private var bufferOffset = 0

    /**
     * Adds an element to the buffer
     *
     * @param l
     * The element to be added
     */
    fun add()
    {
        bufferOffset = (bufferOffset + 1) % buffer.size
        buffer[bufferOffset] = System.currentTimeMillis()
    }

    /**
     * Gets the count of elements added in a row which are higher than l
     *
     * @param  l
     * The threshold timestamp
     * @return   The count
     */
    fun getTimestampsSince(l: Long): Int
    {
        var i = 0
        val j = buffer.size

        try
        {
            while (i < j)
            {
                if (buffer[if (bufferOffset < i) buffer.size - i + bufferOffset else bufferOffset - i] < l) return i
                i++
            }
        }
        catch (_: ArrayIndexOutOfBoundsException)
        {
        }

        // If every element is lower than l (or Index Exception occurs), return the array length
        return buffer.size
    }
}
