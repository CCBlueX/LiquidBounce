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
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NullStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S2EPacketCloseWindow
import org.lwjgl.input.Keyboard
import java.awt.Color

@ModuleInfo(
    name = "ClickGUI",
    description = "Opens the ClickGUI.",
    category = ModuleCategory.RENDER,
    keyBind = Keyboard.KEY_RSHIFT,
    canEnable = false
)
object ClickGUI : Module() {
    private val styleValue: ListValue =
        object : ListValue("Style", arrayOf("LiquidBounce", "Null", "Slowly"), "LiquidBounce") {
            override fun onChanged(oldValue: String, newValue: String) = updateStyle()
        }
    val scaleValue = FloatValue("Scale", 0.8f, 0.5f, 1.5f)
    val maxElementsValue = IntegerValue("MaxElements", 15, 1, 30)
    val fadeSpeedValue = FloatValue("FadeSpeed", 1f, 0.5f, 2f)
    val scrollsValue = BoolValue("Scrolls", false)

    private val colorRainbow = object : BoolValue("Rainbow", false) {
        override fun isSupported() = styleValue.get() != "Slowly"
    }
    private val colorRedValue = object : IntegerValue("R", 0, 0, 255) {
        override fun isSupported() = colorRainbow.isSupported() && !colorRainbow.get()
    }
    private val colorGreenValue = object : IntegerValue("G", 160, 0, 255) {
        override fun isSupported() = colorRainbow.isSupported() && !colorRainbow.get()
    }
    private val colorBlueValue = object : IntegerValue("B", 255, 0, 255) {
        override fun isSupported() = colorRainbow.isSupported() && !colorRainbow.get()
    }

    val guiColor: Int
        get() {
            return if (colorRainbow.get()) ColorUtils.rainbow().rgb
            else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()).rgb
        }

    override fun onEnable() {
        updateStyle()
        mc.displayGuiScreen(clickGui)
    }

    private fun updateStyle() {
        when (styleValue.get()) {
            "LiquidBounce" -> clickGui.style = LiquidBounceStyle
            "Null" -> clickGui.style = NullStyle
            "Slowly" -> clickGui.style = SlowlyStyle
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S2EPacketCloseWindow && mc.currentScreen is ClickGui)
            event.cancelEvent()
    }
}