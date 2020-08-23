/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IServerData
import net.minecraft.client.multiplayer.ServerData

class ServerDataImpl(val wrapped: ServerData) : IServerData {
    override val pingToServer: Long
        get() = wrapped.pingToServer
    override val version: Int
        get() = wrapped.version
    override val gameVersion: String
        get() = wrapped.gameVersion
    override val serverMOTD: String
        get() = wrapped.serverMOTD
    override val populationInfo: String
        get() = wrapped.populationInfo
    override val serverName: String
        get() = wrapped.serverName
    override val serverIP: String
        get() = wrapped.serverIP


    override fun equals(other: Any?): Boolean {
        return other is ServerDataImpl && other.wrapped == this.wrapped
    }
}

inline fun IServerData.unwrap(): ServerData = (this as ServerDataImpl).wrapped
inline fun ServerData.wrap(): IServerData = ServerDataImpl(this)