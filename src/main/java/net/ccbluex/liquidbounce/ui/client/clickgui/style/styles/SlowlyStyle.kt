/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles



import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getButtonFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getDescriptionFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getPanelFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getValueFont
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.assumeVolatile
import net.ccbluex.liquidbounce.ui.font.assumeVolatileIf
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_4
import net.ccbluex.liquidbounce.utils.misc.StringUtils.stripControlCodes
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Mouse
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode

private const val WHITE = -1
private const val LIGHT_GRAY = Int.MAX_VALUE

private const val BACKGROUND = -14010033
private const val BACKGROUND_BORDER = -13220000

private val BUTTON_UNPRESSED = Color(54, 71, 96).rgb
private val BUTTON_PRESSED = Color(7, 152, 252).rgb

class SlowlyStyle : Style()
{
    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
    {
        val font = getPanelFont()
        drawBorderedRect(panel.x.toFloat(), panel.y.toFloat() - 3, panel.x.toFloat() + panel.width, panel.y.toFloat() + 17, 3f, BACKGROUND, BACKGROUND)

        if (panel.fade > 0)
        {
            drawBorderedRect(panel.x.toFloat(), panel.y.toFloat() + 17, panel.x.toFloat() + panel.width, panel.y + 19 + panel.fade, 3f, BACKGROUND_BORDER, BACKGROUND_BORDER)
            drawBorderedRect(panel.x.toFloat(), panel.y + panel.fade + 17, panel.x.toFloat() + panel.width, panel.y + 19 + panel.fade + 5, 3f, BACKGROUND, BACKGROUND)
        }

        classProvider.GlStateManager.resetColor()

        val textWidth = font.getStringWidth("\u00A7f" + stripControlCodes(panel.name)).toFloat()
        font.drawString(panel.name, (panel.x - (textWidth - 100.0f) * 0.5).toInt(), panel.y + 7 - 3, WHITE)
    }

