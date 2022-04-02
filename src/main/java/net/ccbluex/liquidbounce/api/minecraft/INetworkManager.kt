/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import javax.crypto.SecretKey

interface INetworkManager {
    fun sendPacket(packet: IPacket)
    fun enableEncryption(secretKey: SecretKey)
    fun sendPacket(packet: IPacket, any: () -> Unit)
}