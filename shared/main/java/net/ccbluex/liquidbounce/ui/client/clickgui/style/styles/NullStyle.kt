/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IGlStateManager
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generateButtonColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generateDescriptionColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generatePanelColor
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
import net.ccbluex.liquidbounce.ui.font.assumeVolatile
import net.ccbluex.liquidbounce.ui.font.assumeVolatileIf
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.serialized
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_4
import net.ccbluex.liquidbounce.utils.misc.StringUtils.stripControlCodes
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Mouse
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

private const val WHITE = -1
private const val LIGHT_GRAY = Int.MAX_VALUE
private const val BACKGROUND = Int.MIN_VALUE

private const val SLIDER_START_SHIFT = 8f

class NullStyle : Style()
{
	private var mouseDown = false
	private var rightMouseDown = false
	private var queuedTask: (() -> Unit)? = null

	override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
	{
		val font = getPanelFont()
		val fontHeight = font.fontHeight
		val xF = panel.x.toFloat()
		val yF = panel.y.toFloat()

		drawRect(xF - 3, yF, xF + panel.width + 3, yF + fontHeight + 10, generatePanelColor())

		if (panel.fade > 0) drawBorderedRect(xF, yF + fontHeight + 10, xF + panel.width, panel.y + 19f + panel.fade, 1f, BACKGROUND, BACKGROUND)

		classProvider.glStateManager.resetColor()

		val textWidth = font.getStringWidth("\u00A7f" + stripControlCodes(panel.name))
		font.drawString("\u00A7f" + panel.name, (panel.x - (textWidth - 100.0f) * 0.5f).toInt(), panel.y + 7, LIGHT_GRAY)
	}

	override fun drawDescription(mouseX: Int, mouseY: Int, text: String)
	{
		val font = getDescriptionFont()
		val fontHeight = font.fontHeight
		val textWidth = font.getStringWidth(text)

		drawRect(mouseX + 9, mouseY, mouseX + textWidth + 14, mouseY + fontHeight + 3, generateDescriptionColor())

		classProvider.glStateManager.resetColor()

		font.drawString(text, mouseX + 12, mouseY + (fontHeight shr 1), LIGHT_GRAY)
	}

	override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
	{
		val font = getButtonFont()

		classProvider.glStateManager.resetColor()
		font.drawString(buttonElement.displayName, (buttonElement.x - (font.getStringWidth(buttonElement.displayName) - 100.0f) * 0.5f).toInt(), buttonElement.y + 6, buttonElement.color)
	}

	override fun drawModuleElement(mouseX: Int, mouseY: Int, moduleElement: ModuleElement)
	{
		val buttonFont = getButtonFont()

		val valueColor = generateValueColor()
		val valueFont = getValueFont()
		val glStateManager = classProvider.glStateManager

		glStateManager.resetColor()

		val elementX = moduleElement.x + moduleElement.width
		val elementY = moduleElement.y
		buttonFont.drawString(moduleElement.displayName, (moduleElement.x - (buttonFont.getStringWidth(moduleElement.displayName) - 100.0f) * 0.5f).toInt(), elementY + 6, if (moduleElement.module.state) generateButtonColor() else LIGHT_GRAY)

		val moduleValues = moduleElement.module.values.filter(AbstractValue::showCondition)

		if (moduleValues.isNotEmpty())
		{
			valueFont.drawString("+", elementX - 8 - if (moduleElement.isHovering(mouseX, mouseY)) 2 else 0, elementY + (moduleElement.height shr 1), WHITE)

			if (moduleElement.showSettings)
			{
				queuedTask = null

				var yPos = elementY + 4

				for (value in moduleValues) yPos = drawAbstractValue(valueFont, glStateManager, moduleElement, value, yPos, mouseX, mouseY, valueColor)

				moduleElement.updatePressed()

				mouseDown = Mouse.isButtonDown(0)
				rightMouseDown = Mouse.isButtonDown(1)

				if (moduleElement.settingsWidth > 0.0f && yPos > elementY + 4) drawBorderedRect(elementX + 4f, elementY + 6f, elementX + moduleElement.settingsWidth, yPos + 2f, 1.0f, BACKGROUND, 0)

				queuedTask?.invoke()
			}
		}
	}

