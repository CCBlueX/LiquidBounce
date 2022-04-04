/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import java.util.*

@ModuleInfo(name = "Aimbot", description = "Automatically faces selected entities around you.", category = ModuleCategory.COMBAT)
class Aimbot : Module() {

    private val rangeValue = FloatValue("Range", 4.4F, 1F, 8F)
    private val turnSpeedValue = FloatValue("TurnSpeed", 10f, 1F, 180F)
    private val inViewTurnSpeed = FloatValue("InViewTurnSpeed", 35f, 1f, 180f)
    private val fovValue = FloatValue("FOV", 180F, 1F, 180F)
    private val centerValue = BoolValue("Center", false)
    private val lockValue = BoolValue("Lock", true)
    private val onClickValue = BoolValue("OnClick", false)
    private val jitterValue = BoolValue("Jitter", false)

    private val clickTimer = MSTimer()

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        // Clicking delay
        if (mc.gameSettings.keyBindAttack.isKeyDown)
            clickTimer.reset()

        if (onClickValue.get() && clickTimer.hasTimePassed(500L))
            return

        // Search for the best enemy to target

        val range = rangeValue.get()
        val entity = mc.theWorld.loadedEntityList
                .filter {
                    EntityUtils.isSelected(it, true) && mc.thePlayer.canEntityBeSeen(it) &&
                            mc.thePlayer.getDistanceToEntityBox(it) <= range && RotationUtils.getRotationDifference(it) <= fovValue.get()
                }
                .minByOrNull { RotationUtils.getRotationDifference(it) } ?: return

        // Should it always keep trying to lock on the enemy or just try to assist you?
        if (!lockValue.get() && RotationUtils.isFaced(entity, range.toDouble()))
            return

        // Look up required rotations to hit enemy
        val boundingBox = entity.entityBoundingBox ?: return

        val playerRotation = mc.thePlayer.rotation
        val destinationRotation = if (centerValue.get()) {
            RotationUtils.toRotation(RotationUtils.getCenter(boundingBox) ?: return, true)
        } else {
            RotationUtils.searchCenter(boundingBox, false, false, true, false, range)?.rotation ?: return
        }

        // Figure out the best turn speed suitable for the distance and configured turn speed

        val rotationDiff = RotationUtils.getRotationDifference(playerRotation, destinationRotation)

        // is enemy visible to player on screen. Fov is about to be right with that you can actually see on the screen. Still not 100% accurate, but it is fast check.
        val supposedTurnSpeed = if (rotationDiff < mc.gameSettings.fovSetting) {
            inViewTurnSpeed.get()
        } else {
            turnSpeedValue.get()
        }

        val random = Random()
        val gaussian = random.nextGaussian()

        val realisticTurnSpeed = rotationDiff * ((supposedTurnSpeed + (gaussian - 0.5)) / 180)
        val rotation = RotationUtils.limitAngleChange(mc.thePlayer.rotation, destinationRotation, realisticTurnSpeed.toFloat())

        rotation.toPlayer(mc.thePlayer)

        // Jitter
        // Some players do jitter on their mouses causing them to shake around. This is trying to simulate this behavior.
        if (jitterValue.get()) {
            val yaw = random.nextBoolean()
            val pitch = random.nextBoolean()

            if (yaw) {
                mc.thePlayer.rotationYaw += (random.nextGaussian() - 0.5).toFloat()
            }

            if (pitch) {
                mc.thePlayer.rotationPitch += (random.nextGaussian() - 0.5).toFloat()
                if (mc.thePlayer.rotationPitch > 90)
                    mc.thePlayer.rotationPitch = 90F
                else if (mc.thePlayer.rotationPitch < -90)
                    mc.thePlayer.rotationPitch = -90F
            }
        }
    }
}