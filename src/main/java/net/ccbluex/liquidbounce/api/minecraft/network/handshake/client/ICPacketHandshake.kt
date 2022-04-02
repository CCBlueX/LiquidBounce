/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.handshake.client

import net.ccbluex.liquidbounce.api.minecraft.network.IEnumConnectionState
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket

interface ICPacketHandshake : IPacket {
    val port: Int
    var ip: String
    val requestedState: IEnumConnectionState
}