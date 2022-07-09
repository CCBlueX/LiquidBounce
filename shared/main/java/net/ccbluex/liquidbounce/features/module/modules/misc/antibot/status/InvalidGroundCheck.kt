package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class InvalidGroundCheck : BotCheck("status.invalidGround")
{
    override val isActive: Boolean
        get() = AntiBot.invalidGroundValue.get()

    private val vl = mutableMapOf<Int, Int>()

    override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in vl

    override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId

        if (isTeleport) return

        val previousVL = vl[entityId] ?: 0
        if (onGround)
        {
            if ((previousVL + 5) % 10 == 0) notification(target) { arrayOf("delta=${target.prevPosY - target.posY}") }
            if (target.prevPosY != target.posY) vl[entityId] = previousVL + 2
        }
        else
        {
            val currentVL = previousVL - 1
            if (currentVL <= 0) vl.remove(entityId) else vl[entityId] = currentVL
        }
    }

    override fun clear()
    {
        vl.clear()
    }
}
