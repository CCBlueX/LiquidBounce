/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category

object NoBob : Module("NoBob", Category.RENDER, gameDetecting = false, hideModule = false) {

    @EventTarget
    fun onMotion(event: MotionEvent) {
        mc.player?.distanceWalkedModified = -1f
    }
}
