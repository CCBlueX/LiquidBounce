/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.block.BlockCactus
import net.minecraft.util.Box

object AntiCactus : Module("AntiCactus", Category.PLAYER, gameDetecting = false, hideModule = false) {

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (event.block is BlockCactus)
            event.boundingBox = Box(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(),
                event.x + 1.0, event.y + 1.0, event.z + 1.0)
    }
}
