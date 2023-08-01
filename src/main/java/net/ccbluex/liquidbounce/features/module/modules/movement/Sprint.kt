/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.RotationUtils.targetRotation
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.KeyBinding.setKeyBindState
import net.minecraft.potion.Potion
import net.minecraft.util.MathHelper

object Sprint : Module("Sprint", ModuleCategory.MOVEMENT) {
    val mode by ListValue("Mode", arrayOf("Legit", "Vanilla"), "Vanilla")

    val allDirections by BoolValue("AllDirections", true) { mode == "Vanilla" }

    val jumpDirections by BoolValue("JumpDirections", false) { mode == "Vanilla" && allDirections }

    private val allDirectionsLimitSpeed by FloatValue("AllDirectionsLimitSpeed", 1f, 0.75f..1f) { mode == "Vanilla" && allDirections }

    private val allDirectionsLimitSpeedGround by BoolValue("AllDirectionsLimitSpeedOnlyGround", true) { mode == "Vanilla" && allDirections }

    private val blindness by BoolValue("Blindness", true) { mode == "Vanilla" }

    private val usingItem by BoolValue("UsingItem", false) { mode == "Vanilla" }

    val food by BoolValue("Food", true) { mode == "Vanilla" }

    val checkServerSide by BoolValue("CheckServerSide", false) { mode == "Vanilla" }

    val checkServerSideGround by BoolValue("CheckServerSideOnlyGround", false) { mode == "Vanilla" && checkServerSide }
    override val tag
        get() = mode

    @EventTarget
    fun onTick(event: TickEvent) {
        if (mode == "Legit") setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, true)
    }

    override fun onDisable() {
        if (mode == "Legit") {
            val keyCode = mc.gameSettings.keyBindSprint.keyCode
            setKeyBindState(keyCode, keyCode > 0 && mc.gameSettings.keyBindSprint.isKeyDown)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val targetRotation = targetRotation
        val movementInput = mc.thePlayer.movementInput

        val rotationYaw = mc.thePlayer.rotationYaw

        val shouldStop =
            targetRotation != null && movementInput.moveForward * MathHelper.cos((rotationYaw - targetRotation.yaw).toRadians()) + movementInput.moveStrafe * MathHelper.sin(
                (rotationYaw - targetRotation.yaw).toRadians()
            ) < 0.8

        if (mode == "Vanilla") {
            if (!isMoving || mc.thePlayer.isSneaking || blindness && mc.thePlayer.isPotionActive(Potion.blindness) || usingItem && mc.thePlayer.isUsingItem || food && !(mc.thePlayer.foodStats.foodLevel > 6f || mc.thePlayer.capabilities.allowFlying) || (checkServerSide && (mc.thePlayer.onGround || !checkServerSideGround) && !allDirections && shouldStop)) {
                mc.thePlayer.isSprinting = false
                return
            }

            if (mc.thePlayer.movementInput.moveForward >= 0.8f) {
                mc.thePlayer.isSprinting = true
            }
            else if (allDirections) {
                mc.thePlayer.isSprinting = true
                if (!allDirectionsLimitSpeedGround || mc.thePlayer.onGround) {
                    mc.thePlayer.motionX *= allDirectionsLimitSpeed
                    mc.thePlayer.motionZ *= allDirectionsLimitSpeed
                }
            }
        }
    }
}
