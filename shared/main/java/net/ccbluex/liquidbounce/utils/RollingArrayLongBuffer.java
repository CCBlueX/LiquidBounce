/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

/**
 * A buffer which stores it's contents in an array. You can only add contents to it. If you add more elements than it can hold it will overflow and overwrite the first element. Made to improve performance for time measurements.
 *
 * @author superblaubeere27
 */
public class RollingArrayLongBuffer
{
	private final long[] buffer;
	private int bufferOffset;

	public RollingArrayLongBuffer(final int length)
	{
		try
		{
			buffer = new long[length];
		}
		catch (final OutOfMemoryError e)
		{
			ClientUtils.getLogger().error("Can't allocate the buffer", e);
			throw e;
		}
	}

	/**
	 * @return The contents of the buffer
	 */
	public long[] getBuffer()
	{
		return buffer;
	}

	/**
	 * Adds an element to the buffer
	 *
	 * @param l
	 *          The element to be added
	 */
	public final void add(final long l)
	{
		bufferOffset = (bufferOffset + 1) % buffer.length;
		buffer[bufferOffset] = l;
	}

	/**
	 * Gets the count of elements added in a row which are higher than l
	 *
	 * @param  l
	 *           The threshold timestamp
	 * @return   The count
	 */
	public final int getTimestampsSince(final long l)
	{
		for (int i = 0, j = buffer.length; i < j; i++)
			if (buffer[bufferOffset < i ? buffer.length - i + bufferOffset : bufferOffset - i] < l)
				return i;

		// If every element is lower than l, return the array length
		return buffer.length;
	}
}
