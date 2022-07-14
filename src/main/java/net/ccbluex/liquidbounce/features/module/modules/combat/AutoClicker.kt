/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import kotlin.random.Random

@ModuleInfo(name = "AutoClicker", description = "Constantly clicks when holding down a mouse button.", category = ModuleCategory.COMBAT)
class AutoClicker : Module() {
    private val maxCPSValue: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20) {

        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minCPSValue.get()
            if (minCPS > newValue)
                set(minCPS)
        }

    }

    private val minCPSValue: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20) {

        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = maxCPSValue.get()
            if (maxCPS < newValue)
                set(maxCPS)
        }

    }

    private val rightValue = BoolValue("Right", true)
    private val leftValue = BoolValue("Left", true)
    private val jitterValue = BoolValue("Jitter", false)

    private var rightDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
    private var rightLastSwing = 0L
    private var leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
    private var leftLastSwing = 0L

    private var blockBrokenDelay = 1000L / 20 * (6 + 2) // 6 ticks and 2 ticks more, just in case
    private var blockLastBroken = 0L
    private var wasBreakingBlock = false

    // BUG: There is no delay between breaking blocks in creative mode
    // BUG: If jitter is enabled, cursor jitters between breaking blocks
    fun leftClick(currentTime: Long) {
        if (mc.gameSettings.keyBindAttack.isKeyDown && leftValue.get()) {
            var isBreakingBlock = mc.playerController.curBlockDamageMP != 0F
            if (!isBreakingBlock && wasBreakingBlock) {
                blockLastBroken = currentTime
            }
            wasBreakingBlock = isBreakingBlock
            if (isBreakingBlock || currentTime - leftLastSwing < leftDelay
                || (currentTime - blockLastBroken < blockBrokenDelay &&
                mc.objectMouseOver != null && mc.objectMouseOver!!.blockPos != null && mc.theWorld != null &&
                mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block != Blocks.air)) {
                return
            }
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

            leftLastSwing = currentTime
            blockLastBroken = 0L
            leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }
    }

    fun rightClick(currentTime: Long) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown && !mc.thePlayer!!.isUsingItem && rightValue.get() &&
                currentTime - rightLastSwing >= rightDelay) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode) // Minecraft Click Handling

            rightLastSwing = currentTime
            rightDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }
    }

    @EventTarget
    fun onRender(event: Render3DEvent) {
        var currentTime = System.currentTimeMillis()
        leftClick(currentTime)
        rightClick(currentTime)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (jitterValue.get() && (leftValue.get() && mc.gameSettings.keyBindAttack.isKeyDown && mc.playerController.curBlockDamageMP == 0F
                        || rightValue.get() && mc.gameSettings.keyBindUseItem.isKeyDown && !thePlayer.isUsingItem)) {
            if (Random.nextBoolean()) thePlayer.rotationYaw += if (Random.nextBoolean()) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)

            if (Random.nextBoolean()) {
                thePlayer.rotationPitch += if (Random.nextBoolean()) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)

                // Make sure pitch is not going into unlegit values
                if (thePlayer.rotationPitch > 90)
                    thePlayer.rotationPitch = 90F
                else if (thePlayer.rotationPitch < -90)
                    thePlayer.rotationPitch = -90F
            }
        }
    }
}
