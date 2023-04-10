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
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityPitch
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityYaw
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import kotlin.random.Random.Default.nextBoolean

@ModuleInfo(name = "AutoClicker", description = "Constantly clicks when holding down a mouse button.", category = ModuleCategory.COMBAT)
object AutoClicker : Module() {
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

        override fun isSupported() = !maxCPSValue.isMinimal()
    }

    private val rightValue = BoolValue("Right", true)
    private val leftValue = BoolValue("Left", true)
    private val jitterValue = BoolValue("Jitter", false)
    private val blockValue = BoolValue("AutoBlock", false)

    private var rightDelay = randomClickDelay(minCPSValue.get(), maxCPSValue.get())
    private var rightLastSwing = 0L
    private var leftDelay = randomClickDelay(minCPSValue.get(), maxCPSValue.get())
    private var leftLastSwing = 0L

    private var blockBrokenDelay = 1000L / 20 * (6 + 2) // 6 ticks and 2 more, so autoclicker
    // won't click between breaking blocks for sure
    private var blockLastBroken = 0L
    private var isBreakingBlock = false
    private var wasBreakingBlock = false

    private val timer = TickTimer()

    private fun leftCanAutoClick(currentTime: Long): Boolean {
        return !isBreakingBlock
                && !(currentTime - blockLastBroken < blockBrokenDelay &&
                mc.objectMouseOver != null && mc.objectMouseOver.blockPos != null && mc.theWorld != null &&
                getBlock(mc.objectMouseOver.blockPos) != Blocks.air)
    }

    private fun rightCanAutoClick() = !mc.thePlayer.isUsingItem

    // BUG: There is no delay between breaking blocks in creative mode
    private fun leftClick(currentTime: Long) {
        if (leftValue.get() && mc.gameSettings.keyBindAttack.isKeyDown) {
            isBreakingBlock = mc.playerController.curBlockDamageMP != 0F
            if (!isBreakingBlock && wasBreakingBlock) {
                blockLastBroken = currentTime
            }
            wasBreakingBlock = isBreakingBlock
            if (currentTime - leftLastSwing < leftDelay || !leftCanAutoClick(currentTime)) {
                return
            }
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

            leftLastSwing = currentTime
            blockLastBroken = 0L
            leftDelay = randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }
    }

    private fun rightClick(currentTime: Long) {
        if (rightValue.get() && mc.gameSettings.keyBindUseItem.isKeyDown && currentTime - rightLastSwing >= rightDelay && rightCanAutoClick()) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode) // Minecraft Click Handling

            rightLastSwing = currentTime
            rightDelay = randomClickDelay(minCPSValue.get(), maxCPSValue.get())
        }
    }

    @EventTarget
    fun onRender(event: Render3DEvent) {
        val currentTime = System.currentTimeMillis()
        leftClick(currentTime)
        rightClick(currentTime)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (jitterValue.get() && ((leftValue.get() && mc.gameSettings.keyBindAttack.isKeyDown && leftCanAutoClick(System.currentTimeMillis()))
                || (rightValue.get() && mc.gameSettings.keyBindUseItem.isKeyDown && rightCanAutoClick()))) {
            val thePlayer = mc.thePlayer ?: return

            if (nextBoolean()) thePlayer.fixedSensitivityYaw += RandomUtils.nextFloat(-1F, 1F)

            if (nextBoolean()) thePlayer.fixedSensitivityPitch += RandomUtils.nextFloat(-1F, 1F)
        }

        if (blockValue.get() && timer.hasTimePassed(1) && mc.gameSettings.keyBindAttack.isKeyDown && leftValue.get()) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
        }
    }

}
