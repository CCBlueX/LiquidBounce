/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.minecraft.entity.player.EntityPlayer

object EntityUtils : MinecraftInstance()
{

    @JvmField
    var targetInvisible = false

    @JvmField
    var targetPlayer = true

    @JvmField
    var targetMobs = true

    @JvmField
    var targetAnimals = false

    @JvmField
    var targetArmorStand = false

    @JvmField
    var targetDead = false

    @JvmStatic
    fun getPlayerHealthFromScoreboard(playername: String?, isMineplex: Boolean): Int
    {
        val theWorld = mc.theWorld ?: return 0
        val thePlayer = mc.thePlayer ?: return 0

        val scoreboard = theWorld.scoreboard
        val multiplier = if (isMineplex) 2 else 1

        theWorld.loadedEntityList.filterIsInstance<EntityPlayer>().filter { it != thePlayer }.forEach { entity ->
            val profileName = entity.gameProfile.name
            scoreboard.getObjectivesForEntity(profileName).values.filter { playername.equals(profileName, ignoreCase = true) }.forEach { return@getPlayerHealthFromScoreboard it.scorePoints * multiplier }
        }

        return 0
    }
}