    override fun drawDescription(mouseX: Int, mouseY: Int, text: String)
    {
        val font = getDescriptionFont()
        val fontHeight = font.fontHeight
        val textWidth = font.getStringWidth(text)
        drawBorderedRect((mouseX + 9).toFloat(), mouseY.toFloat(), mouseX + textWidth + 14f, mouseY + fontHeight + 3f, 3.0f, BACKGROUND, BACKGROUND)

        classProvider.GlStateManager.resetColor()

        font.drawString(text, mouseX + 12, mouseY + (fontHeight shr 1), WHITE)
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
    {
        drawRect(buttonElement.x - 1, buttonElement.y - 1, buttonElement.x + buttonElement.width + 1, buttonElement.y + buttonElement.height + 1, getHoverColor(if (buttonElement.color == Int.MAX_VALUE) BUTTON_UNPRESSED else BUTTON_PRESSED, buttonElement.hoverTime))

        classProvider.GlStateManager.resetColor()

        getButtonFont().drawString(buttonElement.displayName, buttonElement.x + 5, buttonElement.y + 5, WHITE)
    }

    // fun drawCheckbox(value: Boolean, x: Int, y: Int, mouseX: Int, mouseY: Int, color: Color?): Boolean
    // {
    // 	drawRect(x, y, x + 20, y + 10, if (value) Color.GREEN.rgb else Color.RED.rgb)
    // 	drawFilledCircle(x + if (value) 15 else 5, y + 5, 5f, Color.WHITE.rgb)
    // 	return if (mouseX >= x && mouseX <= x + 20 && mouseY >= y && mouseY <= y + 10 && 0 is ButtonDown) !value else value
    // }

    override fun drawModuleElement(mouseX: Int, mouseY: Int, moduleElement: ModuleElement)
    {
        val buttonFont = getButtonFont()
        val valueFont = getValueFont()

        drawRect(moduleElement.x - 1, moduleElement.y - 1, moduleElement.x + moduleElement.width + 1, moduleElement.y + moduleElement.height + 1, getHoverColor(BUTTON_UNPRESSED, moduleElement.hoverTime))
        drawRect(moduleElement.x - 1, moduleElement.y - 1, moduleElement.x + moduleElement.width + 1, moduleElement.y + moduleElement.height + 1, getHoverColor(ColorUtils.applyAlphaChannel(BUTTON_PRESSED, moduleElement.slowlyFade), moduleElement.hoverTime))

        val glStateManager = classProvider.glStateManager
        GlStateManager.resetColor()

        buttonFont.drawString(moduleElement.displayName, moduleElement.x + 5, moduleElement.y + 5, WHITE)

        // Draw settings
        val moduleValues = moduleElement.module.values
        if (moduleValues.isNotEmpty())
        {
            buttonFont.drawString(">", moduleElement.x + moduleElement.width - 8, moduleElement.y + 5, WHITE)

            if (moduleElement.showSettings)
            {
                if (moduleElement.settingsWidth > 0.0f && moduleElement.slowlySettingsYPos > moduleElement.y + 6) drawBorderedRect((moduleElement.x + moduleElement.width + 4).toFloat(), moduleElement.y + 6f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, moduleElement.slowlySettingsYPos + 2f, 3.0f, BACKGROUND_BORDER, BACKGROUND_BORDER)
                moduleElement.slowlySettingsYPos = moduleElement.y + 6

                for (value in moduleValues) drawAbstractValue(valueFont, glStateManager, moduleElement, value, mouseX, mouseY)

                moduleElement.updatePressed()
            }
        }
    }

    private fun drawAbstractValue(font: FontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: AbstractValue, mouseX: Int, mouseY: Int, indent: Int = 0)
    {
        when (value)
        {
            is ValueGroup ->
            {
                val moduleX = moduleElement.x + moduleElement.width
                val moduleIndentX = moduleX + indent

                val text = value.displayName
                val textWidth = font.getStringWidth(text) + indent + 16f
                val textHeight = font.fontHeight

                if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                val moduleXEnd = moduleX + moduleElement.settingsWidth

                GlStateManager.resetColor()
                font.drawString("\u00A7c$text", moduleIndentX + 6, moduleElement.slowlySettingsYPos + 2, WHITE)
                font.drawString(if (value.foldState) "-" else "+", (moduleXEnd - if (value.foldState) 5 else 6).toInt(), moduleElement.slowlySettingsYPos + 2, WHITE)

                if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + textHeight && 0 is ButtonDown && moduleElement.isntLeftPressed)
                {
                    value.foldState = !value.foldState
                    mc.soundHandler.playSound("gui.button.press", 1.0f)
                }

                moduleElement.slowlySettingsYPos += textHeight + 1

                if (value.foldState)
                {
                    val cachedYPos = moduleElement.slowlySettingsYPos
                    val valuesInGroup = value.values.filter(AbstractValue::showCondition)
                    var i = 0
                    val j = valuesInGroup.size
                    while (i < j)
                    {
                        val valueOfGroup = valuesInGroup[i]
                        val textWidth2 = font.getStringWidth(valueOfGroup.displayName) + 12f

                        if (moduleElement.settingsWidth < textWidth2) moduleElement.settingsWidth = textWidth2

                        GlStateManager.resetColor()
                        drawAbstractValue(font, glStateManager, moduleElement, valueOfGroup, mouseX, mouseY, indent + 10)

                        if (i == j - 1) // Last Index
                        {
                            drawRect(moduleIndentX + 7, cachedYPos, moduleIndentX + 8, moduleElement.slowlySettingsYPos - 1, LIGHT_GRAY)
                            drawRect(moduleIndentX + 7, moduleElement.slowlySettingsYPos - 1, moduleIndentX + 12, moduleElement.slowlySettingsYPos, LIGHT_GRAY)
                        }
                        i++
                    }
                }
                else moduleElement.slowlySettingsYPos++
            }

            is ColorValue -> drawColorValue(font, glStateManager, moduleElement, value, mouseX, mouseY, indent)
            is RangeValue<*> -> drawRangeValue(font, glStateManager, moduleElement, value, mouseX, mouseY, indent)
            else -> drawValue(font, glStateManager, moduleElement, value as Value<*>, mouseX, mouseY, indent)
        }
    }

