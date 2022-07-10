package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3

class NoClipCheck : BotCheck("move.noClip")
{
    override val isActive: Boolean
        get() = AntiBot.noClipEnabledValue.get()

    private val vl = mutableMapOf<Int, Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val entityId = target.entityId
        return entityId in vl && ((vl[entityId] ?: 0) >= AntiBot.noClipVLLimitValue.get())
    }

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId
        val rayTraceResult = theWorld.rayTraceBlocks(Vec3(target.posX, target.posY, target.posZ), newPos)
        if (rayTraceResult?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            vl[entityId] = (vl[entityId] ?: 0) + 20
        }
        else if (AntiBot.noClipVLDecValue.get())
        {
            val currentVL = (vl[entityId] ?: 0) - 1
            if (currentVL <= 0) vl.remove(entityId) else vl[entityId] = currentVL
        }
    }

    override fun clear()
    {
        vl.clear()
    }
}
