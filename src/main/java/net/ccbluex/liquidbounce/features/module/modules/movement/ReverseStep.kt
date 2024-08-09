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
import net.minecraft.util.AxisAlignedBB

object ReverseStep : Module("ReverseStep", Category.MOVEMENT) {

    private val motion by FloatValue("Motion", 1f, 0.21f..1f)
    private var jumped = false

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (player.onGround)
            jumped = false

        if (player.motionY > 0)
            jumped = true

        if (!handleEvents())
            return

        if (collideBlock(player.entityBoundingBox) { it is BlockLiquid } ||
            collideBlock(AxisAlignedBB.fromBounds(player.entityBoundingBox.maxX, player.entityBoundingBox.maxY, player.entityBoundingBox.maxZ, player.entityBoundingBox.minX, player.entityBoundingBox.minY - 0.01, player.entityBoundingBox.minZ)) {
                it is BlockLiquid
            }) return

        if (!mc.gameSettings.keyBindJump.isKeyDown && !player.onGround && !player.movementInput.jump && player.motionY <= 0.0 && player.fallDistance <= 1f && !jumped)
            player.motionY = (-motion).toDouble()
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent) {
        jumped = true
    }

}