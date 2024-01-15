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
import net.ccbluex.liquidbounce.features.module.modules.player.Reach
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.isFaced
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.Entity
import java.util.*

object Aimbot : Module("Aimbot", ModuleCategory.COMBAT) {

    private val range by FloatValue("Range", 4.4F, 1F..8F)
    private val turnSpeed by FloatValue("TurnSpeed", 10f, 1F..180F)
    private val inViewTurnSpeed by FloatValue("InViewTurnSpeed", 35f, 1f..180f)
    private val predictClientMovement by IntegerValue("PredictClientMovement", 2, 0..5)
    private val predictEnemyPosition by FloatValue("PredictEnemyPosition", 1.5f, -1f..2f)
    private val fov by FloatValue("FOV", 180F, 1F..180F)
    private val center by BoolValue("Center", false)
    private val lock by BoolValue("Lock", true)
    private val onClick by BoolValue("OnClick", false)
    private val jitter by BoolValue("Jitter", false)

    private val clickTimer = MSTimer()

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST)
            return

        val thePlayer = mc.thePlayer ?: return

        // Clicking delay
        if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()

        if (onClick && (clickTimer.hasTimePassed(150) || (!mc.gameSettings.keyBindAttack.isKeyDown && AutoClicker.handleEvents()))) return

        // Search for the best enemy to target
        val entity = mc.theWorld.loadedEntityList.filter {
            var result = false

            Backtrack.runWithNearestTrackedDistance(it) {
                result = isSelected(it, true)
                    && thePlayer.canEntityBeSeen(it)
                    && thePlayer.getDistanceToEntityBox(it) <= range
                    && getRotationDifference(it) <= fov
            }

            result
        }.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) } ?: return

        // Should it always keep trying to lock on the enemy or just try to assist you?
        if (!lock && isFaced(entity, range.toDouble())) return

        val random = Random()

        var shouldReturn = false

        Backtrack.runWithNearestTrackedDistance(entity) {
            shouldReturn = !findRotation(entity, random)
        }

        if (shouldReturn) {
            return
        }

        // Jitter
        // Some players do jitter on their mouses causing them to shake around. This is trying to simulate this behavior.
        if (jitter) {
            if (random.nextBoolean()) {
                thePlayer.fixedSensitivityYaw += (random.nextGaussian() - 0.5f).toFloat()
            }

            if (random.nextBoolean()) {
                thePlayer.fixedSensitivityPitch += (random.nextGaussian() - 0.5f).toFloat()
            }
        }
    }

    private fun findRotation(entity: Entity, random: Random): Boolean {
        val player = mc.thePlayer ?: return false

        val (predictX, predictY, predictZ) = entity.currPos.subtract(entity.prevPos)
            .times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(predictX, predictY, predictZ)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)

        repeat(predictClientMovement + 1) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val playerRotation = player.rotation

        val destinationRotation = if (center) {
            toRotation(boundingBox.center, true)
        } else {
            searchCenter(boundingBox,
                outborder = false,
                random = false,
                gaussianOffset = false,
                predict = true,
                lookRange = range,
                attackRange = if (Reach.handleEvents()) Reach.combatReach else 3f
            )
        }

        if (destinationRotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)

            return false
        }

        // Figure out the best turn speed suitable for the distance and configured turn speed
        val rotationDiff = getRotationDifference(playerRotation, destinationRotation)

        // is enemy visible to player on screen. Fov is about to be right with that you can actually see on the screen. Still not 100% accurate, but it is fast check.
        val supposedTurnSpeed = if (rotationDiff < mc.gameSettings.fovSetting) {
            inViewTurnSpeed
        } else {
            turnSpeed
        }

        val gaussian = random.nextGaussian()

        val realisticTurnSpeed = rotationDiff * ((supposedTurnSpeed + (gaussian - 0.5)) / 180)
        val rotation = limitAngleChange(player.rotation, destinationRotation, realisticTurnSpeed.toFloat())

        rotation.toPlayer(player)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }
}