package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import kotlin.math.abs

class VerticalSpeedCheck : BotCheck("move.vspeed")
{
    override val isActive: Boolean
        get() = AntiBot.vspeedEnabledValue.get()

    private val vl = mutableMapOf<Int, Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val entityId = target.entityId
        return entityId in vl && (!AntiBot.vspeedVLValue.get() || (vl[entityId] ?: 0) >= AntiBot.vspeedVLLimitValue.get())
    }

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId
        val speed = abs(target.posY - newPos.yCoord)
        if (speed > AntiBot.vspeedLimitValue.get())
        {
            notification(target) { arrayOf("delta=${StringUtils.DECIMALFORMAT_6.format(speed)}") }
            vl[entityId] = (vl[entityId] ?: 0) + 2
        }
        else if (AntiBot.vspeedVLDecValue.get())
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
