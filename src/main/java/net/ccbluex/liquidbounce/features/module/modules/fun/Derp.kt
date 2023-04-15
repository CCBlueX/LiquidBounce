/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

object Derp : Module("Derp", ModuleCategory.FUN) {

    private val headlessValue = BoolValue("Headless", false)
    private val spinnyValue = BoolValue("Spinny", false)
    private val incrementValue = object : FloatValue("Increment", 1F, 0F, 50F) {
        override fun isSupported() = spinnyValue.get()
    }

    private var currentSpin = 0F

    val rotation: Rotation
        get() {
            val rot = Rotation(mc.thePlayer.rotationYaw + nextFloat(-180f, 180f), nextFloat(-90f, 90f))

            if (headlessValue.get())
                rot.pitch = 180F

            if (spinnyValue.get()) {
                currentSpin += incrementValue.get()
                rot.yaw = currentSpin
            }

            rot.fixedSensitivity()

            return rot
        }

}