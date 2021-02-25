package net.ccbluex.liquidbounce.utils

object PPSCounter
{
	private const val MAX_PPS = 2048
	private val TIMESTAMP_BUFFERS = arrayOfNulls<RollingArrayLongBuffer>(BoundType.values().size)

	/**
	 * Registers a mouse button click
	 *
	 * @param boundType
	 * The packet type
	 */
	@JvmStatic
	fun registerPacket(boundType: BoundType)
	{
		TIMESTAMP_BUFFERS[boundType.index]?.add(System.currentTimeMillis())
	}

	/**
	 * Gets the count of clicks that have occurrence since the last 1000ms
	 *
	 * @param  boundType
	 * The packet type
	 * @return        The PPT
	 */
	fun getPacketCount(boundType: BoundType, time: Long): Int = TIMESTAMP_BUFFERS[boundType.index]?.getTimestampsSince(System.currentTimeMillis() - time) ?: 0

	enum class BoundType(val index: Int)
	{
		INBOUND(0),
		OUTBOUND(1);
	}

	init
	{
		for (i in TIMESTAMP_BUFFERS.indices) TIMESTAMP_BUFFERS[i] = RollingArrayLongBuffer(MAX_PPS)
	}
}
