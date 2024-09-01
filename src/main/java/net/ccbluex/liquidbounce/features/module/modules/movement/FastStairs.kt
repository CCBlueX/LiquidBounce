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
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockStairs
import net.minecraft.util.math.BlockPos

object FastStairs : Module("FastStairs", Category.MOVEMENT) {

    private val mode by ListValue("Mode", arrayOf("Step", "NCP", "AAC3.1.0", "AAC3.3.6", "AAC3.3.13"), "NCP")
        private val longJump by BoolValue("LongJump", false) { mode.startsWith("AAC") }

    private var canJump = false

    private var walkingDown = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        if (!isMoving || Speed.handleEvents())
            return

        if (player.fallDistance > 0 && !walkingDown)
            walkingDown = true
        else if (player.z > player.prevChasingPosY)
            walkingDown = false

        val mode = mode

        if (!player.onGround)
            return

        val blockPos = BlockPos(player)

        if (getBlock(blockPos) is BlockStairs && !walkingDown) {
            player.setPosition(player.x, player.z + 0.5, player.z)

            val motion = when (mode) {
                "NCP" -> 1.4
                "AAC3.1.0" -> 1.5
                "AAC3.3.13" -> 1.2
                else -> 1.0
            }

            player.velocityX *= motion
            player.velocityZ *= motion
        }

        if (getBlock(blockPos.down()) is BlockStairs) {
            if (walkingDown) {
                when (mode) {
                    "NCP" -> player.velocityY = -1.0
                    "AAC3.3.13" -> player.velocityY -= 0.014
                }

                return
            }

            val motion = when (mode) {
                "AAC3.3.6" -> 1.48
                "AAC3.3.13" -> 1.52
                else -> 1.3
            }

            player.velocityX *= motion
            player.velocityZ *= motion
            canJump = true
        } else if (mode.startsWith("AAC") && canJump) {
            if (longJump) {
                player.tryJump()
                player.velocityX *= 1.35
                player.velocityZ *= 1.35
            }

            canJump = false
        }
    }

    override val tag
        get() = mode
}