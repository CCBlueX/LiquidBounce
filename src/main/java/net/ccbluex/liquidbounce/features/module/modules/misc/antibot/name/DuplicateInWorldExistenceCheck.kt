package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class DuplicateInWorldExistenceCheck : BotCheck("tab.duplicateInWorld.existence")
{
    override val isActive: Boolean
        get() = AntiBot.duplicateInWorldExistenceEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val stripColors = AntiBot.duplicateInWorldExistenceStripColorsValue.get()
        val tryStripColors = { string: String -> if (stripColors) ColorUtils.stripColor(string) else string }

        val mode = AntiBot.duplicateInWorldExistenceNameModeValue.get().lowercase()
        val entityName = when (mode)
        {
            "displayname" -> target.displayName.formattedText
            "customnametag" -> target.customNameTag.ifBlank { target.gameProfile.name }
            else -> target.gameProfile.name
        }?.let(tryStripColors)

        return AntiBot.duplicateInWorldExistenceEnabledValue.get() && theWorld.loadedEntityList.filterIsInstance<EntityPlayer>().count {
            entityName == when (mode)
            {
                "displayname" -> it.displayName.formattedText
                "customnametag" -> it.customNameTag.ifBlank { it.gameProfile.name }
                else -> it.gameProfile.name
            }
        } > 1
    }
}
