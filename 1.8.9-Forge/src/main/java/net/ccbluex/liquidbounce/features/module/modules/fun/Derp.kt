package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "Derp", description = "Makes it look like you were derping around.", category = ModuleCategory.FUN)
class Derp : Module() {

    private val headlessValue = BoolValue("Headless", false)
    private val spinnyValue = BoolValue("Spinny", false)
    private val incrementValue = FloatValue("Increment", 1F, 0F, 50F)

    private var currentSpin = 0F

    val rotation: FloatArray
        get() {
            val derpRotations = floatArrayOf(mc.thePlayer.rotationYaw + (Math.random() * 360 - 180).toFloat(), (Math.random() * 180 - 90).toFloat())

            if (headlessValue.get())
                derpRotations[1] = 180F

            if (spinnyValue.get()) {
                derpRotations[0] = currentSpin + incrementValue.get()
                currentSpin = derpRotations[0]
            }

            return derpRotations
        }
}