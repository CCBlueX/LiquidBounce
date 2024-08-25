/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce.clickGui
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NullStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import org.lwjgl.input.Keyboard
import java.awt.Color

object ClickGUI : Module("ClickGUI", Category.RENDER, Keyboard.KEY_RSHIFT, canBeEnabled = false) {
    private val style by
        object : ListValue("Style", arrayOf("LiquidBounce", "Null", "Slowly", "Black"), "LiquidBounce") {
            override fun onChanged(oldValue: String, newValue: String) = updateStyle()
        }
    var scale by FloatValue("Scale", 0.8f, 0.5f..1.5f)
    val maxElements by IntegerValue("MaxElements", 15, 1..30)
    val fadeSpeed by FloatValue("FadeSpeed", 1f, 0.5f..4f)
    val scrolls by BoolValue("Scrolls", false)
    val spacedModules by BoolValue("SpacedModules", false)
    val panelsForcedInBoundaries by BoolValue("PanelsForcedInBoundaries", true)

    private val colorRainbowValue = BoolValue("Rainbow", false) { style !in arrayOf("Slowly", "Black") }
        private val colorRed by IntegerValue("R", 0, 0..255) { colorRainbowValue.isSupported() && !colorRainbowValue.get() }
        private val colorGreen by IntegerValue("G", 160, 0..255) { colorRainbowValue.isSupported() && !colorRainbowValue.get() }
        private val colorBlue by IntegerValue("B", 255, 0..255) { colorRainbowValue.isSupported() && !colorRainbowValue.get() }

    val guiColor
        get() = if (colorRainbowValue.get()) ColorUtils.rainbow().rgb
        else Color(colorRed, colorGreen, colorBlue).rgb

    override fun onEnable() {
        updateStyle()
        mc.setScreen(clickGui)
    }

    private fun updateStyle() {
        clickGui.style = when (style) {
            "LiquidBounce" -> LiquidBounceStyle
            "Null" -> NullStyle
            "Slowly" -> SlowlyStyle
            "Black" -> BlackStyle
            else -> return
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is CloseScreenS2CPacket && mc.currentScreen is ClickGui)
            event.cancelEvent()
    }
}
