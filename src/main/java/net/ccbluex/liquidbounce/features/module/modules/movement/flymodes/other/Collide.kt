package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.minecraft.init.Blocks.air
import net.minecraft.util.AxisAlignedBB

object Collide : FlyMode("Collide") {
    override fun onBB(event: BlockBBEvent) {
        if (event.block == air) {
            event.boundingBox = AxisAlignedBB(
                -2.0,
                -1.0,
                -2.0,
                2.0,
                1.0,
                2.0
            ).offset(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble()
            )
        }
    }
}