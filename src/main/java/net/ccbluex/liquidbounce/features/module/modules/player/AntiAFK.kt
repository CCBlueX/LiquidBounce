/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding

@ModuleInfo(name = "AntiAFK", description = "Prevents you from getting kicked for being AFK.", category = ModuleCategory.PLAYER)
class AntiAFK : Module() {

    private val swingDelayTimer = MSTimer()
    private val delayTimer = MSTimer()

    private val modeValue = ListValue("Mode", arrayOf("Old", "Random", "Custom"), "Random")

    private val swingDelayValue = IntegerValue("SwingDelay", 100, 0, 1000)
    private val rotationDelayValue = IntegerValue("RotationDelay", 100, 0, 1000)
    private val rotationAngleValue = FloatValue("RotationAngle", 1f, -180F, 180F)

    private val jumpValue = BoolValue("Jump", true)
    private val moveValue = BoolValue("Move", true)
    private val rotateValue = BoolValue("Rotate", true)
    private val swingValue = BoolValue("Swing", true)

    private var shouldMove = false
    private var randomTimerDelay = 500L

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        when (modeValue.get().lowercase()) {
            "old" -> {
                mc.gameSettings.keyBindForward.pressed = true

                if (delayTimer.hasTimePassed(500)) {
                    thePlayer.rotationYaw += 180F
                    delayTimer.reset()
                }
            }
            "random" -> {
                getRandomMoveKeyBind()!!.pressed = shouldMove

                if (!delayTimer.hasTimePassed(randomTimerDelay)) return
                shouldMove = false
                randomTimerDelay = 500L
                when (RandomUtils.nextInt(0, 6)) {
                    0 -> {
                        if (thePlayer.onGround) thePlayer.jump()
                        delayTimer.reset()
                    }
                    1 -> {
                        if (!thePlayer.isSwingInProgress) thePlayer.swingItem()
                            delayTimer.reset()
                        }
                        2 -> {
                            randomTimerDelay = RandomUtils.nextInt(0, 1000).toLong()
                            shouldMove = true
                            delayTimer.reset()
                        }
                        3 -> {
                            thePlayer.inventory.currentItem = RandomUtils.nextInt(0, 9)
                            mc.playerController.updateController()
                            delayTimer.reset()
                        }
                        4 -> {
                            thePlayer.rotationYaw += RandomUtils.nextFloat(-180.0F, 180.0F)
                            delayTimer.reset()
                        }
                        5 -> {
                            if (thePlayer.rotationPitch <= -90 || thePlayer.rotationPitch >= 90) thePlayer.rotationPitch = 0F
                            thePlayer.rotationPitch += RandomUtils.nextFloat(-10.0F, 10.0F)
                            delayTimer.reset()
                        }
                    }
            }
            "custom" -> {
                if (moveValue.get())
                    mc.gameSettings.keyBindForward.pressed = true

                if (jumpValue.get() && thePlayer.onGround)
                    thePlayer.jump()

                if (rotateValue.get() && delayTimer.hasTimePassed(rotationDelayValue.get().toLong())) {
                    thePlayer.rotationYaw += rotationAngleValue.get()
                    if (thePlayer.rotationPitch <= -90 || thePlayer.rotationPitch >= 90) thePlayer.rotationPitch = 0F
                    thePlayer.rotationPitch += RandomUtils.nextFloat(0F, 1F) * 2 - 1
                    delayTimer.reset()
                }

                if (swingValue.get() && !thePlayer.isSwingInProgress && swingDelayTimer.hasTimePassed(swingDelayValue.get().toLong())) {
                    thePlayer.swingItem()
                    swingDelayTimer.reset()
                }
            }
        }
    }

    private fun getRandomMoveKeyBind(): KeyBinding? {
        when (RandomUtils.nextInt(0, 4)) {
            0 -> {
                return mc.gameSettings.keyBindRight
            }
            1 -> {
                return mc.gameSettings.keyBindLeft
            }
            2 -> {
                return mc.gameSettings.keyBindBack
            }
            3 -> {
                return mc.gameSettings.keyBindForward
            }
            else -> {
                return null
            }
        }
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
            mc.gameSettings.keyBindForward.pressed = false
    }
}