/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import io.netty.util.concurrent.GenericFutureListener
import net.ccbluex.liquidbounce.api.minecraft.INetworkManager
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.injection.implementations.IMixinNetworkManager
import net.minecraft.network.NetworkManager
import javax.crypto.SecretKey

class NetworkManagerImpl(val wrapped: NetworkManager) : INetworkManager
{
	override val lastPacket: Long
		get() = (wrapped as IMixinNetworkManager).lastPacket()

	override fun sendPacket(packet: IPacket) = wrapped.sendPacket(packet.unwrap())
	override fun sendPacket(packet: IPacket, listener: () -> Unit) = wrapped.sendPacket(packet.unwrap(), GenericFutureListener { listener() })

	override fun sendPacketWithoutEvent(packet: IPacket)
	{
		(wrapped as IMixinNetworkManager).sendPacketWithoutEvent(packet.unwrap())
	}

	override fun enableEncryption(secretKey: SecretKey) = wrapped.enableEncryption(secretKey)

	override fun equals(other: Any?): Boolean = other is NetworkManagerImpl && other.wrapped == wrapped
}

inline fun INetworkManager.unwrap(): NetworkManager = (this as NetworkManagerImpl).wrapped
inline fun NetworkManager.wrap(): INetworkManager = NetworkManagerImpl(this)
