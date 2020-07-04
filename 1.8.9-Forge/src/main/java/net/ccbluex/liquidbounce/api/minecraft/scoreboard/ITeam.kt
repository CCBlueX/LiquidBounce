/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.scoreboard

import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChatFormatting

interface ITeam {
    val chatFormat: WEnumChatFormatting

    fun formatString(name: String): String
    fun isSameTeam(team: ITeam): Boolean
}