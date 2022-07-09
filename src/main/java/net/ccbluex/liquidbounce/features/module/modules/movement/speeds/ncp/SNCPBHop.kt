/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.api.minecraft.client.entity.EntityLivingBase
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionRadians
import net.ccbluex.liquidbounce.utils.extensions.multiply
import net.ccbluex.liquidbounce.utils.extensions.speedEffectAmplifier
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.hypot
import kotlin.math.roundToInt

class SNCPBHop : SpeedMode("SNCPBHop")
{
    private var step = 1
    private var moveSpeed = 0.2873
    private var lastSpeed = 0.0
    private var timerDelay = 0

    override fun onEnable()
    {
        mc.timer.timerSpeed = 1f
        lastSpeed = 0.0
        moveSpeed = 0.0
        step = 4
    }

    override fun onDisable()
    {
        moveSpeed = getBaseMoveSpeed(mc.thePlayer ?: return)
        mc.timer.timerSpeed = 1f
        step = 0
    }

    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        lastSpeed = hypot(thePlayer.posX - thePlayer.prevPosX, thePlayer.posZ - thePlayer.prevPosZ)
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer

        ++timerDelay
        timerDelay %= 5

        val move = thePlayer.isMoving

        if (timerDelay != 0) timer.timerSpeed = 1f
        else if (move)
        {
            timer.timerSpeed = 1.3f

            thePlayer.multiply(1.0199999809265137)
        }

        if (thePlayer.onGround && move) step = 2

        if (round(thePlayer.posY - thePlayer.posY.roundToInt()) == 0.138)
        {
            val d = 0.09316090325960147

            thePlayer.motionY -= 0.08
            event.y -= d
            thePlayer.posY -= d
        }

        val baseMoveSpeed = getBaseMoveSpeed(thePlayer)

        when (step)
        {
            1 -> if (move)
            {
                step = 2

                moveSpeed = 1.35 * baseMoveSpeed - 0.01
            }

            2 ->
            {
                step = 3

                val d = 0.399399995803833

                thePlayer.motionY = d
                event.y = d
                moveSpeed *= 2.149
            }

            3 ->
            {
                step = 4

                val difference = 0.66 * (lastSpeed - baseMoveSpeed)
                moveSpeed = lastSpeed - difference
            }

            5 ->
            {
                moveSpeed = baseMoveSpeed
                lastSpeed = 0.0
                step = 6
            }

            6 ->
            {
                if (theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically) step = 1

                lastSpeed = 0.0
                moveSpeed = baseMoveSpeed

                return
            }

            else ->
            {
                if (theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0)).isNotEmpty() || thePlayer.isCollidedVertically)
                {
                    moveSpeed = baseMoveSpeed
                    lastSpeed = 0.0
                    step = 5

                    return
                }

                moveSpeed = lastSpeed - lastSpeed / 159.0
            }
        }

        moveSpeed = moveSpeed.coerceAtLeast(baseMoveSpeed)

        if (move)
        {
            val func = functions

            val dir = thePlayer.moveDirectionRadians
            event.x = -func.sin(dir) * moveSpeed
            event.z = func.cos(dir) * moveSpeed

            thePlayer.stepHeight = 0.5f
        }
        else event.zeroXZ()
    }

    private fun getBaseMoveSpeed(thePlayer: EntityLivingBase): Double = 0.2873 * (1.0 + 0.2 * thePlayer.speedEffectAmplifier)

    private fun round(value: Double): Double = BigDecimal(value).setScale(3, RoundingMode.HALF_UP).toDouble()
}
