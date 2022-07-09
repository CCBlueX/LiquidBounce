package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils

class DuplicateInTabExistenceCheck : BotCheck("tab.duplicateInTab.existence")
{
    override val isActive: Boolean
        get() = AntiBot.duplicateInTabExistenceEnabledValue.get()

    override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean
    {
        val stripColors = AntiBot.duplicateInTabExistenceStripColorsValue.get()
        val tryStripColors = { string: String -> if (stripColors) ColorUtils.stripColor(string) else string }

        val mode = AntiBot.duplicateInTabExistenceModeValue.get().toLowerCase()
        val entityName = when (mode)
        {
            "displayname" -> target.displayName.formattedText
            "customnametag" -> target.customNameTag.ifBlank { target.gameProfile.name }
            else -> target.gameProfile.name
        }?.let(tryStripColors)

        val useDisplayName = AntiBot.duplicateInTabExistenceNameModeValue.get().equals("DisplayName", ignoreCase = true)
        return mc.netHandler.playerInfoMap.count { entityName == tryStripColors(it.getFullName(useDisplayName)) } > 1
    }
}
