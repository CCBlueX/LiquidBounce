/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.Companion.LOCK_CENTER
import net.ccbluex.liquidbounce.utils.extensions.getEntitiesInRadius
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "Derp", description = "Makes it look like you were derping around.", category = ModuleCategory.FUN)
class Derp : Module()
{
    private val yawModeValue = ListValue("Yaw", arrayOf("Off", "Static", "Jitter", "Switch", "Random", "Spin"), "Random", "Mode", "Yaw rotation mode")
    private val yawOffsetValue = object : FloatValue("YawOffset", 0f, -180f, 180f, description = "Static yaw value")
    {
        override fun showCondition() = yawModeValue.get().equals("Static", ignoreCase = true) || yawModeValue.get().equals("Jitter", ignoreCase = true)
    }
    private val yawToEntityValue = object : BoolValue("ToEntity", true, description = "If enabled, Yaw rotation will affected by nearby enemies as Anti-Aim")
    {
        override fun showCondition() = yawModeValue.get().equals("Static", ignoreCase = true) || yawModeValue.get().equals("Jitter", ignoreCase = true)
    }
    private val yawToEntityDistanceValue = object : FloatValue("EntityDistance", 8f, 6f, 32f)
    {
        override fun showCondition() = (yawModeValue.get().equals("Static", ignoreCase = true) || yawModeValue.get().equals("Jitter", ignoreCase = true)) && yawToEntityValue.get()
    }
    private val yawToEntityPriorityValue = object : ListValue("EntityPriority", arrayOf("Distance", "Direction"), "Distance")
    {
        override fun showCondition() = (yawModeValue.get().equals("Static", ignoreCase = true) || yawModeValue.get().equals("Jitter", ignoreCase = true)) && yawToEntityValue.get()
    }
    private val yawSpinAmountValue = object : FloatValue("SpinAmount", 1F, 0F, 180F, "Increment")
    {
        override fun showCondition() = yawModeValue.get().equals("Spin", ignoreCase = true)
    }
    private val yawJitterIntensityValue = object : FloatValue("JitterIntensity", 10F, 0F, 180F)
    {
        override fun showCondition() = yawModeValue.get().equals("Jitter", ignoreCase = true)
    }
    private val yawSwitchIntensityValue = object : FloatValue("SwitchIntensity", 25F, 0F, 180F)
    {
        override fun showCondition() = yawModeValue.get().equals("Switch", ignoreCase = true)
    }

    private val pitchModeValue = ListValue("Pitch", arrayOf("Off", "Static", "Random", "RandomHeadless", "Headless"), "Random")
    private val pitchOffsetValue = object : FloatValue("StaticPitch", 85f, -90f, 90f)
    {
        override fun showCondition() = pitchModeValue.get().equals("Static", ignoreCase = true)
    }

    private var currentSpin = 0F
    private var switch = false

    val rotation: FloatArray
        get()
        {
            val theWorld = mc.theWorld ?: return floatArrayOf(0f, 0f)
            val thePlayer = mc.thePlayer ?: return floatArrayOf(0f, 0f)
            val yawOffset = yawOffsetValue.get()

            val yaw = lazy(LazyThreadSafetyMode.NONE) {
                ((if (yawToEntityValue.get())
                {
                    val distance = yawToEntityDistanceValue.get()
                    val entities = theWorld.getEntitiesInRadius(thePlayer, distance.toDouble()).filter { it.isSelected(false) }
                    (if (yawToEntityPriorityValue.get().equals("Distance", ignoreCase = true)) entities.minBy { it.getDistanceSqToEntity(thePlayer) } else entities.minBy { RotationUtils.getClientRotationDifference(thePlayer, it, false, RotationUtils.MinMaxPair.ZERO) })?.let { RotationUtils.searchCenter(theWorld, thePlayer, it.entityBoundingBox, LOCK_CENTER, null, RotationUtils.MinMaxPair.ZERO, distance, 0.0, 0, 0.0)?.rotation?.yaw } ?: thePlayer.rotationYaw
                }
                else thePlayer.rotationYaw) + 180f + yawOffset) % 360f
            }

            return floatArrayOf(when (yawModeValue.get().toLowerCase())
            {
                "random" -> thePlayer.rotationYaw + RandomUtils.nextFloat(-180f, 180f)
                "static" -> yaw.value
                "jitter" -> yaw.value + RandomUtils.nextFloat(-yawJitterIntensityValue.get(), yawJitterIntensityValue.get())

                "spin" -> (currentSpin + yawSpinAmountValue.get()).also { currentSpin = it }

                "switch" ->
                {
                    switch = !switch
                    yaw.value + yawSwitchIntensityValue.get() * if (switch) 1f else -1f
                }

                else -> thePlayer.rotationYaw
            }, when (pitchModeValue.get().toLowerCase())
            {
                "static" -> pitchOffsetValue.get()
                "random" -> RandomUtils.nextFloat(-90f, 90f)
                "randomheadless" -> RandomUtils.nextFloat(-180f, 180f)
                "headless" -> 180f
                else -> thePlayer.rotationPitch
            })
        }
}
