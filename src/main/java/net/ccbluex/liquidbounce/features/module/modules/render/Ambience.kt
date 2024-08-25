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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.packet.s2c.play.S03PacketTimeUpdate
import net.minecraft.network.packet.s2c.play.S2BPacketChangeGameState

object Ambience : Module("Ambience", Category.RENDER, gameDetecting = false, hideModule = false) {

    private val timeMode by ListValue("Mode", arrayOf("None", "Normal", "Custom"), "Custom")
        private val customWorldTime by IntegerValue("Time", 19000, 0..24000) { timeMode == "Custom" }
        private val changeWorldTimeSpeed by IntegerValue("TimeSpeed", 150, 10..500) { timeMode == "Normal" }

    private val weatherMode by ListValue("WeatherMode", arrayOf("None", "Sun", "Rain", "Thunder"), "None")
        private val weatherStrength by FloatValue("WeatherStrength", 1f, 0f..1f)
            { weatherMode == "Rain" || weatherMode == "Thunder" }

    private var i = 0L

    override fun onDisable() {
        i = 0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        when (timeMode.lowercase()) {
            "normal" -> {
                i += changeWorldTimeSpeed
                i %= 24000
                mc.world.worldTime = i
            }
            "custom" -> {
                mc.world.worldTime = customWorldTime.toLong()
            }
        }

		val strength = weatherStrength.coerceIn(0F, 1F)

        when (weatherMode.lowercase()) {
            "sun" -> {
                mc.world.setRainStrength(0f)
                mc.world.setThunderStrength(0f)
            }
            "rain" -> {
                mc.world.setRainStrength(strength)
                mc.world.setThunderStrength(0f)
            }
            "thunder" -> {
                mc.world.setRainStrength(strength)
                mc.world.setThunderStrength(strength)
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