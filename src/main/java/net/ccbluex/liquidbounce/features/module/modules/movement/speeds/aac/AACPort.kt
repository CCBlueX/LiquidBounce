/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.block.BlockAir
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos

class AACPort : SpeedMode("AACPort")
{
    override fun onMotion(eventState: EventState)
    {
    }

    override fun onUpdate()
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.cantBoostUp) return

        val dir = thePlayer.moveDirectionDegrees
        var speed = 0.2
        val maxSpeed = Speed.portMax.get()

        while (speed <= maxSpeed)
        {
            val (x, z) = thePlayer.getForwardAmount(speed, dir)
            val y = thePlayer.posY

            if (y < y.toInt() + 0.5 && theWorld.getBlock(BlockPos(x, y, z)) !is BlockAir) break

            thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(x, y, z, true))
            speed += 0.2
        }
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
