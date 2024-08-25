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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.randomString
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue

object Spammer : Module("Spammer", Category.MISC, subjective = true, hideModule = false) {
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 1000, 0..5000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)

        override fun onChanged(oldValue: Int, newValue: Int) {
            delay = randomDelay(minDelay, get())
        }
    }
    private val maxDelay by maxDelayValue

    private val minDelay: Int by object : IntegerValue("MinDelay", 500, 0..5000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)

        override fun onChanged(oldValue: Int, newValue: Int) {
            delay = randomDelay(get(), maxDelay)
        }

        override fun isSupported() = !maxDelayValue.isMinimal()
    }

    private val message by
        TextValue("Message", "$CLIENT_NAME Client | liquidbounce(.net) | CCBlueX on yt")

    private val custom by BoolValue("Custom", false)

    private val msTimer = MSTimer()
    private var delay = randomDelay(minDelay, maxDelay)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (msTimer.hasTimePassed(delay)) {
            mc.player.sendChatMessage(
                if (custom) replace(message)
                else message + " >" + randomString(nextInt(5, 11)) + "<"
            )
            msTimer.reset()
            delay = randomDelay(minDelay, maxDelay)
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
        mc.networkHandler.playerList
            .map { playerInfo -> playerInfo.gameProfile.name }
            .filter { name -> name != mc.player.name }
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