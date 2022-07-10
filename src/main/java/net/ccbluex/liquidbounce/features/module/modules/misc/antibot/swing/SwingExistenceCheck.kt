package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.swing

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0BPacketAnimation

class SwingExistenceCheck : BotCheck("swing.existence")
{
    override val isActive: Boolean
        get() = AntiBot.swingValue.get()

    private val swing = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in swing

    override fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return

        val packet = event.packet

        if (packet is S0BPacketAnimation)
        {
            val entityId = packet.entityID
            val entity = theWorld.getEntityByID(entityId)
            if (entity != null && entity is EntityLivingBase && packet.animationType == 0 && entityId !in swing) swing.add(entityId)
        }
    }

    override fun clear()
    {
        swing.clear()
    }
}
