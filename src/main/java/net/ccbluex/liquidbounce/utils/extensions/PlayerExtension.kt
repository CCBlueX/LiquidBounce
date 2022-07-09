/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.entity.player.EntityPlayer

val EntityPlayer.ping: Int
    get()
    {
        val playerInfo = mc.netHandler.getPlayerInfo(uniqueID)
        return playerInfo?.responseTime ?: 0
    }

fun EntityPlayer.isClientFriend(): Boolean = LiquidBounce.fileManager.friendsConfig.isFriend(stripColor(name))
