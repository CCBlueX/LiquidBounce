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
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S2BPacketChangeGameState

object Ambience : Module() {

    private val timeMode by ListValue("Mode", arrayOf("None", "Normal", "Custom"), "Custom")
    private val weatherMode by ListValue("WeatherMode", arrayOf("None", "Sun", "Rain", "Thunder"), "None")

    private val customWorldTime by IntegerValue("Time", 19000, 0..24000) { timeMode != "Custom" }

    private val changeWorldTimeSpeed by IntegerValue("TimeSpeed", 150, 10..500) { timeMode != "None" }

    private val weatherStrength by FloatValue("WeatherStrength", 1f, 0f..1f) { weatherMode != "None" }

    var i = 0L

    override fun onDisable() {
        i = 0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        when (timeMode.lowercase()) {
            "normal" -> {
                if (i < 24000) {
                    i += changeWorldTimeSpeed
                } else {
                    i = 0
                }
                mc.theWorld.worldTime = i
            }
            "custom" -> {
                mc.theWorld.worldTime = customWorldTime.toLong()
            }
        }

        when (weatherMode.lowercase()) {
            "sun" -> {
                mc.theWorld.setRainStrength(0f)
                mc.theWorld.setThunderStrength(0f)
            }
            "rain" -> {
                mc.theWorld.setRainStrength(weatherStrength)
                mc.theWorld.setThunderStrength(0f)
            }
            "thunder" -> {
                mc.theWorld.setRainStrength(weatherStrength)
                mc.theWorld.setThunderStrength(weatherStrength)
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (timeMode != "None" && packet is S03PacketTimeUpdate)
            event.cancelEvent()

        if (weatherMode != "None" && packet is S2BPacketChangeGameState) {
            if (packet.gameState in 7..8) { // change weather packet
                event.cancelEvent()
            }
        }
    }
}