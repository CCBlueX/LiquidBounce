/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customAirStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customAirTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customAirTimerTick
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customGroundStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customGroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.customY
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notOnConsuming
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notOnFalling
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notOnVoid
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.stopY
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.minecraft.item.MilkBucketItem
import net.minecraft.item.FoodItem
import net.minecraft.item.PotionItem

object CustomSpeed : SpeedMode("Custom") {

    override fun onMotion() {
        val player = mc.player ?: return
        val mainHandStack = player.mainHandStack

        val fallingPlayer = FallingPlayer()
        if (notOnVoid && fallingPlayer.findCollision(500) == null
            || notOnFalling && player.fallDistance > 2.5f
            || notOnConsuming && player.isUsingItem
                    && (mainHandStack.item is FoodItem
                    || mainHandStack.item is PotionItem
                    || mainHandStack.item is MilkBucketItem)
            ) {

            if (player.onGround) player.tryJump()
            mc.ticker.timerSpeed = 1f
            return
        }

        if (isMoving) {
            if (player.onGround) {
                if (customGroundStrafe > 0) {
                    strafe(customGroundStrafe)
                }

                mc.ticker.timerSpeed = customGroundTimer
                player.velocityY = customY.toDouble()
            } else {
                if (customAirStrafe > 0) {
                    strafe(customAirStrafe)
                }

                if (player.ticksAlive % customAirTimerTick == 0) {
                    mc.ticker.timerSpeed = customAirTimer
                } else {
                    mc.ticker.timerSpeed = 1f
                }
            }
        }
    }

    override fun onEnable() {
        val player = mc.player ?: return

        if (Speed.resetXZ) player.stopXZ()
        if (Speed.resetY) player.stopY()

        super.onEnable()
    }

}