package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.injection.forge.mixins.network.MixinNetworkManager
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet

fun NetworkPlayerInfo.getFullName(useDisplayNameIfPresent: Boolean): String
{
    val displayName = displayName

    if (useDisplayNameIfPresent && displayName != null) return displayName.formattedText

    val name = gameProfile.name

    return playerTeam?.formatString(name) ?: name
}

@Suppress("CAST_NEVER_SUCCEEDS")
fun NetworkManager.sendPacketWithoutEvent(packet: Packet<*>) = (this as MixinNetworkManager).sendPacketWithoutEvent(packet)

