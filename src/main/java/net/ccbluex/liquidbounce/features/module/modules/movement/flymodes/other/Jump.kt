/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.init.Blocks.air
import net.minecraft.util.AxisAlignedBB

object Jump : FlyMode("Jump") {

    override fun onUpdate() {
        if (mc.thePlayer == null) {
            return
        }
        if (mc.thePlayer.onGround && MovementUtils.isMoving) {
            mc.thePlayer.jump()
        }
    }

    override fun onBB(event: BlockBBEvent) {
        if (event.block == air && event.y.toDouble() < startY) {
            event.boundingBox = AxisAlignedBB.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x.toDouble() + 1,
                startY,
                event.z.toDouble() + 1
            )
        }
    }
}
