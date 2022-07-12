/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NullStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.FontRenderer
import net.minecraft.network.play.server.S2EPacketCloseWindow
import org.lwjgl.input.Keyboard
import java.util.*

@ModuleInfo(name = "ClickGUI", description = "Opens the ClickGUI.", category = ModuleCategory.RENDER, defaultKeyBinds = [Keyboard.KEY_RSHIFT], canEnable = false)
class ClickGUI : Module()
{
    /**
     * Options
     */
    private val styleValue: ListValue = object : ListValue("Style", arrayOf("LiquidBounce", "Null", "Slowly"), "Slowly")
    {
        override fun onChanged(oldValue: String, newValue: String)
        {
            updateStyle()
        }
    }
    val scaleValue = FloatValue("Scale", 1.0f, 0.7f, 2.0f)
    val maxElementsValue = IntegerValue("MaxElements", 15, 1, 20)

    override fun onEnable()
    {
        updateStyle()
        mc.displayGuiScreen(LiquidBounce.clickGui)
    }

    fun updateStyle()
    {
        LiquidBounce.clickGui.style = when (styleValue.get().lowercase(Locale.getDefault()))
        {
            "liquidbounce" -> LiquidBounceStyle()
            "slowly" -> SlowlyStyle()
            else -> NullStyle() // null style
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is S2EPacketCloseWindow && mc.currentScreen is ClickGui) event.cancelEvent()
    }

    companion object
    {
        private val panelGroup = ValueGroup("Panel")
        private val panelColorValue = RGBColorValue("Color", 0, 160, 255, Triple("R", "G", "B"))

        private val panelColorRainbowGroup = ValueGroup("Rainbow")
        private val panelColorRainbowEnabledValue = BoolValue("Enabled", false, "Rainbow")
        private val panelColorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
        private val panelColorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
        private val panelColorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

        private val panelFontValue = FontValue("Font", Fonts.font35)

        private val descriptionGroup = ValueGroup("Description")
        private val descriptionColorValue = RGBColorValue("Color", 0, 160, 255)

        private val descriptionColorRainbowGroup = ValueGroup("Rainbow")
        private val descriptionColorRainbowEnabledValue = BoolValue("Enabled", false)
        private val descriptionColorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10)
        private val descriptionColorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f)
        private val descriptionColorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f)

        private val descriptionFontValue = FontValue("Font", Fonts.font35)

        private val buttonGroup = ValueGroup("Button")
        private val buttonColorValue = RGBColorValue("Color", 0, 160, 255)

        private val buttonColorRainbowGroup = ValueGroup("Rainbow")
        private val buttonColorRainbowEnabledValue = BoolValue("Enabled", false)
        private val buttonColorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10)
        private val buttonColorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f)
        private val buttonColorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f)

        private val buttonFontValue = FontValue("Font", Fonts.font35)

        private val valueGroup = ValueGroup("Value")
        private val valueColorValue = RGBColorValue("Color", 0, 160, 255)

        private val valueColorRainbowGroup = ValueGroup("Rainbow")
        private val valueColorRainbowEnabledValue = BoolValue("Enabled", false)
        private val valueColorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10)
        private val valueColorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f)
        private val valueColorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f)

        private val valueFontValue = FontValue("Font", Fonts.font35)

        init
        {
            panelColorRainbowGroup.addAll(panelColorRainbowEnabledValue, panelColorRainbowSpeedValue, panelColorRainbowSaturationValue, panelColorRainbowBrightnessValue)
            panelGroup.addAll(panelColorValue, panelColorRainbowGroup, panelFontValue)

            descriptionColorRainbowGroup.addAll(descriptionColorRainbowEnabledValue, descriptionColorRainbowSpeedValue, descriptionColorRainbowSaturationValue, descriptionColorRainbowBrightnessValue)
            descriptionGroup.addAll(descriptionColorValue, descriptionColorRainbowGroup, descriptionFontValue)

            buttonColorRainbowGroup.addAll(buttonColorRainbowEnabledValue, buttonColorRainbowSpeedValue, buttonColorRainbowSaturationValue, buttonColorRainbowBrightnessValue)
            buttonGroup.addAll(buttonColorValue, buttonColorRainbowGroup, buttonFontValue)

            valueColorRainbowGroup.addAll(valueColorRainbowEnabledValue, valueColorRainbowSpeedValue, valueColorRainbowSaturationValue, valueColorRainbowBrightnessValue)
            valueGroup.addAll(valueColorValue, valueColorRainbowGroup, valueFontValue)
        }

        @JvmStatic
        fun generatePanelColor(): Int = if (panelColorRainbowEnabledValue.get()) rainbowRGB(speed = panelColorRainbowSpeedValue.get(), saturation = panelColorRainbowSaturationValue.get(), brightness = panelColorRainbowBrightnessValue.get()) else panelColorValue.get()

        @JvmStatic
        fun generateDescriptionColor(): Int = if (descriptionColorRainbowEnabledValue.get()) rainbowRGB(speed = descriptionColorRainbowSpeedValue.get(), saturation = descriptionColorRainbowSaturationValue.get(), brightness = descriptionColorRainbowBrightnessValue.get()) else descriptionColorValue.get()

        @JvmStatic
        fun generateButtonColor(): Int = if (buttonColorRainbowEnabledValue.get()) rainbowRGB(speed = buttonColorRainbowSpeedValue.get(), saturation = buttonColorRainbowSaturationValue.get(), brightness = buttonColorRainbowBrightnessValue.get()) else buttonColorValue.get()

        @JvmStatic
        fun generateValueColor(): Int = if (valueColorRainbowEnabledValue.get()) rainbowRGB(speed = valueColorRainbowSpeedValue.get(), saturation = valueColorRainbowSaturationValue.get(), brightness = valueColorRainbowBrightnessValue.get()) else valueColorValue.get()

        @JvmStatic
        fun getPanelFont(): FontRenderer = panelFontValue.get()

        @JvmStatic
        fun getDescriptionFont(): FontRenderer = descriptionFontValue.get()

        @JvmStatic
        fun getButtonFont(): FontRenderer = buttonFontValue.get()

        @JvmStatic
        fun getValueFont(): FontRenderer = valueFontValue.get()
    }
}
