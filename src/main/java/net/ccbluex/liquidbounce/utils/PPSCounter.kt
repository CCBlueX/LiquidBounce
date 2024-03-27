/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

object PPSCounter {
    private val TIMESTAMP_BUFFERS = Array(PacketType.values().size) { RollingArrayLongBuffer(Int.MAX_VALUE) }

    /**
     * Registers a packet type
     *
     * @param type The type
     */
    fun registerType(type: PacketType) = TIMESTAMP_BUFFERS[type.ordinal].add(System.currentTimeMillis())

    /**
     * Gets the count of sent and received packets that have occurred in the last 1000ms
     *
     * @param type The packet type
     * @return The PPS
     */
    fun getPPS(type: PacketType) = TIMESTAMP_BUFFERS[type.ordinal].getTimestampsSince(System.currentTimeMillis() - 1000L)

    enum class PacketType { SEND, RECEIVE }
}
