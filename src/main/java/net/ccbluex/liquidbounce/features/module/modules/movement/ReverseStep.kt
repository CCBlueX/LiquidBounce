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
import net.minecraft.block.AbstractFluidBlock
import net.minecraft.util.Box

object ReverseStep : Module("ReverseStep", Category.MOVEMENT) {

    private val motion by FloatValue("Motion", 1f, 0.21f..1f)
    private var jumped = false

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        if (player.onGround)
            jumped = false

        if (player.velocityY > 0)
            jumped = true

        if (!handleEvents())
            return

        if (collideBlock(player.boundingBox) { it is AbstractFluidBlock } ||
            collideBlock(Box.fromBounds(player.boundingBox.maxX, player.boundingBox.maxY, player.boundingBox.maxZ, player.boundingBox.minX, player.boundingBox.minY - 0.01, player.boundingBox.minZ)) {
                it is AbstractFluidBlock
            }) return

        if (!mc.options.jumpKey.isPressed && !player.onGround && !player.movementInput.jump && player.velocityY <= 0.0 && player.fallDistance <= 1f && !jumped)
            player.velocityY = (-motion).toDouble()
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent) {
        jumped = true
    }

}