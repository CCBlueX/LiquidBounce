/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generateButtonColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generateValueColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getButtonFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getDescriptionFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getPanelFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getValueFont
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.assumeVolatileIf
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_4
import net.ccbluex.liquidbounce.utils.misc.StringUtils.stripControlCodes
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import java.math.BigDecimal
import java.math.RoundingMode

private const val WHITE = 0xFFFFFF
private const val BLACK = -16777216
private const val BACKGROUND = Int.MIN_VALUE
private const val BORDER = 1526726655
private const val LIGHT_GRAY = Int.MAX_VALUE

// TODO: Optimze as NullStyle, SlowlyStyle
class LiquidBounceStyle : Style()
{
    private var mouseDown = false
    private var rightMouseDown = false

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
    {
        val font = getPanelFont()
        val xF = panel.x.toFloat()
        val yF = panel.y.toFloat()
        drawBorderedRect(xF - if (panel.scrollbar) 4 else 0, yF, xF + panel.width, yF + 19 + panel.fade, 1.0f, BORDER, BACKGROUND)

        val textWidth = font.getStringWidth("\u00A7f" + stripControlCodes(panel.name))
        font.drawString("\u00A7f" + panel.name, (panel.x - (textWidth - 100f) * 0.5f).toInt(), panel.y + 7, BLACK)

        if (panel.scrollbar && panel.fade > 0)
        {
            val maxElements = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).maxElementsValue.get()

            drawRect(panel.x - 2f, panel.y + 21f, panel.x.toFloat(), panel.y + 16f + panel.fade, LIGHT_GRAY)
            drawRect(panel.x - 2f, panel.y + 30f + (panel.fade - 24.0f) / (panel.elements.size - maxElements) * panel.dragged - 10.0f, xF, panel.y + 40 + (panel.fade - 24.0f) / (panel.elements.size - maxElements) * panel.dragged, BACKGROUND)
        }
    }

    override fun drawDescription(mouseX: Int, mouseY: Int, text: String)
    {
        val font = getDescriptionFont()
        val fontHeight = font.FONT_HEIGHT
        val textWidth = font.getStringWidth(text)
        drawBorderedRect(mouseX + 9f, mouseY.toFloat(), mouseX + textWidth + 14f, mouseY + fontHeight + 3f, 1f, BORDER, BACKGROUND)

        GlStateManager.resetColor()

        font.drawString(text, mouseX + 12, mouseY + (fontHeight shr 1), LIGHT_GRAY)
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
    {
        val font = getButtonFont()
        GlStateManager.resetColor()
        font.drawString(buttonElement.displayName, (buttonElement.x - (font.getStringWidth(buttonElement.displayName) - 100.0f) * 0.5f).toInt(), buttonElement.y + 6, buttonElement.color)
    }

    override fun drawModuleElement(mouseX: Int, mouseY: Int, moduleElement: ModuleElement)
    {
        val buttonFont = getButtonFont()

        val valueFont = getValueFont()
        val valueColor = generateValueColor()
        GlStateManager.resetColor()

        val elementX = moduleElement.x + moduleElement.width
        val elementY = moduleElement.y

        buttonFont.drawString(moduleElement.displayName, (moduleElement.x - (buttonFont.getStringWidth(moduleElement.displayName) - 100.0f) * 0.5f).toInt(), elementY + 6, if (moduleElement.module.state) generateButtonColor() else LIGHT_GRAY)

        val moduleValues = moduleElement.module.values
        if (moduleValues.isNotEmpty())
        {
            valueFont.drawString("+", elementX - 8, elementY + (moduleElement.height shr 1), -1)
            if (moduleElement.showSettings)
            {
                var yPos = elementY + 4
                for (value in moduleValues) yPos = drawAbstractValue(valueFont, moduleElement, value, yPos, mouseX, mouseY, valueColor)

                moduleElement.updatePressed()

                mouseDown = Mouse.isButtonDown(0)
                rightMouseDown = Mouse.isButtonDown(1)

                if (moduleElement.settingsWidth > 0.0f && yPos > elementY + 4) drawBorderedRect(elementX + 4f, elementY + 6f, elementX + moduleElement.settingsWidth, yPos + 2f, 1.0f, BACKGROUND, 0)
            }
        }
    }

    private fun drawAbstractValue(font: FontRenderer, moduleElement: ModuleElement, value: AbstractValue, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int = 0): Int
    {
        var yPos = _yPos
        when (value)
        {
            is ValueGroup ->
            {
                val moduleX = moduleElement.x + moduleElement.width
                val moduleIndentX = moduleX + indent

                val text = value.displayName
                val textWidth = font.getStringWidth(text) + indent + 16f

                if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                val moduleXEnd = moduleX + moduleElement.settingsWidth

                drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

                GlStateManager.resetColor()
                font.drawString("\u00A7c$text", moduleIndentX + 6, yPos + 4, WHITE)
                font.drawString(if (value.foldState) "-" else "+", (moduleXEnd - if (value.foldState) 5 else 6).toInt(), yPos + 4, WHITE)

                if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= yPos + 2 && mouseY <= yPos + 14 && Mouse.isButtonDown(0) && moduleElement.isntLeftPressed)
                {
                    value.foldState = !value.foldState
                    mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 1.0f))
                }

                yPos += 12

                if (value.foldState)
                {
                    val cachedYPos = yPos
                    val valuesInGroup = value.values.filter(AbstractValue::showCondition)
                    var i = 0
                    val j = valuesInGroup.size
                    while (i < j)
                    {
                        val valueOfGroup = valuesInGroup[i]
                        val textWidth2 = font.getStringWidth(valueOfGroup.displayName) + 12f

                        if (moduleElement.settingsWidth < textWidth2) moduleElement.settingsWidth = textWidth2

                        GlStateManager.resetColor()
                        yPos = drawAbstractValue(font, moduleElement, valueOfGroup, yPos, mouseX, mouseY, guiColor, indent + 10)

                        if (i == j - 1) // Last Index
                        {
                            drawRect(moduleIndentX + 7, cachedYPos, moduleIndentX + 8, yPos, LIGHT_GRAY)
                            drawRect(moduleIndentX + 7, yPos, moduleIndentX + 12, yPos + 1, LIGHT_GRAY)
                        }
                        i++
                    }
                }
            }

            is ColorValue -> yPos = drawColorValue(font, moduleElement, value, yPos, mouseX, mouseY, indent)
            is RangeValue<*> -> yPos = drawRangeValue(font, moduleElement, value, yPos, mouseX, mouseY, guiColor, indent)
            else -> yPos = drawValue(font, moduleElement, value as Value<*>, yPos, mouseX, mouseY, guiColor, indent)
        }
        return yPos
    }

    private fun drawColorValue(font: FontRenderer, moduleElement: ModuleElement, value: ColorValue, _yPos: Int, mouseX: Int, mouseY: Int, indent: Int = 0): Int
    {
        var yPos = _yPos
        val moduleX = moduleElement.x + moduleElement.width
        val moduleIndentX = moduleX + indent

        val alphaPresent = value is RGBAColorValue
        val text = "${value.displayName}\u00A7f: \u00A7c${value.getRed()} \u00A7a${value.getGreen()} \u00A79${value.getBlue()}${if (alphaPresent) " \u00A77${value.getAlpha()}" else ""}\u00A7r "
        val colorText = "(#${if (alphaPresent) encodeToHex(value.getAlpha()) else ""}${encodeToHex(value.getRed())}${encodeToHex(value.getGreen())}${encodeToHex(value.getBlue())})"
        val displayTextWidth = font.getStringWidth(text)
        val textWidth = displayTextWidth + font.getStringWidth(colorText) + indent + 2f

        if (moduleElement.settingsWidth < textWidth + 20f) moduleElement.settingsWidth = textWidth + 20f
        val moduleXEnd = moduleX + moduleElement.settingsWidth
        val sliderXEnd = moduleElement.settingsWidth - indent - 12f
        val newSliderValue = lazy(LazyThreadSafetyMode.NONE, (255 * ((mouseX - moduleElement.x - moduleElement.width - indent - 8f) / sliderXEnd).coerceIn(0f, 1f))::toInt)

        drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

        GlStateManager.resetColor()
        font.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)
        font.drawString(colorText, moduleIndentX + displayTextWidth + 6, yPos + 4, value.get(255))
        drawRect(moduleIndentX + textWidth, yPos + 4f, moduleXEnd - 4f, yPos + 10f, value.get())

        val renderSlider = { startY: Int, sliderValue: Int, color: Int ->
            drawRect(moduleIndentX + 8f, startY + 8f, moduleXEnd - 4, startY + 9f, LIGHT_GRAY)

            val sliderMarkXPos = (moduleIndentX + sliderXEnd * sliderValue / 255) + 8
            drawRect(sliderMarkXPos, startY + 5f, sliderMarkXPos + 3, startY + 11f, color)

            mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= startY + 15 && mouseY <= startY + 21 && Mouse.isButtonDown(0)
        }

        yPos += 10

        drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
        if (renderSlider(yPos, value.getRed(), -65536 /* 0xFFFF0000 */)) value.set(newSliderValue.value, value.getGreen(), value.getBlue(), value.getAlpha())

        yPos += 12

        drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
        if (renderSlider(yPos, value.getRed(), -16711936 /* 0xFF00FF00 */)) value.set(value.getRed(), newSliderValue.value, value.getBlue(), value.getAlpha())

        yPos += 12

        drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
        if (renderSlider(yPos, value.getRed(), -16776961 /* 0xFF0000FF */)) value.set(value.getRed(), value.getGreen(), newSliderValue.value, value.getAlpha())

        yPos += 12

        if (alphaPresent)
        {
            drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
            if (renderSlider(yPos, value.getAlpha(), LIGHT_GRAY)) value.set(value.getRed(), value.getGreen(), value.getBlue(), newSliderValue.value)

            yPos += 12
        }

        return yPos
    }

    private fun drawRangeValue(font: FontRenderer, moduleElement: ModuleElement, value: RangeValue<*>, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int = 0): Int
    {
        var yPos = _yPos
        val moduleX = moduleElement.x + moduleElement.width
        val moduleIndentX = moduleX + indent

        val valueDisplayName = value.displayName

        assumeNonVolatile {
            when (value)
            {
                is IntegerRangeValue ->
                {
                    val text = "$valueDisplayName\u00A7f: \u00A7c${value.getMin()}-${value.getMax()}"
                    val textWidth = font.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                    val moduleXEnd = moduleX + moduleElement.settingsWidth
                    val sliderXEnd = moduleElement.settingsWidth - indent - 12f

                    drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
                    drawRect(moduleIndentX + 8f, yPos + 18f, moduleXEnd - 4, yPos + 19f, LIGHT_GRAY)

                    val minSliderValue = (moduleIndentX + sliderXEnd * (value.getMin() - value.minimum) / (value.maximum - value.minimum)) + 8
                    drawRect(minSliderValue, yPos + 15f, minSliderValue + 3, yPos + 21f, guiColor)

                    val maxSliderValue = (moduleIndentX + sliderXEnd * (value.getMax() - value.minimum) / (value.maximum - value.minimum)) + 8
                    drawRect(maxSliderValue, yPos + 15f, maxSliderValue + 3, yPos + 21f, guiColor)

                    drawRect(minSliderValue + 3, yPos + 18f, maxSliderValue, yPos + 19f, guiColor)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= yPos + 15 && mouseY <= yPos + 21 && Mouse.isButtonDown(0))
                    {
                        val newValue = (value.minimum + (value.maximum - value.minimum) * ((mouseX - moduleElement.x - moduleElement.width - indent - 8f) / sliderXEnd).coerceIn(0f, 1f)).toInt()
                        if (mouseX > minSliderValue + (maxSliderValue - minSliderValue) * 0.5f) value.setMax(newValue)
                        else value.setMin(newValue)
                    }

                    GlStateManager.resetColor()
                    font.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

                    yPos += 22
                }

                is FloatRangeValue ->
                {
                    val text = "$valueDisplayName\u00A7f: \u00A7c${DECIMALFORMAT_4.format(value.getMin())}-${DECIMALFORMAT_4.format(value.getMax())}"
                    val textWidth = font.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                    val moduleXEnd = moduleX + moduleElement.settingsWidth
                    val sliderXEnd = moduleElement.settingsWidth - indent - 12

                    drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
                    drawRect(moduleIndentX + 8f, yPos + 18f, moduleXEnd - 4, yPos + 19f, LIGHT_GRAY)

                    val minSliderValue = moduleIndentX + sliderXEnd * (value.getMin() - value.minimum) / (value.maximum - value.minimum) + 8
                    drawRect(minSliderValue, yPos + 15f, minSliderValue + 3, yPos + 21f, guiColor)

                    val maxSliderValue = moduleIndentX + sliderXEnd * (value.getMax() - value.minimum) / (value.maximum - value.minimum) + 8
                    drawRect(maxSliderValue, yPos + 15f, maxSliderValue + 3, yPos + 21f, guiColor)

                    drawRect(minSliderValue + 3, yPos + 18f, maxSliderValue, yPos + 19f, guiColor)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd - 4 && mouseY >= yPos + 15 && mouseY <= yPos + 21 && Mouse.isButtonDown(0))
                    {
                        val newValue = round(value.minimum + (value.maximum - value.minimum) * ((mouseX - moduleElement.x - moduleElement.width - indent - 8) / sliderXEnd).coerceIn(0f, 1f))
                        if (mouseX > minSliderValue + (maxSliderValue - minSliderValue) * 0.5f) value.setMax(newValue.toFloat())
                        else value.setMin(newValue.toFloat())
                    }

                    GlStateManager.resetColor()

                    font.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

                    yPos += 22
                }
            }
        }

        return yPos
    }

    private fun drawValue(valueFont: FontRenderer, moduleElement: ModuleElement, value: Value<*>, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int = 0): Int
    {
        var yPos = _yPos
        val moduleX = moduleElement.x + moduleElement.width
        val moduleIndentX = moduleX + indent

        val valueDisplayName = value.displayName

        assumeVolatileIf(value.get() is Number) {
            when (value)
            {
                is BoolValue ->
                {
                    val textWidth = valueFont.getStringWidth(valueDisplayName) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth

                    drawRect(moduleX + 4f, yPos + 2f, moduleX + moduleElement.settingsWidth, yPos + 14f, BACKGROUND)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleX + moduleElement.settingsWidth && mouseY >= yPos + 2 && mouseY <= yPos + 14 && Mouse.isButtonDown(0) && moduleElement.isntLeftPressed)
                    {
                        value.set(!value.get())
                        mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 1.0f))
                    }

                    GlStateManager.resetColor()

                    valueFont.drawString(valueDisplayName, moduleIndentX + 6, yPos + 4, if (value.get()) guiColor else LIGHT_GRAY)

                    yPos += 12
                }

                is ListValue ->
                {
                    val textWidth = valueFont.getStringWidth(valueDisplayName) + indent + 16f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                    var moduleXEnd = moduleX + moduleElement.settingsWidth

                    drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

                    GlStateManager.resetColor()

                    valueFont.drawString("\u00A7c$valueDisplayName", moduleIndentX + 6, yPos + 4, WHITE)
                    valueFont.drawString(if (value.openList) "-" else "+", (moduleXEnd - if (value.openList) 5 else 6).toInt(), yPos + 4, WHITE)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= yPos + 2 && mouseY <= yPos + 14 && Mouse.isButtonDown(0) && moduleElement.isntLeftPressed)
                    {
                        value.openList = !value.openList
                        mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 1.0f))
                    }

                    yPos += 12

                    for (valueOfList in value.values)
                    {
                        val textWidth2 = valueFont.getStringWidth("> $valueOfList") + indent + 8f

                        if (moduleElement.settingsWidth < textWidth2)
                        {
                            moduleElement.settingsWidth = textWidth2
                            moduleXEnd = moduleX + textWidth2
                        }

                        if (value.openList)
                        {
                            drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

                            if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= yPos + 2 && mouseY <= yPos + 14 && Mouse.isButtonDown(0) && moduleElement.isntLeftPressed)
                            {
                                value.set(valueOfList)
                                mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 1.0f))
                            }

                            GlStateManager.resetColor()

                            valueFont.drawString(">", moduleIndentX + 6, yPos + 4, LIGHT_GRAY)
                            valueFont.drawString(valueOfList, moduleIndentX + 14, yPos + 4, if (value.get().equals(valueOfList, ignoreCase = true)) guiColor else LIGHT_GRAY)

                            yPos += 12
                        }
                    }
                }

                is IntegerValue ->
                {
                    val text = valueDisplayName + "\u00A7f: \u00A7c" + if (value is BlockValue) getBlockName(value.get()) + " (" + value.get() + ")" else value.get()
                    val textWidth = valueFont.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                    val moduleXEnd = moduleX + moduleElement.settingsWidth
                    val sliderXEnd = moduleElement.settingsWidth - indent - 12

                    drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
                    drawRect(moduleIndentX + 8f, yPos + 18f, moduleXEnd - 4, yPos + 19f, LIGHT_GRAY)

                    val sliderValue = moduleIndentX + sliderXEnd * (value.get() - value.minimum) / (value.maximum - value.minimum)
                    drawRect(8 + sliderValue, yPos + 15f, sliderValue + 11, yPos + 21f, guiColor)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd && mouseY >= yPos + 15 && mouseY <= yPos + 21 && Mouse.isButtonDown(0)) value.set((value.minimum + (value.maximum - value.minimum) * ((mouseX - (moduleIndentX + 8)) / sliderXEnd).coerceIn(0f, 1f)).toInt())

                    GlStateManager.resetColor()

                    valueFont.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

                    yPos += 22
                }

                is FloatValue ->
                {
                    val text = valueDisplayName + "\u00A7f: \u00A7c" + DECIMALFORMAT_4.format(value.get())
                    val textWidth = valueFont.getStringWidth(text) + indent + 8f

                    if (moduleElement.settingsWidth < textWidth) moduleElement.settingsWidth = textWidth
                    val moduleXEnd = moduleX + moduleElement.settingsWidth
                    val sliderXEnd = moduleElement.settingsWidth - indent - 12

                    drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
                    drawRect(moduleIndentX + 8f, yPos + 18f, moduleXEnd - 4f, yPos + 19f, LIGHT_GRAY)

                    val sliderValue = moduleIndentX + sliderXEnd * (value.get() - value.minimum) / (value.maximum - value.minimum)
                    drawRect(8 + sliderValue, yPos + 15f, sliderValue + 11, yPos + 21f, guiColor)

                    if (mouseX >= moduleIndentX + 4 && mouseX <= moduleXEnd - 4 && mouseY >= yPos + 15 && mouseY <= yPos + 21 && Mouse.isButtonDown(0)) value.set(round(value.minimum + (value.maximum - value.minimum) * ((mouseX - (moduleIndentX + 8)) / sliderXEnd).coerceIn(0f, 1f)).toFloat())

                    GlStateManager.resetColor()

                    valueFont.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

                    yPos += 22
                }

                is FontValue ->
                {
                    val fontRenderer = value.get()

                    drawRect(moduleX + 4f, yPos + 2f, moduleX + moduleElement.settingsWidth, yPos + 14f, BACKGROUND)

                    var displayString = "Font: Unknown"

                    if (fontRenderer is GameFontRenderer) displayString = "Font: " + fontRenderer.defaultFont.font.name + " - " + fontRenderer.defaultFont.font.size
                    else if (fontRenderer == Fonts.minecraftFont) displayString = "Font: Minecraft"
                    else
                    {
                        val fontInfo = Fonts.getFontDetails(fontRenderer)
                        if (fontInfo != null) displayString = fontInfo.name + if (fontInfo.fontSize == -1) "" else " - " + fontInfo.fontSize
                    }

                    valueFont.drawString(displayString, moduleIndentX + 6, yPos + 4, WHITE)

                    val stringWidth = valueFont.getStringWidth(displayString)

                    if (moduleElement.settingsWidth < stringWidth + 8) moduleElement.settingsWidth = stringWidth + 8f

                    if ((Mouse.isButtonDown(0) && !mouseDown || Mouse.isButtonDown(1) && !rightMouseDown) && mouseX >= moduleIndentX + 4 && mouseX <= moduleX + moduleElement.settingsWidth && mouseY >= yPos + 4 && mouseY <= yPos + 12)
                    {
                        val fonts = Fonts.fonts

                        if (Mouse.isButtonDown(0))
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

                    yPos += 11
                }

                else ->
                {
                    val text = valueDisplayName + "\u00A7f: \u00A7c" + value.get()
                    val textWidth = valueFont.getStringWidth(text)

                    if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8f

                    drawRect(moduleX + 4f, yPos + 2f, moduleX + moduleElement.settingsWidth, yPos + 14f, BACKGROUND)

                    GlStateManager.resetColor()

                    valueFont.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

                    yPos += 12
                }
            }
        }

        return yPos
    }

    companion object
    {
        private fun encodeToHex(hex: Int) = hex.toString(16).uppercase().padStart(2, '0')

        private fun round(f: Float): BigDecimal = BigDecimal("$f").setScale(4, RoundingMode.HALF_UP)
    }
}