	private fun drawAbstractValue(font: IFontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: AbstractValue, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int = 0): Int
	{
		var yPos = _yPos
		yPos = when (value)
		{
			is ValueGroup -> drawValueGroup(font, glStateManager, moduleElement, value, yPos, mouseX, mouseY, guiColor, indent)
			is ColorValue -> drawColorValue(font, glStateManager, moduleElement, value, yPos, mouseX, mouseY, indent)
			is RangeValue<*> -> drawRangeValue(font, glStateManager, moduleElement, value, yPos, mouseX, mouseY, guiColor, indent)
			else -> drawValue(font, glStateManager, moduleElement, value as Value<*>, yPos, mouseX, mouseY, guiColor, indent)
		}
		return yPos
	}

	private fun drawValueGroup(font: IFontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: ValueGroup, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int): Int
	{
		var yPos = _yPos
		val moduleX = moduleElement.x + moduleElement.width
		val moduleIndentX = moduleX + indent

		val text = value.displayName
		val textWidth = font.getStringWidth(text) + indent

		if (moduleElement.settingsWidth < textWidth + 16f) moduleElement.settingsWidth = textWidth + 16f
		val moduleXEnd = moduleX + moduleElement.settingsWidth

		drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

		glStateManager.resetColor()

		val mouseOver = mouseX in moduleIndentX + 4..moduleXEnd.toInt() && mouseY in yPos + 2..yPos + 14

		font.drawString("\u00A7c$text", moduleIndentX + 6, yPos + 4, WHITE)
		font.drawString(if (value.foldState) "-" else "+", (moduleXEnd - (if (value.foldState) 5 else 6) - if (mouseOver) 2 else 0).toInt(), yPos + 4, WHITE)

		if (mouseOver && Mouse.isButtonDown(0) && moduleElement.isntPressed)
		{
			value.foldState = !value.foldState
			mc.soundHandler.playSound("gui.button.press", 1.0f)
		}

		yPos += 12

		drawValueDescription(value, mouseX, mouseY, moduleIndentX + 6, _yPos, textWidth, yPos)

		if (value.foldState)
		{
			val startYPos = yPos
			val valuesInGroup = value.values.filter(AbstractValue::showCondition)
			var i = 0
			val j = valuesInGroup.size
			while (i < j)
			{
				val valueOfGroup = valuesInGroup[i]
				val textWidthOfValue = font.getStringWidth(valueOfGroup.displayName) + 12f

				if (moduleElement.settingsWidth < textWidthOfValue) moduleElement.settingsWidth = textWidthOfValue

				glStateManager.resetColor()
				yPos = drawAbstractValue(font, glStateManager, moduleElement, valueOfGroup, yPos, mouseX, mouseY, guiColor, indent + 10)

				if (i == j - 1) // Last Index
				{
					drawRect(moduleIndentX + 7, startYPos, moduleIndentX + 8, yPos, LIGHT_GRAY)
					drawRect(moduleIndentX + 7, yPos, moduleIndentX + 12, yPos + 1, LIGHT_GRAY)
				}
				i++
			}
		}

		return yPos
	}

	private fun drawColorValue(font: IFontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: ColorValue, _yPos: Int, mouseX: Int, mouseY: Int, indent: Int = 0): Int
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

		drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

