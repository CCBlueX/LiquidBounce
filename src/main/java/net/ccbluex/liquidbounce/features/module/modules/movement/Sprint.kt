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

object Sprint : Module("Sprint", "Automatically sprints all the time.", ModuleCategory.MOVEMENT) {
    val modeValue = ListValue("Mode", arrayOf("Legit", "Vanilla"), "Vanilla")

    val allDirectionsValue = object : BoolValue("AllDirections", true) {
        override fun isSupported() =modeValue.get() == "Vanilla"
    }

    private val blindnessValue = object : BoolValue("Blindness", true) {
        override fun isSupported() = modeValue.get() == "Vanilla"
    }

    val foodValue = object : BoolValue("Food", true) {
        override fun isSupported() = modeValue.get() == "Vanilla"
    }

    val checkServerSide = object : BoolValue("CheckServerSide", false) {
        override fun isSupported() = modeValue.get() == "Vanilla"
    }

    val checkServerSideGround = object : BoolValue("CheckServerSideOnlyGround", false) {
        override fun isSupported() = modeValue.get() == "Vanilla" && checkServerSide.get()
    }
    override val tag
        get() = modeValue.get()

    @EventTarget
    fun onTick(event: TickEvent) {
        if (modeValue.get() == "Legit")
            setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, true)
    }

    override fun onDisable() {
        if (modeValue.get() == "Legit") {
            val keyCode = mc.gameSettings.keyBindSprint.keyCode
            setKeyBindState(keyCode, keyCode > 0 && mc.gameSettings.keyBindSprint.isKeyDown)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (modeValue.get() == "Vanilla") {
            if (!isMoving || mc.thePlayer.isSneaking || blindnessValue.get()
                && mc.thePlayer.isPotionActive(Potion.blindness) || foodValue.get()
                && !(mc.thePlayer.foodStats.foodLevel > 6f || mc.thePlayer.capabilities.allowFlying)
                    || (checkServerSide.get() && (mc.thePlayer.onGround || !checkServerSideGround.get())
                        && !allDirectionsValue.get() && targetRotation != null)
                    && getRotationDifference(mc.thePlayer.rotation) > 30
            ) {
                mc.thePlayer.isSprinting = false
                return
            } else

            if (allDirectionsValue.get() || mc.thePlayer.movementInput.moveForward >= 0.8f)
                mc.thePlayer.isSprinting = true
        }
    }
}
