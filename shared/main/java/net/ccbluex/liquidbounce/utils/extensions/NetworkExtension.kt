package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.api.minecraft.client.network.INetworkPlayerInfo

fun INetworkPlayerInfo.getFullName(): String {
    if (displayName != null)
        return displayName!!.formattedText

    val team = playerTeam
    val name = gameProfile.name
    return team?.formatString(name) ?: name
}