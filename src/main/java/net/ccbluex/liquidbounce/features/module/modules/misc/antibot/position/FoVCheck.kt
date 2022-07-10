package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

class FoVCheck : BotCheck("position.fov")
{
    override val isActive: Boolean
        get() = AntiBot.fovEnabledValue.get()

    private val vl = mutableMapOf<Int, Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val entityId = target.entityId
        return (vl[entityId] ?: 0) >= AntiBot.fovVLLimitValue.get()
    }

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId

        val deltaLimit = AntiBot.fovFoVValue.get().toDouble()
        val delta = RotationUtils.getRotationDifference(RotationUtils.toRotation(thePlayer, newPos.plus(0.0, target.height * 0.5, 0.0), false, RotationUtils.MinMaxPair.ZERO), getPingCorrectionAppliedLocation(thePlayer, AntiBot.fovPingCorrectionOffsetValue.get()).rotation)

        if (delta > deltaLimit) vl[entityId] = (vl[entityId] ?: 0) + 1
        else if (AntiBot.fovVLDecValue.get())
        {
            val currentVL = (vl[entityId] ?: 0) - 5
            if (currentVL <= 0) vl.remove(entityId) else vl[entityId] = currentVL
        }
    }

    override fun clear()
    {
        vl.clear()
    }
}
