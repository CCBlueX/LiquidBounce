package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class EntityIDCheck : BotCheck("misc.entityID")
{
    override val isActive: Boolean
        get() = AntiBot.entityIDEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val entityId = target.entityId

        if (entityId < 0 || entityId >= AntiBot.entityIDLimitValue.get()) return true

        if (AntiBot.entityIDStaticEntityIDEntityIDCountValue.get() > 0)
        {
            val ids = arrayOf(AntiBot.entityIDStaticEntityIDEntityID1Value.get(), AntiBot.entityIDStaticEntityIDEntityID2Value.get(), AntiBot.entityIDStaticEntityIDEntityID3Value.get())
            if ((0 until AntiBot.entityIDStaticEntityIDEntityIDCountValue.get()).map(ids::get).any { entityId == it }) return true
        }

        return false
    }
}
