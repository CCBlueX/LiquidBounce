/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

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
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion

object VerusFHop : SpeedMode("VerusFHop") {

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
            if (player.onGround) {
                if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
                  strafe(0.4825f)
              } else {
                  strafe(0.535f)
              }

                player.tryJump()
            } else {
              if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
                  strafe(0.334f)
              } else {
                  strafe(0.3345f)
              }
          }
    }
}
