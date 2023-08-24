/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.item.*

object HypixelHop : SpeedMode("HypixelHop") {
    override fun onMotion() {
        val thePlayer = mc.thePlayer ?: return
        val itemInUse = mc.thePlayer.itemInUse?.item

        if (!isMoving || thePlayer.isInWater || thePlayer.isInLava )
            return

        if (!NoSlow.state && (itemInUse is ItemSword || itemInUse is ItemFood || itemInUse is ItemPotion || itemInUse is ItemBucketMilk || itemInUse is ItemBow)) {
            return
        }else {
            if (!NoSlow.getNoSlowBlockState() && itemInUse is ItemSword)
                return

            if (!NoSlow.getNoSlowConsumeState() && (itemInUse is ItemFood || itemInUse is ItemPotion || itemInUse is ItemBucketMilk))
                return

            if (!NoSlow.getNoSlowBowState() && itemInUse is ItemBow)
                return
        }
        if (thePlayer.onGround) {
            thePlayer.jump()
            strafe(0.4f)
        }
    }
}
