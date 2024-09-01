/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

object Derp : Module("Derp", Category.FUN, subjective = true, hideModule = false) {

    private val headless by BoolValue("Headless", false)
    private val spinny by BoolValue("Spinny", false)
        private val increment by FloatValue("Increment", 1F, 0F..50F) { spinny }

    private var currentSpin = 0F

    val rotation: Rotation
        get() {
            val rot = Rotation(mc.player.yaw + nextFloat(-180f, 180f), nextFloat(-90f, 90f))

            if (headless)
                rot.pitch = 180F

            if (spinny) {
                currentSpin += increment
                rot.yaw = currentSpin
            }

            rot.fixedSensitivity()

            return rot
        }

}