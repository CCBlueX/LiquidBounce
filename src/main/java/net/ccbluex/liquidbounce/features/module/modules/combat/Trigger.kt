/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.KeyBinding

object Trigger : Module("Trigger", ModuleCategory.COMBAT) {

    private val maxCPSValue: IntegerValue = object : IntegerValue("MaxCPS", 8, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minCPS)

        override fun onChanged(oldValue: Int, newValue: Int) {
            delay = randomClickDelay(minCPS, get())
        }
    }
    private val maxCPS by maxCPSValue

    private val minCPS: Int by object : IntegerValue("MinCPS", 5, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxCPS)

        override fun onChanged(oldValue: Int, newValue: Int) {
            delay = randomClickDelay(get(), maxCPS)
        }

        override fun isSupported() = !maxCPSValue.isMinimal()
    }

    private var delay = randomClickDelay(minCPS, maxCPS)
    private var lastSwing = 0L

    @EventTarget
    fun onRender(event: Render3DEvent) {
        val objectMouseOver = mc.objectMouseOver

        if (objectMouseOver != null && System.currentTimeMillis() - lastSwing >= delay &&
                isSelected(objectMouseOver.entityHit, true)) {
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click handling

            lastSwing = System.currentTimeMillis()
            delay = randomClickDelay(minCPS, maxCPS)
        }
    }
}