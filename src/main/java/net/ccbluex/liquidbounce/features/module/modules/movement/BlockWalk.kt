/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB

object BlockWalk : Module("BlockWalk", ModuleCategory.MOVEMENT) {
    private val cobweb by BoolValue("Cobweb", true)
    private val snow by BoolValue("Snow", true)

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (cobweb && event.block == Blocks.web || snow && event.block == Blocks.snow_layer)
            event.boundingBox = AxisAlignedBB.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(),
                    event.x + 1.0, event.y + 1.0, event.z + 1.0)
    }
}
