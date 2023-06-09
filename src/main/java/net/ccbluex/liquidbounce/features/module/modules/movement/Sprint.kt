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
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.targetRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.KeyBinding.setKeyBindState
import net.minecraft.potion.Potion

object Sprint : Module("Sprint", ModuleCategory.MOVEMENT) {
    val mode by ListValue("Mode", arrayOf("Legit", "Vanilla"), "Vanilla")

    val allDirections by BoolValue("AllDirections", true) { mode == "Vanilla" }

    private val blindness by BoolValue("Blindness", true) { mode == "Vanilla" }

    val food by BoolValue("Food", true) { mode == "Vanilla" }

    val checkServerSide by BoolValue("CheckServerSide", false) { mode == "Vanilla" }

    val checkServerSideGround by BoolValue("CheckServerSideOnlyGround", false) { mode == "Vanilla" && checkServerSide }
    override val tag
        get() = mode

    @EventTarget
    fun onTick(event: TickEvent) {
        if (mode == "Legit")
            setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, true)
    }

    override fun onDisable() {
        if (mode == "Legit") {
            val keyCode = mc.gameSettings.keyBindSprint.keyCode
            setKeyBindState(keyCode, keyCode > 0 && mc.gameSettings.keyBindSprint.isKeyDown)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mode == "Vanilla") {
            if (!isMoving || mc.thePlayer.isSneaking || blindness
                && mc.thePlayer.isPotionActive(Potion.blindness) || food
                && !(mc.thePlayer.foodStats.foodLevel > 6f || mc.thePlayer.capabilities.allowFlying)
                    || (checkServerSide && (mc.thePlayer.onGround || !checkServerSideGround)
                        && !allDirections && targetRotation != null)
                    && getRotationDifference(mc.thePlayer.rotation) > 30
            ) {
                mc.thePlayer.isSprinting = false
                return
            } else

            if (allDirections || mc.thePlayer.movementInput.moveForward >= 0.8f)
                mc.thePlayer.isSprinting = true
        }
    }
}