    private fun drawColorValue(font: FontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: ColorValue, mouseX: Int, mouseY: Int, indent: Int = 0)
    {
        val moduleX = moduleElement.x + moduleElement.width
        val moduleIndentX = moduleX + indent

        val alphaPresent = value is RGBAColorValue
        val text = "${value.displayName}\u00A7f: \u00A7c${value.getRed()} \u00A7a${value.getGreen()} \u00A79${value.getBlue()}${if (alphaPresent) " \u00A77${value.getAlpha()}" else ""}\u00A7r "
        val colorText = "(#${if (alphaPresent) encodeToHex(value.getAlpha()) else ""}${encodeToHex(value.getRed())}${encodeToHex(value.getGreen())}${encodeToHex(value.getBlue())})"
        val displayTextWidth = font.getStringWidth(text)
        val textWidth = displayTextWidth + font.getStringWidth(colorText) + indent + 2f

        if (moduleElement.settingsWidth < textWidth + 20f) moduleElement.settingsWidth = textWidth + 20f
        val moduleXEnd = moduleX + moduleElement.settingsWidth

        drawRect(moduleIndentX + textWidth, moduleElement.slowlySettingsYPos.toFloat(), moduleXEnd - 4f, moduleElement.slowlySettingsYPos + 10f, value.get())

        GlStateManager.resetColor()
        font.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 4, WHITE)
        font.drawString(colorText, moduleIndentX + displayTextWidth + 6, moduleElement.slowlySettingsYPos + 4, value.get(255))

        drawSlider(value.getRed().toFloat(), 0f, 255f, moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -65536 /* 0xFFFF0000 */) {
            value.set(it.toInt(), value.getGreen(), value.getBlue(), value.getAlpha())
        }

        moduleElement.slowlySettingsYPos += 9

        drawSlider(value.getGreen().toFloat(), 0f, 255f, moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -16711936 /* 0xFF00FF00 */) {
            value.set(value.getRed(), it.toInt(), value.getBlue(), value.getAlpha())
        }

        moduleElement.slowlySettingsYPos += 9

        drawSlider(value.getBlue().toFloat(), 0f, 255f, moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -16776961 /* 0xFF0000FF */) {
            value.set(value.getRed(), value.getGreen(), it.toInt(), value.getAlpha())
        }

        if (alphaPresent)
        {
            moduleElement.slowlySettingsYPos += 9

            drawSlider(value.getAlpha().toFloat(), 0f, 255f, moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, LIGHT_GRAY) {
                value.set(value.getRed(), value.getGreen(), value.getBlue(), it.toInt())
            }
        }

