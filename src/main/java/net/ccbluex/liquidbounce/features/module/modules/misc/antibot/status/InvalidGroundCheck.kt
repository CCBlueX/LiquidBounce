package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

class InvalidGroundCheck : BotCheck("status.invalidGround")
{
    override val isActive: Boolean
        get() = AntiBot.invalidGroundValue.get()

    private val vl = mutableMapOf<Int, Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in vl

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
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
