/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import com.mojang.authlib.GameProfile

interface ISession {
    val profile: GameProfile
    val username: String
    val playerId: String
    val sessionType: String

    val token: String
}