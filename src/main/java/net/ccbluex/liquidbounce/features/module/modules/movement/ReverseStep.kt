/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.block.BlockLiquid
import net.minecraft.util.Box

object ReverseStep : Module("ReverseStep", Category.MOVEMENT) {

    private val motion by FloatValue("Motion", 1f, 0.21f..1f)
    private var jumped = false

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (thePlayer.onGround)
            jumped = false

        if (thePlayer.velocityY > 0)
            jumped = true

        if (!handleEvents())
            return

        if (collideBlock(thePlayer.boundingBox) { it is BlockLiquid } ||
            collideBlock(Box.fromBounds(thePlayer.boundingBox.maxX, thePlayer.boundingBox.maxY, thePlayer.boundingBox.maxZ, thePlayer.boundingBox.minX, thePlayer.boundingBox.minY - 0.01, thePlayer.boundingBox.minZ)) {
                it is BlockLiquid
            }) return

        if (!mc.options.jumpKey.isPressed && !thePlayer.onGround && !thePlayer.movementInput.jump && thePlayer.velocityY <= 0.0 && thePlayer.fallDistance <= 1f && !jumped)
            thePlayer.velocityY = (-motion).toDouble()
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent) {
        jumped = true
    }

}