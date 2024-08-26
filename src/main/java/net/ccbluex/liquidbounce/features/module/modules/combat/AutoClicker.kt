/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityPitch
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityYaw
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isBlock
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.item.EnumAction
import net.minecraft.util.UseAction
import kotlin.random.Random.Default.nextBoolean

object AutoClicker : Module("AutoClicker", Category.COMBAT, hideModule = false) {

    private val simulateDoubleClicking by BoolValue("SimulateDoubleClicking", false)

    private val maxCPSValue: IntegerValue = object : IntegerValue("MaxCPS", 8, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minCPS)
    }
    private val maxCPS by maxCPSValue

    private val minCPS by object : IntegerValue("MinCPS", 5, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxCPS)

        override fun isSupported() = !maxCPSValue.isMinimal()
    }

    private val right by BoolValue("Right", true)
    private val left by BoolValue("Left", true)
    private val jitter by BoolValue("Jitter", false)
    private val block by BoolValue("AutoBlock", false) { left }
    private val blockDelay by IntegerValue("BlockDelay", 50, 0..100) { block }

    private val requiresNoInput by BoolValue("RequiresNoInput", false) { left }
    private val maxAngleDifference by FloatValue("MaxAngleDifference", 30f, 10f..180f) { left && requiresNoInput }
    private val range by FloatValue("Range", 3f, 0.1f..5f) { left && requiresNoInput }

    private var rightDelay = randomClickDelay(minCPS, maxCPS)
    private var rightLastSwing = 0L
    private var leftDelay = randomClickDelay(minCPS, maxCPS)
    private var leftLastSwing = 0L

    private var lastBlocking = 0L

    private val shouldAutoClick
        get() = mc.player.abilities.creativeMode || !mc.objectMouseOver.type.isBlock

    private var shouldJitter = false

    override fun onDisable() {
        rightLastSwing = 0L
        leftLastSwing = 0L
        lastBlocking = 0L
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        mc.player?.let { player ->
            val time = System.currentTimeMillis()
            val doubleClick = if (simulateDoubleClicking) RandomUtils.nextInt(-1, 1) else 0

            if (block && player.handSwingProgress > 0 && !mc.options.useKey.isPressed) {
                mc.options.useKey.timesPressed = 0
            }

            if (right && mc.options.useKey.isPressed && time - rightLastSwing >= rightDelay) {
                handleRightClick(time, doubleClick)
            }

            if (requiresNoInput) {
                val nearbyEntity = getNearestEntityInRange() ?: return
                if (!isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) return

                if (left && shouldAutoClick && time - leftLastSwing >= leftDelay) {
                    handleLeftClick(time, doubleClick)
                } else if (block && !mc.options.useKey.isPressed && shouldAutoClick && shouldAutoRightClick() && mc.options.attackKey.timesPressed != 0) {
                    handleBlock(time)
                }
            } else {
                if (left && mc.options.attackKey.isPressed && !mc.options.useKey.isPressed && shouldAutoClick && time - leftLastSwing >= leftDelay) {
                    handleLeftClick(time, doubleClick)
                } else if (block && mc.options.attackKey.isPressed && !mc.options.useKey.isPressed && shouldAutoClick && shouldAutoRightClick() && mc.options.attackKey.timesPressed != 0) {
                    handleBlock(time)
                }
            }
        }
    }

    @EventTarget
    fun onTick(event: UpdateEvent) {
        mc.player?.let { player ->
            shouldJitter = !mc.objectMouseOver.type.isBlock && (player.handSwinging || mc.options.attackKey.timesPressed != 0)

            if (jitter && ((left && shouldAutoClick && shouldJitter) || (right && !mc.player.isUsingItem && mc.options.useKey.isPressed))) {
                if (nextBoolean()) player.fixedSensitivityYaw += nextFloat(-1F, 1F)
                if (nextBoolean()) player.fixedSensitivityPitch += nextFloat(-1F, 1F)
            }
        }
    }

    private fun getNearestEntityInRange(): Entity? {
        val player = mc.player ?: return null

        return mc.world?.entities?.filter { isSelected(it, true) }
            ?.filter { player.getDistanceToEntityBox(it) <= range }
            ?.minByOrNull { player.getDistanceToEntityBox(it) }
    }

    private fun shouldAutoRightClick() = mc.player.mainHandStack?.useAction in arrayOf(UseAction.BLOCK)

    private fun handleLeftClick(time: Long, doubleClick: Int) {
        repeat(1 + doubleClick) {
            KeyBinding.onKeyPressed(mc.options.attackKey.code)

            leftLastSwing = time
            leftDelay = randomClickDelay(minCPS, maxCPS)
        }
    }

    private fun handleRightClick(time: Long, doubleClick: Int) {
        repeat(1 + doubleClick) {
            KeyBinding.onKeyPressed(mc.options.useKey.code)

            rightLastSwing = time
            rightDelay = randomClickDelay(minCPS, maxCPS)
        }
    }

    private fun handleBlock(time: Long) {
        if (time - lastBlocking >= blockDelay) {
            KeyBinding.onKeyPressed(mc.options.useKey.code)

            lastBlocking = time
        }
    }
}
