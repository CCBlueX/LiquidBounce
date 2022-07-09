/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.IntegerRangeValue

// TODO: Implement, Load messages from file
@ModuleInfo(name = "AutoGG", description = "Sends 'gg' when you win", category = ModuleCategory.MISC)
class AutoGG : Module()
{
    /**
     * Options
     */
    private val delayValue: IntegerRangeValue = object : IntegerRangeValue("Delay", 500, 1000, 0, 5000, "MaxDelay" to "MinDelay")
    {
        override fun onMaxValueChanged(oldValue: Int, newValue: Int)
        {
            delay = TimeUtils.randomDelay(getMin(), newValue)
        }

        override fun onMinValueChanged(oldValue: Int, newValue: Int)
        {
            delay = TimeUtils.randomDelay(newValue, getMax())
        }
    }

    /**
     * Variables
     */
    private val msTimer = MSTimer()
    var delay = delayValue.getRandomLong()
}