        moduleElement.slowlySettingsYPos += 19
    }

    private fun drawRangeValue(font: FontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: RangeValue<*>, mouseX: Int, mouseY: Int, indent: Int = 0)
    {
        val moduleX = moduleElement.x + moduleElement.width
        val moduleIndentX = moduleX + indent

        assumeVolatile {
            when (value)
            {
                is IntegerRangeValue ->
                {
                    val text = "${value.displayName}\u00A7f: \u00A7c${value.getMin()}-${value.getMax()}"
                    val textWidth = font.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    drawRangeSlider(value.getMin().toFloat(), value.getMax().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -16279300 /* 0xFF0798FC */, value::setMin, value::setMax)

                    GlStateManager.resetColor()
                    font.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 4, WHITE)

                    moduleElement.slowlySettingsYPos += 19
                }

                is FloatRangeValue ->
                {
                    val text = "${value.displayName}\u00A7f: \u00A7c${DECIMALFORMAT_4.format(value.getMin())}-${DECIMALFORMAT_4.format(value.getMax())}"
                    val textWidth = font.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    drawRangeSlider(value.getMin(), value.getMax(), value.minimum, value.maximum, moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -16279300 /* 0xFF0798FC */, value::setMin, value::setMax)

                    GlStateManager.resetColor()

                    font.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 4, WHITE)

                    moduleElement.slowlySettingsYPos += 19
                }
            }
        }
    }

    private fun drawValue(valueFont: FontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: Value<*>, mouseX: Int, mouseY: Int, indent: Int = 0)
    {
        val moduleX = moduleElement.x + moduleElement.width
        val moduleIndentX = moduleElement.x + moduleElement.width + indent

        assumeVolatileIf(value.get() is Number) {
            when (value)
            {
                is BoolValue ->
                {
                    val text = value.displayName
                    val textWidth = valueFont.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleX + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + 12 && 0 is ButtonDown && moduleElement.isntLeftPressed)
                    {
                        value.set(!value.get())
                        mc.soundHandler.playSound("gui.button.press", 1.0f)
                    }

                    valueFont.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 2, if (value.get()) WHITE else LIGHT_GRAY)

                    moduleElement.slowlySettingsYPos += 11
                }

                is ListValue ->
                {
                    val text = value.displayName
                    val textWidth = valueFont.getStringWidth(text) + indent + 16f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    valueFont.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 2, WHITE)
                    valueFont.drawString(if (value.openList) "-" else "+", (moduleX + moduleElement.settingsWidth - if (value.openList) 5 else 6).toInt(), moduleElement.slowlySettingsYPos + 2, WHITE)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleX + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + valueFont.fontHeight && 0 is ButtonDown && moduleElement.isntLeftPressed)
                    {
                        value.openList = !value.openList
                        mc.soundHandler.playSound("gui.button.press", 1.0f)
                    }

                    moduleElement.slowlySettingsYPos += valueFont.fontHeight + 1

                    for (valueOfList in value.values)
                    {
                        val textWidth2 = valueFont.getStringWidth("> $valueOfList").toFloat()

                        if (moduleElement.settingsWidth < textWidth2 + 12) moduleElement.settingsWidth = textWidth2 + 12

                        if (value.openList)
                        {
                            if (mouseX >= moduleIndentX + 4 && mouseX <= moduleX + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos + 2 && mouseY <= moduleElement.slowlySettingsYPos + 14 && 0 is ButtonDown && moduleElement.isntLeftPressed)
                            {
                                value.set(valueOfList)
                                mc.soundHandler.playSound("gui.button.press", 1.0f)
                            }
                            GlStateManager.resetColor()
                            valueFont.drawString("> $valueOfList", moduleIndentX + 6, moduleElement.slowlySettingsYPos + 2, if (value.get().equals(valueOfList, ignoreCase = true)) WHITE else LIGHT_GRAY)
                            moduleElement.slowlySettingsYPos += valueFont.fontHeight + 1
                        }
                    }

                    if (!value.openList) moduleElement.slowlySettingsYPos += 1
                }

                is IntegerValue ->
                {
                    val text = value.displayName + "\u00A7f: " + if (value is BlockValue) getBlockName(value.get()) + " (" + value.get() + ")" else value.get()
                    val textWidth = valueFont.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    drawSlider(value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -16279300, value::set)

                    valueFont.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 3, WHITE)

                    moduleElement.slowlySettingsYPos += 19
                }

                is FloatValue ->
                {
                    val text = value.displayName + "\u00A7f: " + DECIMALFORMAT_4.format(value.get())
                    val textWidth = valueFont.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    drawSlider(value.get(), value.minimum, value.maximum, moduleX + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, indent, -16279300, value::set)

                    valueFont.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 3, WHITE)

                    moduleElement.slowlySettingsYPos += 19
                }

                is FontValue ->
                {
                    val fontRenderer = value.get()
                    var displayString = "Font: Unknown"

                    if (fontRenderer.isGameFontRenderer())
                    {
                        val liquidFontRenderer = fontRenderer.getGameFontRenderer()
                        displayString = "Font: " + liquidFontRenderer.defaultFont.font.name + " - " + liquidFontRenderer.defaultFont.font.size
                    }
                    else if (fontRenderer == Fonts.minecraftFont) displayString = "Font: Minecraft"
                    else
                    {
                        val objects = Fonts.getFontDetails(fontRenderer)
                        if (objects != null) displayString = objects.name + if (objects.fontSize == -1) "" else " - " + objects.fontSize
                    }

                    valueFont.drawString(displayString, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 2, WHITE)

                    val stringWidth = valueFont.getStringWidth(displayString) + indent + 8f

                    if (moduleElement.settingsWidth < stringWidth) moduleElement.settingsWidth = stringWidth

                    if ((0 is ButtonDown && moduleElement.isntLeftPressed || 1 is ButtonDown && moduleElement.isntRightPressed) && mouseX >= moduleIndentX + 4 && mouseX <= moduleX + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + 12)
                    {
                        val fonts = Fonts.fonts
                        if (0 is ButtonDown)
                        {
                            var i = 0
                            val j = fonts.size
                            while (i < j)
                            {
                                val font = fonts[i]
                                if (font == fontRenderer)
                                {
                                    i++
                                    if (i >= fonts.size) i = 0
                                    value.set(fonts[i])
                                    break
                                }
                                i++
                            }
                        }
                        else
                        {
                            var i = fonts.size - 1
                            while (i >= 0)
                            {
                                val font = fonts[i]
                                if (font == fontRenderer)
                                {
                                    i--
                                    if (i >= fonts.size) i = 0
                                    if (i < 0) i = fonts.size - 1
                                    value.set(fonts[i])
                                    break
                                }
                                i--
                            }
                        }
                    }

                    moduleElement.slowlySettingsYPos += 11
                }

                else ->
                {
                    val text = value.displayName + "\u00A7f: " + value.get()
                    val textWidth = valueFont.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    GlStateManager.resetColor()

                    valueFont.drawString(text, moduleIndentX + 6, moduleElement.slowlySettingsYPos + 4, WHITE)

                    moduleElement.slowlySettingsYPos += 12
                }
            }
        }
    }

    companion object
    {
        private fun encodeToHex(hex: Int) = hex.toString(16).toUpperCase().padStart(2, '0')

        private inline fun drawSlider(value: Float, min: Float, max: Float, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, indent: Int, color: Int, changeCallback: (Float) -> Unit)
        {
            val indentX = x + indent
            val xEnd = x + width
            drawRect(indentX, y, xEnd, y + 2, Int.MAX_VALUE)

            val sliderValue = indentX + (width - indent) * (value.coerceIn(min, max) - min) / (max - min)

            drawRect(indentX.toFloat(), y.toFloat(), sliderValue, y + 2f, color)
            drawFilledCircle(sliderValue.toInt(), y + 1, 3f, color)

            if (mouseX in indentX..xEnd && mouseY >= y && mouseY <= y + 3 && 0 is ButtonDown)
            {
                val sliderXEnd = width - indent - 3f
                changeCallback(BigDecimal("${(min + (max - min) * ((mouseX - indentX) / sliderXEnd).coerceIn(0f, 1f))}").setScale(4, RoundingMode.HALF_UP).toFloat())
            }
        }

        private inline fun drawRangeSlider(minValue: Float, maxValue: Float, min: Float, max: Float, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, indent: Int, color: Int, onMinChanged: (Float) -> Unit, onMaxChanged: (Float) -> Unit)
        {
            val indentX = x + indent
            val sliderXEnd = width - indent - 3f
            drawRect(indentX, y, x + width, y + 2, Int.MAX_VALUE)

            val minSliderValue = indentX + (width - indent) * (minValue.coerceIn(min, max) - min) / (max - min)
            drawFilledCircle(minSliderValue.toInt(), y + 1, 3f, color)

            val maxSliderValue = indentX + (width - indent) * (maxValue.coerceIn(min, max) - min) / (max - min)
            drawFilledCircle(maxSliderValue.toInt(), y + 1, 3f, color)

            drawRect(minSliderValue, y.toFloat(), maxSliderValue, y + 2f, color)

            val center = minSliderValue + (maxSliderValue - minSliderValue) * 0.5f

            if (mouseX >= indentX && mouseX <= x + width && mouseY >= y && mouseY <= y + 3 && 0 is ButtonDown)
            {
                val newValue = BigDecimal("${(min + (max - min) * ((mouseX - indentX) / sliderXEnd).coerceIn(0f, 1f))}").setScale(4, RoundingMode.HALF_UP).toFloat()
                if (mouseX > center) onMaxChanged(newValue) else onMinChanged(newValue)
            }
        }

        private fun getHoverColor(color: Int, hover: Int): Int = ColorUtils.createRGB(((color shr 16 and 0xFF) - (hover shl 1)).coerceAtLeast(0), ((color shr 8 and 0xFF) - (hover shl 1)).coerceAtLeast(0), ((color and 0xFF) - (hover shl 1)).coerceAtLeast(0), color shr 24 and 0xFF)
    }
}
