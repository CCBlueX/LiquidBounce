/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import kotlin.math.cos
import kotlin.math.sin

class AACPort : SpeedMode("AACPort") {
    override fun onMotion() {}
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!MovementUtils.isMoving)
            return

        val f = thePlayer.rotationYaw * 0.017453292f
        var d = 0.2

        while (d <= (LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed?)!!.portMax.get()) {
            val x: Double = thePlayer.posX - sin(f) * d
            val z: Double = thePlayer.posZ + cos(f) * d

            if (thePlayer.posY < thePlayer.posY.toInt() + 0.5 && !classProvider.isBlockAir(getBlock(WBlockPos(x, thePlayer.posY, z))))
                break
            thePlayer.sendQueue.addToSendQueue(classProvider.createCPacketPlayerPosition(x, thePlayer.posY, z, true))
            d += 0.2
        }
    }

    override fun onMove(event: MoveEvent) {}
}