/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityPitch
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityYaw
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.GameSettings

object AntiAFK : Module("AntiAFK", Category.PLAYER, gameDetecting = false, hideModule = false) {

    private val mode by ListValue("Mode", arrayOf("Old", "Random", "Custom"), "Random")

        private val rotateValue = BoolValue("Rotate", true) { mode == "Custom" }
            private val rotationDelay by IntegerValue("RotationDelay", 100, 0..1000) { rotateValue.isActive() }
            private val rotationAngle by FloatValue("RotationAngle", 1f, -180F..180F) { rotateValue.isActive() }

        private val swingValue = BoolValue("Swing", true) { mode == "Custom" }
            private val swingDelay by IntegerValue("SwingDelay", 100, 0..1000) { swingValue.isActive() }

        private val jump by BoolValue("Jump", true) { mode == "Custom" }
        private val move by BoolValue("Move", true) { mode == "Custom" }

    private var shouldMove = false
    private var randomTimerDelay = 500L

    private val swingDelayTimer = MSTimer()
    private val delayTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        when (mode.lowercase()) {
            "old" -> {
                mc.gameSettings.keyBindForward.pressed = true

                if (delayTimer.hasTimePassed(500)) {
                    thePlayer.fixedSensitivityYaw += 180F
                    delayTimer.reset()
                }
            }
            "random" -> {
                getRandomMoveKeyBind().pressed = shouldMove

                if (!delayTimer.hasTimePassed(randomTimerDelay)) return
                shouldMove = false
                randomTimerDelay = 500L
                when (nextInt(0, 6)) {
                    0 -> {
                        if (thePlayer.onGround) thePlayer.tryJump()
                        delayTimer.reset()
                    }
                    1 -> {
                        if (!thePlayer.isSwingInProgress) thePlayer.swingItem()
                            delayTimer.reset()
                        }
                        2 -> {
                            randomTimerDelay = nextInt(0, 1000).toLong()
                            shouldMove = true
                            delayTimer.reset()
                        }
                        3 -> {
                            thePlayer.inventory.currentItem = nextInt(0, 9)
                            mc.playerController.updateController()
                            delayTimer.reset()
                        }
                        4 -> {
                            thePlayer.fixedSensitivityYaw += nextFloat(-180f, 180f)
                            delayTimer.reset()
                        }
                        5 -> {
                            thePlayer.fixedSensitivityPitch += nextFloat(-10f, 10f)
                            delayTimer.reset()
                        }
                    }
            }
            "custom" -> {
                if (move)
                    mc.gameSettings.keyBindForward.pressed = true

                if (jump && thePlayer.onGround)
                    thePlayer.tryJump()

                if (rotateValue.get() && delayTimer.hasTimePassed(rotationDelay)) {
                    thePlayer.fixedSensitivityYaw += rotationAngle
                    thePlayer.fixedSensitivityPitch += nextFloat(0F, 1F) * 2 - 1
                    delayTimer.reset()
                }

                if (swingValue.get() && !thePlayer.isSwingInProgress && swingDelayTimer.hasTimePassed(swingDelay)) {
                    thePlayer.swingItem()
                    swingDelayTimer.reset()
                }
            }
        }
    }

    private val moveKeyBindings =
         arrayOf(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindRight)

    private fun getRandomMoveKeyBind() = moveKeyBindings.random()

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
            mc.gameSettings.keyBindForward.pressed = false
    }
}