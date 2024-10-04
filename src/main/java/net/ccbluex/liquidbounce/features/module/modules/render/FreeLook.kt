/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.RotationSetEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.extensions.prevRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation

object FreeLook : Module("FreeLook", Category.RENDER) {

    // The module's rotations
    private var currRotation = Rotation.ZERO
    private var prevRotation = currRotation

    // The player's rotations
    private var savedCurrRotation = Rotation.ZERO
    private var savedPrevRotation = Rotation.ZERO

    override fun onEnable() {
        mc.thePlayer?.run {
            currRotation = rotation
            prevRotation = prevRotation
        }
    }

    @EventTarget
    fun onRotationSet(event: RotationSetEvent) {
        if (mc.gameSettings.thirdPersonView != 0) {
            event.cancelEvent()
        }

        prevRotation = currRotation
        currRotation += Rotation(event.yawDiff, -event.pitchDiff)

        currRotation.withLimitedPitch()
    }

    fun useModifiedRotation() {
        val player = mc.thePlayer ?: return

        savedCurrRotation = player.rotation
        savedPrevRotation = player.prevRotation

        if (!handleEvents())
            return

        player.rotation = currRotation
        player.prevRotation = prevRotation
    }

    fun restoreOriginalRotation() {
        val player = mc.thePlayer ?: return

        if (mc.gameSettings.thirdPersonView == 0) {
            savedCurrRotation = player.rotation
            savedPrevRotation = player.prevRotation
            return
        }

        if (!handleEvents())
            return

        player.rotation = savedCurrRotation
        player.prevRotation = savedPrevRotation
    }
}