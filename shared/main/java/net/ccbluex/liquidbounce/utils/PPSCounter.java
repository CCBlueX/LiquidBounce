package net.ccbluex.liquidbounce.utils;

public class PPSCounter
{
	private static final int MAX_PPS = 10000;
	private static final RollingArrayLongBuffer[] TIMESTAMP_BUFFERS = new RollingArrayLongBuffer[2];

	static
	{
		for (int i = 0, j = TIMESTAMP_BUFFERS.length; i < j; i++)
			TIMESTAMP_BUFFERS[i] = new RollingArrayLongBuffer(MAX_PPS);
	}

	/**
	 * Registers a mouse button click
	 *
	 * @param boundType
	 *               The packet type
	 */
	public static void registerPacket(final BoundType boundType)
	{
		TIMESTAMP_BUFFERS[boundType.index].add(System.currentTimeMillis());
	}

	/**
	 * Gets the count of clicks that have occurrence since the last 1000ms
	 *
	 * @param  boundType
	 *                The packet type
	 * @return        The PPT
	 */
	public static int getPacketCount(final BoundType boundType, final long time)
	{
		return TIMESTAMP_BUFFERS[boundType.index].getTimestampsSince(System.currentTimeMillis() - time);
	}

	public enum BoundType
	{
		INBOUND(0),
		OUTBOUND(1);

		final int index;

		BoundType(final int index)
		{
			this.index = index;
		}
	}
}
