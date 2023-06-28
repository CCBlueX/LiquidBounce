/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationUtils.getCenter
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.isFaced
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import java.util.*

object Aimbot : Module("Aimbot", ModuleCategory.COMBAT) {

    private val range by FloatValue("Range", 4.4F, 1F..8F)
    private val turnSpeed by FloatValue("TurnSpeed", 10f, 1F..180F)
    private val inViewTurnSpeed by FloatValue("InViewTurnSpeed", 35f, 1f..180f)
    private val fov by FloatValue("FOV", 180F, 1F..180F)
    private val center by BoolValue("Center", false)
    private val lock by BoolValue("Lock", true)
    private val onClick by BoolValue("OnClick", false)
    private val jitter by BoolValue("Jitter", false)

    private val clickTimer = MSTimer()

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST) {
            return
        }

        // Clicking delay
        if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()

        if (onClick && clickTimer.hasTimePassed(500)) return

        // Search for the best enemy to target

        val entity = mc.theWorld.loadedEntityList.filter {
            isSelected(
                it, true
            ) && mc.thePlayer.canEntityBeSeen(it) && mc.thePlayer.getDistanceToEntityBox(it) <= range && getRotationDifference(
                it
            ) <= fov
        }.minByOrNull { getRotationDifference(it) } ?: return

        // Should it always keep trying to lock on the enemy or just try to assist you?
        if (!lock && isFaced(entity, range.toDouble())) return

        // Look up required rotations to hit enemy
        val boundingBox = entity.hitBox

        val playerRotation = mc.thePlayer.rotation
        val destinationRotation = if (center) toRotation(getCenter(boundingBox), true)
        else searchCenter(boundingBox, false, false, true, false, range)?.rotation ?: return

        // Figure out the best turn speed suitable for the distance and configured turn speed

        val rotationDiff = getRotationDifference(playerRotation, destinationRotation)

        // is enemy visible to player on screen. Fov is about to be right with that you can actually see on the screen. Still not 100% accurate, but it is fast check.
        val supposedTurnSpeed = if (rotationDiff < mc.gameSettings.fovSetting) {
            inViewTurnSpeed
        } else {
            turnSpeed
        }

        val random = Random()
        val gaussian = random.nextGaussian()

        val realisticTurnSpeed = rotationDiff * ((supposedTurnSpeed + (gaussian - 0.5)) / 180)
        val rotation = limitAngleChange(mc.thePlayer.rotation, destinationRotation, realisticTurnSpeed.toFloat())

        rotation.toPlayer(mc.thePlayer)

        // Jitter
        // Some players do jitter on their mouses causing them to shake around. This is trying to simulate this behavior.
        if (jitter) {
            if (random.nextBoolean()) {
                mc.thePlayer.fixedSensitivityYaw += (random.nextGaussian() - 0.5f).toFloat()
            }

            if (random.nextBoolean()) {
                mc.thePlayer.fixedSensitivityPitch += (random.nextGaussian() - 0.5f).toFloat()
            }
        }
    }
}