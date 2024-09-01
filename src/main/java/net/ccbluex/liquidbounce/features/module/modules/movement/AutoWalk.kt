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
import net.minecraft.client.option.GameOptions

object AutoWalk : Module("AutoWalk", Category.MOVEMENT, subjective = true, gameDetecting = false, hideModule = false) {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.options.forwardKey.pressed = true
    }

    override fun onDisable() {
        if (!GameOptions.isPressed(mc.options.forwardKey))
            mc.options.forwardKey.pressed = false
    }
}
