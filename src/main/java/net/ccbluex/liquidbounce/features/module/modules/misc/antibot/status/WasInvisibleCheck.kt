package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class WasInvisibleCheck : BotCheck("misc.wasInvisible")
{
    override val isActive: Boolean
        get() = AntiBot.wasInvisibleValue.get()

    private val wasInvisible = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId in wasInvisible

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId
        if (target.invisible && entityId !in wasInvisible) wasInvisible.add(entityId)
    }

    override fun clear()
    {
        wasInvisible.clear()
    }
}
