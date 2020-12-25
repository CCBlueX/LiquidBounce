/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue
import kotlin.random.Random

@ModuleInfo(name = "Spammer", description = "Spams the chat with a given message.", category = ModuleCategory.MISC)
class Spammer: Module() {

    private val maxDelayValue: IntegerValue = object: IntegerValue("MaxDelay", 1000, 0, 5000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelayValueObject = minDelayValue.get()

            if(minDelayValueObject > newValue)
                set(minDelayValueObject)
            delay = TimeUtils.randomDelay(minDelayValue.get(), this.get())
        }
    }

    private val minDelayValue: IntegerValue = object: IntegerValue("MinDelay", 500, 0, 5000) {


        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelayValueObject = maxDelayValue.get()

            if(maxDelayValueObject < newValue)
                set(maxDelayValueObject)
            delay = TimeUtils.randomDelay(this.get(), maxDelayValue.get())
        }
    }

    private val messageValue = TextValue("Message", LiquidBounce.CLIENT_NAME + " Client | liquidbounce(.net) | CCBlueX on yt")
    private val customValue = BoolValue("Custom", false)

    private val msTimer = MSTimer()
    private var delay: Long = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if(msTimer.hasTimePassed(delay)) {
            mc.thePlayer!!.sendChatMessage(if(customValue.get()) replace(messageValue.get()) else messageValue.get() + " >" + RandomUtils.randomString(5 + Random.nextInt(5)) + "<")
            msTimer.reset()
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
        }
    }

    private fun replace(`object`: String): String {
        val r = Random

        while(`object`.contains("%f"))
            `object`.substring(0, `object`.indexOf("%f")) + r.nextFloat() + `object`.substring(`object`.indexOf("%f") + "%f".length)

        while(`object`.contains("%i"))
            `object`.substring(0, `object`.indexOf("%i")) + r.nextInt(10000) + `object`.substring(`object`.indexOf("%i") + "%i".length)

        while(`object`.contains("%s"))
            `object`.substring(0, `object`.indexOf("%s")) + RandomUtils.randomString(r.nextInt(8) + 1) + `object`.substring(`object`.indexOf("%s") + "%s".length)

        while(`object`.contains("%ss"))
            `object`.substring(0, `object`.indexOf("%ss")) + RandomUtils.randomString(r.nextInt(4) + 1) + `object`.substring(`object`.indexOf("%ss") + "%ss".length)

        while(`object`.contains("%ls"))
            `object`.substring(0, `object`.indexOf("%ls")) + RandomUtils.randomString(r.nextInt(15) + 1) + `object`.substring(`object`.indexOf("%ls") + "%ls".length)
        return `object`
    }

}
