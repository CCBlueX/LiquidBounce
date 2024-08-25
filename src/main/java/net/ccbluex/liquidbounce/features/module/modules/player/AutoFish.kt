/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.item.ItemFishingRod

object AutoFish : Module("AutoFish", Category.PLAYER, subjective = true, gameDetecting = false, hideModule = false) {

    private val rodOutTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player

        if (thePlayer?.mainHandStack == null || mc.player.mainHandStack.item !is ItemFishingRod)
            return

        val fishEntity = thePlayer.fishEntity

        if (rodOutTimer.hasTimePassed(500) && fishEntity == null || (fishEntity != null && fishEntity.velocityX == 0.0 && fishEntity.velocityZ == 0.0 && fishEntity.velocityY != 0.0)) {
            mc.rightClickMouse()
            rodOutTimer.reset()
        }
    }
}
