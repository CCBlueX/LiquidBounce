/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.client.network.PlayerListEntry

fun PlayerListEntry.getFullName(): String {
    if (displayName != null)
        return displayName.asFormattedString()

    val team = scoreboardTeam
    val name = profile.name
    return team?.decorateName(name) ?: name
}