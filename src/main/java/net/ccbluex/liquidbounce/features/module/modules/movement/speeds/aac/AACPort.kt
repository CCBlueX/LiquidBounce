/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.portMax
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import kotlin.math.cos
import kotlin.math.sin

class AACPort : SpeedMode("AACPort") {
    override fun onMotion() {}
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!isMoving)
            return

        val f = thePlayer.rotationYaw * 0.017453292f
        var d = 0.2

        while (d <= portMax.get()) {
            val x = thePlayer.posX - sin(f) * d
            val z = thePlayer.posZ + cos(f) * d

            if (thePlayer.posY < thePlayer.posY.toInt() + 0.5 && getBlock(BlockPos(x, thePlayer.posY, z)) != Blocks.air)
                break
            sendPacket(C04PacketPlayerPosition(x, thePlayer.posY, z, true))
            d += 0.2
        }
    }

    override fun onMove(event: MoveEvent) {}
}