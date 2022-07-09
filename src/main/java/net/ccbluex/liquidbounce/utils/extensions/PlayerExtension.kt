/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor

val IEntityPlayer.ping: Int
	get()
	{
		val playerInfo = wrapper.minecraft.netHandler.getPlayerInfo(uniqueID)
		return playerInfo?.responseTime ?: 0
	}

fun IEntityPlayer.isClientFriend(): Boolean = LiquidBounce.fileManager.friendsConfig.isFriend(stripColor(name))
