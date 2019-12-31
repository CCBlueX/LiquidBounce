package net.ccbluex.liquidbounce.chat

import net.ccbluex.liquidbounce.chat.packet.packets.Packet

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
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