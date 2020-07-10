/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.network

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation

interface INetworkPlayerInfo {
    val locationSkin: IResourceLocation
    val responseTime: Int
    val gameProfile: GameProfile
    val playerTeam: ITeam?
    val displayName: IIChatComponent?
}