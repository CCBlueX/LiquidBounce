/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.client.network.NetworkPlayerInfo

fun NetworkPlayerInfo.getFullName(): String {
    if (displayName != null)
        return displayName.formattedText

    val team = playerTeam
    val name = gameProfile.name
    return team?.formatString(name) ?: name
}