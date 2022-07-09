package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils

class TabCheck : BotCheck("tab.tab")
{
    override val isActive: Boolean
        get() = AntiBot.tabEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val useDisplayName = AntiBot.tabNameModeValue.get().equals("DisplayName", ignoreCase = true)
        val equals = AntiBot.tabModeValue.get().equals("Equals", ignoreCase = true)
        val stripColors = AntiBot.tabStripColorsValue.get()
        val tryStripColors = { string: String -> if (stripColors) ColorUtils.stripColor(string) else string }

        val targetName = (if (useDisplayName) target.displayName.formattedText else target.gameProfile.name)?.let(tryStripColors)
        return targetName != null && !mc.netHandler.playerInfoMap.map { networkPlayerInfo -> tryStripColors(networkPlayerInfo.getFullName(useDisplayName)) }.any { networkName -> if (equals) targetName == networkName else networkName in targetName }
    }
}
