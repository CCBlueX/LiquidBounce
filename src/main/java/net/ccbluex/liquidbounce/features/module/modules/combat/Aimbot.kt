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
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.random.Random

@ModuleInfo(name = "Aimbot", description = "Automatically faces selected entities around you.", category = ModuleCategory.COMBAT)
class Aimbot : Module() {

    private val rangeValue = FloatValue("Range", 4.4F, 1F, 8F)
    private val turnSpeedValue = FloatValue("TurnSpeed", 2F, 1F, 180F)
    private val fovValue = FloatValue("FOV", 180F, 1F, 180F)
    private val centerValue = BoolValue("Center", false)
    private val lockValue = BoolValue("Lock", true)
    private val onClickValue = BoolValue("OnClick", false)
    private val jitterValue = BoolValue("Jitter", false)

    private val clickTimer = MSTimer()

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (mc.gameSettings.keyBindAttack.isKeyDown)
            clickTimer.reset()

        if (onClickValue.get() && clickTimer.hasTimePassed(500L))
            return

        val player = mc.thePlayer ?: return

        val range = rangeValue.get()
        val entity = mc.theWorld.loadedEntityList
                .filter {
                    EntityUtils.isSelected(it, true) && player.canEntityBeSeen(it) &&
                            player.getDistanceToEntityBox(it) <= range && RotationUtils.getRotationDifference(it) <= fovValue.get()
                }
                .minByOrNull { RotationUtils.getRotationDifference(it) } ?: return

        if (!lockValue.get() && RotationUtils.isFaced(entity, range.toDouble()))
            return

        val boundingBox = entity.entityBoundingBox ?: return

        val destinationRotation = if (centerValue.get()) {
            RotationUtils.toRotation(RotationUtils.getCenter(boundingBox) ?: return, true)
        } else {
            RotationUtils.searchCenter(boundingBox, false, false, true, false, range).rotation
        }
        val rotation = RotationUtils.limitAngleChange(player.rotation, destinationRotation, (turnSpeedValue.get() + Math.random()).toFloat())

        rotation.toPlayer(player)

        if (jitterValue.get()) {
            val yaw = Random.nextBoolean()
            val pitch = Random.nextBoolean()
            val yawNegative = Random.nextBoolean()
            val pitchNegative = Random.nextBoolean()

            if (yaw)
                player.rotationYaw += if (yawNegative) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)

            if (pitch) {
                player.rotationPitch += if (pitchNegative) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)
                if (player.rotationPitch > 90)
                    player.rotationPitch = 90F
                else if (player.rotationPitch < -90)
                    player.rotationPitch = -90F
            }
        }
    }
}