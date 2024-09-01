/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.block.AbstractFluidBlock

object WaterSpeed : Module("WaterSpeed", Category.MOVEMENT, gameDetecting = false) {
    private val speed by FloatValue("Speed", 1.2f, 1.1f..1.5f)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        if (player.isTouchingWater && getBlock(player.position) is AbstractFluidBlock) {
            val speed = speed

            player.velocityX *= speed
            player.velocityZ *= speed
        }
    }
}