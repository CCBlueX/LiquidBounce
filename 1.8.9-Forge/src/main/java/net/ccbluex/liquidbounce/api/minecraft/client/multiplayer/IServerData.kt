/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.multiplayer

interface IServerData {
    val pingToServer: Int
    val version: Int
    val gameVersionString: String
    val serverMOTD: String
    val populationInfo: Int
    val serverName: String
    val serverIP: String
}
