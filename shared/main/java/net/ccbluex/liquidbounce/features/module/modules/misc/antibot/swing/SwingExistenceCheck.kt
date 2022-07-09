package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.swing

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class SwingExistenceCheck : BotCheck("swing.existence")
{
    override val isActive: Boolean
        get() = AntiBot.swingValue.get()

    private val swing = mutableSetOf<Int>()

    override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in swing

    override fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return

        val packet = event.packet

        if (classProvider.isSPacketAnimation(packet))
        {
            val swingPacket = packet.asSPacketAnimation()
            val entityId = swingPacket.entityID
            val entity = theWorld.getEntityByID(entityId)
            if (entity != null && classProvider.isEntityLivingBase(entity) && swingPacket.animationType == 0 && entityId !in swing) swing.add(entityId)
        }
    }

    override fun clear()
    {
        swing.clear()
    }
}
