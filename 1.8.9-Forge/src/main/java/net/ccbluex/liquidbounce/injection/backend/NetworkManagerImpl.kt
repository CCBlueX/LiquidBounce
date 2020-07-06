/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.INetworkManager
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.minecraft.network.NetworkManager

class NetworkManagerImpl(val wrapped: NetworkManager) : INetworkManager {
    override fun sendPacket(packet: IPacket) = wrapped.sendPacket(packet.unwrap())


    override fun equals(other: Any?): Boolean {
        return other is NetworkManagerImpl && other.wrapped == this.wrapped
    }
}

inline fun INetworkManager.unwrap(): NetworkManager = (this as NetworkManagerImpl).wrapped
inline fun NetworkManager.wrap(): INetworkManager = NetworkManagerImpl(this)