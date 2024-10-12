/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.syncSpecialModuleRotations
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

object Derp : Module("Derp", Category.FUN, subjective = true, hideModule = false) {

    private val headless by BoolValue("Headless", false)
    private val spinny by BoolValue("Spinny", false)
    private val increment by FloatValue("Increment", 1F, 0F..50F) { spinny }

    override fun onDisable() {
        syncSpecialModuleRotations()
    }

    val rotation: Rotation
        get() {
            val rotationToUse = currentRotation ?: serverRotation

            val rot = Rotation(rotationToUse.yaw, nextFloat(-90f, 90f))

            if (headless)
                rot.pitch = 180F

            rot.yaw += if (spinny) increment else nextFloat(-180f, 180f)

            return rot.fixedSensitivity()
        }

}