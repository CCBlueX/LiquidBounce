/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S2BPacketChangeGameState
object Ambience : Module("Ambience", ModuleCategory.RENDER) {

    private val timeModeValue = ListValue("Mode", arrayOf("None", "Normal", "Custom"), "Custom")
    private val weatherModeValue = ListValue("WeatherMode", arrayOf("None", "Sun", "Rain", "Thunder"), "None")

    private val customWorldTimeValue  = object : IntegerValue("Time", 19000, 0, 24000) {
        override fun isSupported() = timeModeValue.get() != "Custom"
    }

    private val changeWorldTimeSpeedValue = object : IntegerValue("TimeSpeed", 150, 10, 500) {
        override fun isSupported() = timeModeValue.get() != "None"
    }

    private val weatherStrengthValue = object : FloatValue("WeatherStrength", 1f, 0f, 1f) {
        override fun isSupported() = weatherModeValue.get() != "None"
    }

    var i = 0L

    override fun onDisable() {
        i = 0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        when (timeModeValue.get().lowercase()) {
            "normal" -> {
                if (i < 24000) {
                    i += changeWorldTimeSpeedValue.get()
                } else {
                    i = 0
                }
                mc.theWorld.worldTime = i
            }
            "custom" -> {
                mc.theWorld.worldTime = customWorldTimeValue.get().toLong()
            }
        }

        when (weatherModeValue.get().lowercase()) {
            "sun" -> {
                mc.theWorld.setRainStrength(0f)
                mc.theWorld.setThunderStrength(0f)
            }
            "rain" -> {
                mc.theWorld.setRainStrength(weatherStrengthValue.get())
                mc.theWorld.setThunderStrength(0f)
            }
            "thunder" -> {
                mc.theWorld.setRainStrength(weatherStrengthValue.get())
                mc.theWorld.setThunderStrength(weatherStrengthValue.get())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (!timeModeValue.equals("none") && packet is S03PacketTimeUpdate) {
            event.cancelEvent()
        }

        if (!weatherModeValue.equals("none") && packet is S2BPacketChangeGameState) {
            if (packet.gameState in 7..8) { // change weather packet
                event.cancelEvent()
            }
        }
    }
}