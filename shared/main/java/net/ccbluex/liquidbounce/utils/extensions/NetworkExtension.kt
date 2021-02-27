package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.api.minecraft.client.network.INetworkPlayerInfo

fun INetworkPlayerInfo.getFullName(useDisplayNameIfPresent: Boolean): String
{
	val displayName = displayName

	if (useDisplayNameIfPresent && displayName != null) return displayName.formattedText

	val name = gameProfile.name

	return playerTeam?.formatString(name) ?: name
}
