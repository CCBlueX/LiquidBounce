/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.chat

import net.ccbluex.liquidbounce.chat.packet.packets.Packet

interface ClientListener {

    /**
     * Handle connect to web socket
     */
    fun onConnect()

    /**
     * Handle connect to web socket
     */
    fun onConnected()

    /**
     * Handle handshake
     */
    fun onHandshake(success: Boolean)

    /**
     * Handle disconnect
     */
    fun onDisconnect()

    /**
     * Handle logon to web socket with minecraft account
     */
    fun onLogon()

    /**
     * Handle incoming packets
     */
    fun onPacket(packet: Packet)

    /**
     * Handle error
     */
    fun onError(cause: Throwable)

}