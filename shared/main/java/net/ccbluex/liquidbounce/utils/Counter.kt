/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

/**
 * @author superblaubeere27
 */
object CPSCounter
{
	private const val MAX_CPS = 50
	private val TIMESTAMP_BUFFERS = arrayOfNulls<RollingArrayLongBuffer>(MouseButton.values().size)

	/**
	 * Registers a mouse button click
	 *
	 * @param button
	 * The clicked button
	 */
	@JvmStatic
	fun registerClick(button: MouseButton)
	{
		TIMESTAMP_BUFFERS[button.index]?.add()
	}

	/**
	 * Gets the count of clicks that have occurrence since the last 1000ms
	 *
	 * @param  button
	 * The mouse button
	 * @return        The CPS
	 */
	fun getCPS(button: MouseButton): Int = TIMESTAMP_BUFFERS[button.index]?.getTimestampsSince(System.currentTimeMillis() - 1000L) ?: 0

	enum class MouseButton(val index: Int)
	{
		LEFT(0),
		MIDDLE(1),
		RIGHT(2);
	}

	init
	{
		for (i in TIMESTAMP_BUFFERS.indices) TIMESTAMP_BUFFERS[i] = RollingArrayLongBuffer(MAX_CPS)
	}
}

object PacketCounter
{
	private const val MAX_PPS = 2048
	private val TIMESTAMP_BUFFERS = arrayOfNulls<RollingArrayLongBuffer>(PacketType.values().size)

	/**
	 * Registers a mouse button click
	 *
	 * @param packetType
	 * The packet type
	 */
	@JvmStatic
	fun registerPacket(packetType: PacketType)
	{
		TIMESTAMP_BUFFERS[packetType.index]?.add()
	}

	/**
	 * Gets the count of packets that have occurrence since the last 1000ms
	 *
	 * @param  packetType
	 * The packet type
	 * @return        The PPT
	 */
	fun getPacketCount(packetType: PacketType, time: Long): Int = TIMESTAMP_BUFFERS[packetType.index]?.getTimestampsSince(System.currentTimeMillis() - time) ?: 0

	enum class PacketType(val index: Int)
	{
		INBOUND(0),
		OUTBOUND(1);
	}

	init
	{
		for (i in TIMESTAMP_BUFFERS.indices) TIMESTAMP_BUFFERS[i] = RollingArrayLongBuffer(MAX_PPS)
	}
}
