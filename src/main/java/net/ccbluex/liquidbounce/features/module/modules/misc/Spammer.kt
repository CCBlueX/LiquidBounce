/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.randomString
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue

@ModuleInfo(name = "Spammer", description = "Spams the chat with a given message.", category = ModuleCategory.MISC)
class Spammer : Module() {
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 1000, 0, 5000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelay = minDelayValue.get()
            if (minDelay > newValue) {
                set(minDelay)
                delay = TimeUtils.randomDelay(minDelayValue.get(), this.get())
            }
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 500, 0, 5000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) {
                set(maxDelay)
                delay = TimeUtils.randomDelay(this.get(), maxDelayValue.get())
            }
        }

        override fun isSupported() = !maxDelayValue.isMinimal()
    }

    private val messageValue =
        TextValue("Message", "$CLIENT_NAME Client | liquidbounce(.net) | CCBlueX on yt")

    private val customValue = BoolValue("Custom", false)

    private val msTimer = MSTimer()
    private var delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (msTimer.hasTimePassed(delay)) {
            mc.thePlayer.sendChatMessage(
                if (customValue.get()) replace(messageValue.get())
                else messageValue.get() + " >" + randomString(nextInt(5, 11)) + "<"
            )
            msTimer.reset()
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
        }
    }

    private fun replace(text: String): String {
        var replacedStr = text

        replaceMap.forEach { (key, valueFunc) ->
            while (key in replacedStr) {
                // You have to replace them one by one, otherwise all parameters like %s would be set to the same random string.
                replacedStr = replacedStr.replaceFirst(key, valueFunc())
            }
        }

        return replacedStr
    }

    private fun randomPlayer() =
        mc.netHandler.playerInfoMap
            .map { playerInfo -> playerInfo.gameProfile.name }
            .filter { name -> name != mc.thePlayer.name }
            .randomOrNull() ?: "none"

    private val replaceMap = mapOf(
        "%f" to { nextFloat().toString() },
        "%i" to { nextInt(0, 10000).toString() },
        "%ss" to { randomString(nextInt(1, 6)) },
        "%s" to { randomString(nextInt(1, 10)) },
        "%ls" to { randomString(nextInt(1, 17)) },
        "%p" to { randomPlayer() }
    )
}