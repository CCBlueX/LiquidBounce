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
import net.minecraft.client.settings.GameSettings

@ModuleInfo(name = "AntiAFK", description = "Prevents you from getting kicked for being AFK.", category = ModuleCategory.PLAYER)
class AntiAFK : Module() {

    private val timer = MSTimer()

    private val moveValue = BoolValue("Move", true)
    private val rotateValue = BoolValue("Rotate", true)
    private val jumpValue = BoolValue("Jump", true)
    private val swingValue = BoolValue("Swing", true)

    private var flipPitch = false
    private var lastPitch = 0f

    private var moved = false

    private var nextSwingDelay = RandomUtils.nextInt(100, 200)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        //Made antiafk bypassing more anticheats and added more features
        if (moveValue.get()) {
            mc.gameSettings.keyBindForward.pressed = true
            moved = true
        } else if (moved) {
            if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
                mc.gameSettings.keyBindForward.pressed = false

            moved = false
        }

        if (rotateValue.get()) {
            mc.thePlayer.rotationYaw += RandomUtils.nextFloat(2f, 5f)
            //Added Pitch Movement the prevent BotLike-Moving Flags
            lastPitch = if (flipPitch) -lastPitch else RandomUtils.nextFloat(0.1f, 0.6f)
            flipPitch = !flipPitch
            mc.thePlayer.rotationPitch += lastPitch
        } else {
            flipPitch = false
        }

        if (jumpValue.get() && mc.thePlayer.onGround)
            mc.thePlayer.jump()

        if (swingValue.get() && !mc.thePlayer.isSwingInProgress && timer.hasTimePassed(nextSwingDelay.toLong())) {
            mc.thePlayer.swingItem()
            timer.reset()
            nextSwingDelay = RandomUtils.nextInt(100, 200)
        }
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
            mc.gameSettings.keyBindForward.pressed = false
    }
}