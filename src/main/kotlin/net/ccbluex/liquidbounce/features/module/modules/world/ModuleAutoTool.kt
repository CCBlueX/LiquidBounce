package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.BlockAttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object ModuleAutoTool : Module("AutoTool", Category.WORLD) {

    val handler = handler<BlockAttackEvent> { event ->
        var bestSpeed = 1F
        var bestSlot = -1

        val blockState = world.getBlockState(event.pos)
        for (i in 0..8) {
            val item = player.inventory.getStack(i) ?: continue
            val speed = item.getMiningSpeedMultiplier(blockState)

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }

        if (bestSlot != -1)
            player.inventory.selectedSlot = bestSlot
    }
}