		glStateManager.resetColor()
		font.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)
		font.drawString(colorText, moduleIndentX + displayTextWidth + 6, yPos + 4, value.get(255))
		drawRect(moduleX + textWidth, yPos + 4f, moduleXEnd - 4f, yPos + 10f, value.get())

		drawValueDescription(value, mouseX, mouseY, moduleIndentX + 6, yPos + 2, textWidth.toInt(), yPos + 14)

		yPos += 10

		drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
		drawSlider(value.getRed().toFloat(), 0f, 255f, moduleX, moduleXEnd, yPos + 5, moduleElement.settingsWidth, mouseX, mouseY, indent, -65536 /* 0xFFFF0000 */) {
			value.set(it.toInt(), value.getGreen(), value.getBlue(), value.getAlpha())
		}

		yPos += 12

		drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
		drawSlider(value.getGreen().toFloat(), 0f, 255f, moduleX, moduleXEnd, yPos + 5, moduleElement.settingsWidth, mouseX, mouseY, indent, -16711936 /* 0xFF00FF00 */) {
			value.set(value.getRed(), it.toInt(), value.getBlue(), value.getAlpha())
		}

		yPos += 12

		drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
		drawSlider(value.getBlue().toFloat(), 0f, 255f, moduleX, moduleXEnd, yPos + 5, moduleElement.settingsWidth, mouseX, mouseY, indent, -16776961 /* 0xFF0000FF */) {
			value.set(value.getRed(), value.getGreen(), it.toInt(), value.getAlpha())
		}

		yPos += 12

		if (alphaPresent)
		{
			drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)
			drawSlider(value.getAlpha().toFloat(), 0f, 255f, moduleX, moduleXEnd, yPos + 5, moduleElement.settingsWidth, mouseX, mouseY, indent, LIGHT_GRAY) {
				value.set(value.getRed(), value.getGreen(), value.getBlue(), it.toInt())
			}

			yPos += 12
		}

		return yPos
	}

	private fun drawRangeValue(font: IFontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: RangeValue<*>, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int = 0): Int
	{
		var yPos = _yPos
		val moduleX = moduleElement.x + moduleElement.width
		val moduleIndentX = moduleX + indent

		val valueDisplayName = value.displayName

		assumeVolatile {
			when (value)
			{
				is IntegerRangeValue ->
				{
					val text = "$valueDisplayName\u00A7f: \u00A7c${value.getMin()}-${value.getMax()}"
					val textWidth = font.getStringWidth(text) + indent

					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f
					val moduleXEnd = moduleX + moduleElement.settingsWidth

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
					drawRangeSlider(value.getMin().toFloat(), value.getMax().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), moduleX, moduleXEnd, yPos + 15, moduleElement.settingsWidth, mouseX, mouseY, indent, guiColor, value::setMin, value::setMax)

					glStateManager.resetColor()
					font.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, yPos + 2, textWidth, yPos + 12)

					yPos += 22
				}

				is FloatRangeValue ->
				{
					val text = "$valueDisplayName\u00A7f: \u00A7c${DECIMALFORMAT_4.format(value.getMin())}-${DECIMALFORMAT_4.format(value.getMax())}"
					val textWidth = font.getStringWidth(text) + indent

					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f
					val moduleXEnd = moduleX + moduleElement.settingsWidth

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
					drawRangeSlider(value.getMin(), value.getMax(), value.minimum, value.maximum, moduleX, moduleXEnd, yPos + 15, moduleElement.settingsWidth, mouseX, mouseY, indent, guiColor, value::setMin, value::setMax)

					glStateManager.resetColor()

					font.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, yPos + 2, textWidth, yPos + 12)

					yPos += 22
				}
			}
		}

		return yPos
	}

	private fun drawValue(valueFont: IFontRenderer, glStateManager: IGlStateManager, moduleElement: ModuleElement, value: Value<*>, _yPos: Int, mouseX: Int, mouseY: Int, guiColor: Int, indent: Int = 0): Int
	{
		var yPos = _yPos
		val moduleX = moduleElement.x + moduleElement.width
		val moduleIndentX = moduleX + indent

		val soundHandler = mc.soundHandler
		val valueDisplayName = value.displayName

		assumeVolatileIf(value.get() is Number) {
			when (value)
			{
				is BoolValue ->
				{
					val textWidth = valueFont.getStringWidth(valueDisplayName) + indent

					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f
					val moduleXEnd = moduleX + moduleElement.settingsWidth

					val mouseOver = mouseX in moduleIndentX..moduleXEnd.toInt() && mouseY in yPos + 2..yPos + 14

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

					if (mouseOver && Mouse.isButtonDown(0) && moduleElement.isntPressed)
					{
						value.set(!value.get())
						soundHandler.playSound("gui.button.press", 1.0f)
					}

					glStateManager.resetColor()

					valueFont.drawString(valueDisplayName, moduleIndentX + if (mouseOver) 8 else 6, yPos + 4, if (value.get()) guiColor else LIGHT_GRAY)

					yPos += 12

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, _yPos, textWidth, yPos)
				}

				is ListValue ->
				{
					val textWidth = valueFont.getStringWidth(valueDisplayName) + indent

					if (moduleElement.settingsWidth < textWidth + 16f) moduleElement.settingsWidth = textWidth + 16f
					var moduleXEnd = moduleX + moduleElement.settingsWidth

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

					glStateManager.resetColor()

					var mouseOver = mouseX in moduleIndentX + 4..moduleXEnd.toInt() && mouseY in yPos + 2..yPos + 14

					valueFont.drawString("\u00A7c$valueDisplayName", moduleIndentX + 6, yPos + 4, WHITE)
					valueFont.drawString(if (value.openList) "-" else "+", (moduleXEnd - (if (value.openList) 5 else 6) - if (mouseOver) 2 else 0).toInt(), yPos + 4, WHITE)

					if (mouseOver && Mouse.isButtonDown(0) && moduleElement.isntPressed)
					{
						value.openList = !value.openList
						soundHandler.playSound("gui.button.press", 1.0f)
					}

					yPos += 12

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, _yPos, textWidth, yPos)

					for (valueOfList in value.values) if (value.openList)
					{
						val textWidth2 = valueFont.getStringWidth("> $valueOfList") + indent + 12f

						if (moduleElement.settingsWidth < textWidth2)
						{
							moduleElement.settingsWidth = textWidth2
							moduleXEnd = moduleX + textWidth2
						}

						drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

						mouseOver = mouseX in moduleIndentX + 4..moduleXEnd.toInt() && mouseY in yPos + 2..yPos + 14

						if (mouseOver && Mouse.isButtonDown(0) && moduleElement.isntPressed)
						{
							value.set(valueOfList)
							soundHandler.playSound("gui.button.press", 1.0f)
						}

						glStateManager.resetColor()

						valueFont.drawString(">", moduleIndentX + (if (mouseOver) 8 else 6), yPos + 4, LIGHT_GRAY)
						valueFont.drawString(valueOfList, moduleIndentX + 14, yPos + 4, if (value.get().equals(valueOfList, ignoreCase = true)) guiColor else LIGHT_GRAY)

						yPos += 12
					}
				}

				is IntegerValue ->
				{
					val text = "$valueDisplayName\u00A7f: \u00A7c${if (value is BlockValue) "${getBlockName(value.get())} (${value.get()})" else "${value.get()}"}"
					val textWidth = valueFont.getStringWidth(text) + indent

					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f
					val moduleXEnd = moduleX + moduleElement.settingsWidth

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
					drawSlider(value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), moduleX, moduleXEnd, yPos + 15, moduleElement.settingsWidth, mouseX, mouseY, indent, guiColor, value::set)

					glStateManager.resetColor()

					valueFont.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, yPos + 2, textWidth, yPos + 12)

					yPos += 22
				}

				is FloatValue ->
				{
					val text = "$valueDisplayName\u00A7f: \u00A7c${DECIMALFORMAT_4.format(value.get())}"
					val textWidth = valueFont.getStringWidth(text) + indent

					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f
					val moduleXEnd = moduleX + moduleElement.settingsWidth

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 24f, BACKGROUND)
					drawSlider(value.get(), value.minimum, value.maximum, moduleX, moduleXEnd, yPos + 15, moduleElement.settingsWidth, mouseX, mouseY, indent, guiColor, value::set)

					glStateManager.resetColor()

					valueFont.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, yPos + 2, textWidth, yPos + 12)

					yPos += 22
				}

				is FontValue ->
				{
					val moduleXEnd = moduleX + moduleElement.settingsWidth
					val fontRenderer = value.get()

					val mouseOver = mouseX in moduleIndentX + 4..moduleXEnd.toInt() && mouseY in yPos + 4..yPos + 12

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

					val displayString = "$valueDisplayName: ${fontRenderer.serialized ?: "Unknown"}"
					valueFont.drawString(displayString, moduleIndentX + if (mouseOver) 8 else 6, yPos + 4, WHITE)

					val textWidth = valueFont.getStringWidth(displayString) + indent
					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f

					if (mouseOver)
					{
						// DEBUG CODE START //
						if (Mouse.isButtonDown(0)) ClientUtils.displayChatMessage(mc.thePlayer, "Pressed left click (prev=$mouseDown)")
						if (Mouse.isButtonDown(1)) ClientUtils.displayChatMessage(mc.thePlayer, "Pressed right click (prev=$rightMouseDown)")
						// DEBUG CODE END //

						if (Mouse.isButtonDown(0) && !mouseDown || Mouse.isButtonDown(1) && !rightMouseDown)
						{
							val fonts = Fonts.fonts
							val index = fonts.indexOf(fontRenderer)
							if (Mouse.isButtonDown(0)) value.set(fonts[if (index + 1 >= fonts.size) 0 else index + 1]) // Next font
							else value.set(fonts[if (index - 1 < 0) fonts.size - 1 else index - 1]) // Previous font
						}
					}

					yPos += 11

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, _yPos, textWidth, yPos)
				}

				else ->
				{
					val text = "$valueDisplayName\u00A7f: \u00A7c${value.get()}"
					val textWidth = valueFont.getStringWidth(text) + indent

					if (moduleElement.settingsWidth < textWidth + 8f) moduleElement.settingsWidth = textWidth + 8f
					val moduleXEnd = moduleX + moduleElement.settingsWidth

					drawRect(moduleX + 4f, yPos + 2f, moduleXEnd, yPos + 14f, BACKGROUND)

					glStateManager.resetColor()

					valueFont.drawString(text, moduleIndentX + 6, yPos + 4, WHITE)

					yPos += 12

					drawValueDescription(value, mouseX, mouseY, moduleIndentX, _yPos, textWidth, yPos)
				}
			}
		}

		return yPos
	}

	private fun drawValueDescription(value: AbstractValue, mouseX: Int, mouseY: Int, valueBBX1: Int, valueBBY1: Int, valueBBX2: Int, valueBBY2: Int)
	{
		if (mouseX in min(valueBBX1, valueBBX2)..max(valueBBX1, valueBBX2) && mouseY in min(valueBBY1, valueBBY2)..max(valueBBY1, valueBBY2) && value.description.isNotBlank()) queuedTask = { drawDescription(mouseX, mouseY, value.description) }
	}

	companion object
	{
		private fun encodeToHex(hex: Int) = hex.toString(16).toUpperCase().padStart(2, '0')

		private inline fun drawSlider(value: Float, min: Float, max: Float, xStart: Int, xEnd: Float, y: Int, settingsWidth: Float, mouseX: Int, mouseY: Int, indent: Int, color: Int, onChanged: (Float) -> Unit)
		{
			val indentX = xStart + indent
			val sliderXEnd = settingsWidth - indent - 12

			val mouseOver = mouseX in (indentX + 4)..xEnd.toInt() && mouseY in y..y + 6

			// Slider
			drawRect(indentX + SLIDER_START_SHIFT, y + 3f, xEnd - 4f, y + 4f, LIGHT_GRAY)

			// Slider mark
			val sliderValue = indentX + sliderXEnd * (value.coerceIn(min, max) - min) / (max - min)
			val offset = if (mouseOver) 1 else 0
			drawRect((sliderValue + SLIDER_START_SHIFT).toInt() - offset, y - offset, (sliderValue + SLIDER_START_SHIFT).toInt() + offset + 3, y + offset + 6, color)

			if (mouseOver && Mouse.isButtonDown(0)) onChanged(round(min + (max - min) * ((mouseX - (indentX + SLIDER_START_SHIFT)) / sliderXEnd).coerceIn(0f, 1f)).toFloat())
		}

		private inline fun drawRangeSlider(minValue: Float, maxValue: Float, min: Float, max: Float, xStart: Int, xEnd: Float, y: Int, settingsWidth: Float, mouseX: Int, mouseY: Int, indent: Int, color: Int, onMinChanged: (Float) -> Unit, onMaxChanged: (Float) -> Unit)
		{
			val indentX = xStart + indent
			val sliderXEnd = settingsWidth - indent - 12

			val minSliderValue = indentX + sliderXEnd * (minValue.coerceIn(min, max) - min) / (max - min)
			val maxSliderValue = indentX + sliderXEnd * (maxValue.coerceIn(min, max) - min) / (max - min)
			val mouseOver = mouseX in indentX..xEnd.toInt() && mouseY in y..y + 6
			val maxMouseOver = mouseX > minSliderValue + (maxSliderValue - minSliderValue) * 0.5f

			// Slider
			drawRect(indentX + SLIDER_START_SHIFT, y + 3f, xEnd - 4f, y + 4f, LIGHT_GRAY)

			// Slider mark (min)
			val minSliderOffset = if (mouseOver && !maxMouseOver) 1 else 0
			drawRect((minSliderValue + SLIDER_START_SHIFT).toInt() - minSliderOffset, y - minSliderOffset, (minSliderValue + SLIDER_START_SHIFT).toInt() + minSliderOffset + 3, y + minSliderOffset + 6, color)

			// Slider mark (max)
			val maxSliderOffset = if (mouseOver && maxMouseOver) 1 else 0
			drawRect((maxSliderValue + SLIDER_START_SHIFT).toInt() - maxSliderOffset, y - maxSliderOffset, (maxSliderValue + SLIDER_START_SHIFT).toInt() + maxSliderOffset + 3, y + maxSliderOffset + 6, color)

			drawRect(minSliderValue + SLIDER_START_SHIFT, y + 3f, maxSliderValue + SLIDER_START_SHIFT, y + 4f, color)

			if (mouseOver && Mouse.isButtonDown(0))
			{
				val newValue = round(min + (max - min) * ((mouseX - (indentX + SLIDER_START_SHIFT)) / sliderXEnd).coerceIn(0f, 1f)).toFloat()
				if (maxMouseOver) onMaxChanged(newValue) else onMinChanged(newValue)
			}
		}

		private fun round(f: Float): BigDecimal = BigDecimal("$f").setScale(4, RoundingMode.HALF_UP)
	}
}
