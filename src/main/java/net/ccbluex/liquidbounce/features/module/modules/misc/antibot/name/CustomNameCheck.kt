package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class CustomNameCheck : BotCheck("name.customName")
{
    override val isActive: Boolean
        get() = AntiBot.customNameEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val customName = target.customNameTag.let { if (AntiBot.customNameStripColorsValue.get()) ColorUtils.stripColor(it) else it }

        if (customName.isBlank()) return AntiBot.customNameBlankValue.get()

        val compareTo = if (AntiBot.customNameCompareToValue.get().equals("DisplayName", ignoreCase = true)) target.displayName.formattedText else target.gameProfile.name
        if (compareTo != null && !(if (AntiBot.customNameModeValue.get().equals("Equals", ignoreCase = true)) compareTo == customName else customName in compareTo)) return true

        return false
    }
}
