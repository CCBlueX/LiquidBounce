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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.minecraft.client.settings.KeyBinding

@ModuleInfo(name = "Trigger", description = "Automatically attacks the entity you are looking at.", category = ModuleCategory.COMBAT)
class Trigger : Module()
{
    private val cpsValue: IntegerRangeValue = object : IntegerRangeValue("CPS", 7, 8, 1, 20, "MaxCPS" to "MinCPS")
    {
        override fun onMaxValueChanged(oldValue: Int, newValue: Int)
        {
            delay = TimeUtils.randomClickDelay(getMin(), newValue)
        }

        override fun onMinValueChanged(oldValue: Int, newValue: Int)
        {
            delay = TimeUtils.randomClickDelay(newValue, getMax())
        }
    }

    private var delay = cpsValue.getRandomClickDelay()
    private var lastSwing = 0L

    @EventTarget
    fun onRender(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val objectMouseOver = mc.objectMouseOver
        val gameSettings = mc.gameSettings

        if (objectMouseOver != null && System.currentTimeMillis() - lastSwing >= delay && objectMouseOver.entityHit.isSelected(true))
        {
            KeyBinding.onTick(gameSettings.keyBindAttack.keyCode) // Minecraft Click handling

            lastSwing = System.currentTimeMillis()
            delay = cpsValue.getRandomClickDelay()
        }
    }
}
