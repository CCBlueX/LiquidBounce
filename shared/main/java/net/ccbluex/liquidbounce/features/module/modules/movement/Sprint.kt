/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "Sprint", description = "Automatically sprints all the time.", category = ModuleCategory.MOVEMENT)
class Sprint : Module() {

    @JvmField
    val allDirectionsValue = BoolValue("AllDirections", true)
    @JvmField
    val blindnessValue = BoolValue("Blindness", true)
    @JvmField
    val foodValue = BoolValue("Food", true)
    @JvmField
    val checkServerSide = BoolValue("CheckServerSide", false)
    @JvmField
    val checkServerSideGround = BoolValue("CheckServerSideOnlyGround", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!MovementUtils.isMoving || mc.thePlayer!!.sneaking ||
            (blindnessValue.get() && mc.thePlayer!!.isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS))) ||
            (foodValue.get() && !(mc.thePlayer!!.foodStats.foodLevel > 6.0F || mc.thePlayer!!.capabilities.allowFlying))
            || (checkServerSide.get() && (mc.thePlayer!!.onGround || !checkServerSideGround.get())
                    && !allDirectionsValue.get() && RotationUtils.targetRotation != null
                    && RotationUtils.getRotationDifference(Rotation(mc.thePlayer!!.rotationYaw, mc.thePlayer!!.rotationPitch)) > 30)) {
            mc.thePlayer!!.sprinting = false
            return
        }
        if (allDirectionsValue.get() || mc.thePlayer!!.movementInput.moveForward >= 0.8F)
            mc.thePlayer!!.sprinting = true
    }
}
