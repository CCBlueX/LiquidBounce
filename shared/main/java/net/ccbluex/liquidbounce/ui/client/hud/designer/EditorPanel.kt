/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.designer

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getPanelFont
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.getValueFont
import net.ccbluex.liquidbounce.injection.backend.ClassProviderImpl.glStateManager
import net.ccbluex.liquidbounce.ui.client.hud.HUD.Companion.createDefault
import net.ccbluex.liquidbounce.ui.client.hud.HUD.Companion.elements
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode

class EditorPanel(private val hudDesigner: GuiHudDesigner, var x: Int, var y: Int) : MinecraftInstance()
{

	var width = 80
		private set
	var height = 20
		private set
	var realHeight = 20
		private set

	private var drag = false
	private var dragX = 0
	private var dragY = 0

	private var mouseDown = false

	private var scroll = 0

	var create = false
	private var currentElement: Element? = null

	/**
	 * Draw editor panel to screen
	 *
	 * @param mouseX
	 * @param mouseY
	 * @param wheel
	 */
	fun drawPanel(mouseX: Int, mouseY: Int, wheel: Int)
	{ // Drag panel
		drag(mouseX, mouseY)

		// Set current element
		if (currentElement != hudDesigner.selectedElement) scroll = 0
		currentElement = hudDesigner.selectedElement

		// Scrolling start
		var currMouseY = mouseY
		val shouldScroll = realHeight > 200

		if (shouldScroll)
		{
			GL11.glPushMatrix()
			RenderUtils.makeScissorBox(x.toFloat(), y + 1F, x + width.toFloat(), y + 200F)
			GL11.glEnable(GL11.GL_SCISSOR_TEST)

			if (y + 200 < currMouseY) currMouseY = -1

			if (mouseX >= x && mouseX <= x + width && currMouseY >= y && currMouseY <= y + 200 && Mouse.hasWheel())
			{
				if (wheel < 0 && -scroll + 205 <= realHeight)
				{
					scroll -= 12
				}
				else if (wheel > 0)
				{
					scroll += 12
					if (scroll > 0) scroll = 0
				}
			}
		}

		// Draw panel
		RenderUtils.drawRect(x, y + 12, x + width, y + realHeight, -14999000)
		when
		{
			create -> drawCreate(mouseX, currMouseY)
			currentElement != null -> drawEditor(mouseX, currMouseY)
			else -> drawSelection(mouseX, currMouseY)
		}

		// Scrolling end
		if (shouldScroll)
		{
			RenderUtils.drawRect(x + width - 5, y + 15, x + width - 2, y + 197, -14079703)

			val v = 197 * (-scroll / (realHeight - 170F))
			RenderUtils.drawRect(x + width - 5F, y + 15 + v, x + width - 2F, y + 20 + v, -14319873)

			GL11.glDisable(GL11.GL_SCISSOR_TEST)
			GL11.glPopMatrix()
		}

		// Save mouse states
		mouseDown = Mouse.isButtonDown(0)
	}

	/**
	 * Draw create panel
	 */
	private fun drawCreate(mouseX: Int, mouseY: Int)
	{
		val font = getValueFont()

		height = 15 + scroll
		realHeight = 15
		width = 90

		for (element in elements)
		{
			val info = element.getAnnotation(ElementInfo::class.java) ?: continue

			if (info.single && LiquidBounce.hud.elements.any { it.javaClass == element }) continue

			val name = info.name

			font.drawString(name, x + 2.0f, y + height.toFloat(), -1)

			val stringWidth = font.getStringWidth(name)
			if (width < stringWidth + 8) width = stringWidth + 8

			if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10)
			{
				try
				{
					val newElement = element.newInstance()

					if (newElement.createElement()) LiquidBounce.hud.addElement(newElement)
				}
				catch (e: InstantiationException)
				{
					e.printStackTrace()
				}
				catch (e: IllegalAccessException)
				{
					e.printStackTrace()
				}
				create = false
			}

			height += 10
			realHeight += 10
		}

		RenderUtils.drawRect(x, y, x + width, y + 12, ClickGUI.generatePanelColor())
		font.drawString("\u00A7lCreate element", x + 2F, y + 3.5F, -1)
	}

	/**
	 * Draw selection panel
	 */
	private fun drawSelection(mouseX: Int, mouseY: Int)
	{
		val font = getValueFont()

		height = 15 + scroll
		realHeight = 15
		width = 120

		font.drawString("\u00A7lCreate element", x + 2.0f, y.toFloat() + height, -1)
		if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10) create = true

		height += 10
		realHeight += 10

		font.drawString("\u00A7lReset", x + 2f, y.toFloat() + height, -1)
		if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10) LiquidBounce.hud = createDefault()

		height += 15
		realHeight += 15

		font.drawString("\u00A7lAvailable Elements", x + 2.0f, y + height.toFloat(), -1)
		height += 10
		realHeight += 10

		for (element in LiquidBounce.hud.elements)
		{
			font.drawString(element.name, x + 2, y + height, -1)

			val stringWidth = font.getStringWidth(element.name)
			if (width < stringWidth + 8) width = stringWidth + 8

			if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10) hudDesigner.selectedElement = element

			height += 10
			realHeight += 10
		}

		RenderUtils.drawRect(x, y, x + width, y + 12, ClickGUI.generatePanelColor())
		RenderUtils.resetColor()
		font.drawString("\u00A7lEditor", x + 2F, y + 3.5f, -1)
	}

	/**
	 * Draw editor panel
	 */
	private fun drawEditor(mouseX: Int, mouseY: Int)
	{
		height = scroll + 15
		realHeight = 15

		val prevWidth = width
		width = 100

		val element = currentElement ?: return
		val valueFont = getValueFont()

		// X
		valueFont.drawString("X: ${"%.2f".format(element.renderX)} (${"%.2f".format(element.x)})", x + 2, y + height, -1)
		height += 10
		realHeight += 10

		// Y
		valueFont.drawString("Y: ${"%.2f".format(element.renderY)} (${"%.2f".format(element.y)})", x + 2, y + height, -1)
		height += 10
		realHeight += 10

		// Scale
		valueFont.drawString("Scale: ${"%.2f".format(element.scale)}", x + 2, y + height, -1)
		height += 10
		realHeight += 10

		// Horizontal
		valueFont.drawString("H:", x + 2, y + height, -1)
		valueFont.drawString(element.side.horizontal.sideName, x + 12, y + height, Color.GRAY.rgb)

		val provider = classProvider

		if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10)
		{
			val values = Side.Horizontal.values()
			val currIndex = values.indexOf(element.side.horizontal)

			val x = element.renderX

			element.side.horizontal = values[if (currIndex + 1 >= values.size) 0 else currIndex + 1]
			element.x = when (element.side.horizontal)
			{
				Side.Horizontal.LEFT -> x
				Side.Horizontal.MIDDLE -> (provider.createScaledResolution(mc).scaledWidth shr 1) - x
				Side.Horizontal.RIGHT -> provider.createScaledResolution(mc).scaledWidth - x
			}
		}

		height += 10
		realHeight += 10

		// Vertical
		valueFont.drawString("V:", x + 2, y + height, -1)
		valueFont.drawString(element.side.vertical.sideName, x + 12, y + height, Color.GRAY.rgb)

		if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10)
		{
			val values = Side.Vertical.values()
			val currIndex = values.indexOf(element.side.vertical)

			val y = element.renderY

			element.side.vertical = values[if (currIndex + 1 >= values.size) 0 else currIndex + 1]
			element.y = when (element.side.vertical)
			{
				Side.Vertical.UP -> y
				Side.Vertical.MIDDLE -> (provider.createScaledResolution(mc).scaledHeight shr 1) - y
				Side.Vertical.DOWN -> provider.createScaledResolution(mc).scaledHeight - y
			}
		}

		height += 10
		realHeight += 10

		// Values
		for (value in element.values) drawAbstractValue(valueFont, value, mouseX, mouseY, prevWidth)

		// Header
		RenderUtils.drawRect(x, y, x + width, y + 12, ClickGUI.generatePanelColor())
		getPanelFont().drawString("\u00A7l${element.name}", x + 2F, y + 3.5F, -1)

		// Delete button
		if (!element.info.force)
		{
			val deleteWidth = x + width - valueFont.getStringWidth("\u00A7lDelete") - 2F
			valueFont.drawString("\u00A7lDelete", deleteWidth, y + 3.5F, -1)
			if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= deleteWidth && mouseX <= x + width && mouseY >= y && mouseY <= y + 10) LiquidBounce.hud.removeElement(element)
		}
	}

	private fun drawAbstractValue(font: IFontRenderer, value: AbstractValue, mouseX: Int, mouseY: Int, prevWidth: Int, indent: Int = 0)
	{
		when (value)
		{
			is ValueGroup ->
			{
				val text = value.displayName
				val textWidth = font.getStringWidth(text) + indent + 16

				if (width < textWidth) width = textWidth
				val xIndent = x + indent
				val xEnd = x + prevWidth

				// Title
				font.drawString(text, xIndent + 2, y + height, -1)
				font.drawString(if (value.foldState) "-" else "+", xEnd - if (value.foldState) 5 else 6, y + height, -1)

				if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= xIndent && mouseX <= xEnd && mouseY >= y + height && mouseY <= y + height + 10)
				{
					value.foldState = !value.foldState
					mc.soundHandler.playSound("gui.button.press", 1.0f)
				}

				height += 10
				realHeight += 10

				if (value.foldState)
				{
					val cachedYPos = y + height
					val valuesInGroup = value.values.filter(AbstractValue::showCondition)
					var i = 0
					val j = valuesInGroup.size
					while (i < j)
					{
						val valueOfGroup = valuesInGroup[i]

						val textWidth2 = font.getStringWidth(valueOfGroup.displayName) + 12
						if (width < textWidth2) width = textWidth2
						glStateManager.resetColor()
						drawAbstractValue(font, valueOfGroup, mouseX, mouseY, prevWidth, indent + 10)

						if (i == j - 1) // Last Index
						{
							RenderUtils.drawRect(xIndent + 7, cachedYPos, xIndent + 8, y + height - 3, Int.MAX_VALUE)
							RenderUtils.drawRect(xIndent + 7, y + height - 3, xIndent + 12, y + height - 2, Int.MAX_VALUE)
						}
						i++
					}
				}
			}

			is ColorValue -> drawColorValue(font, value, mouseX, mouseY, prevWidth, indent)
			is RangeValue<*> -> drawRangeValue(font, value, mouseX, mouseY, prevWidth, indent)
			else -> drawValue(font, value as Value<*>, mouseX, mouseY, prevWidth, indent)
		}
	}

	private fun drawColorValue(font: IFontRenderer, value: ColorValue, mouseX: Int, mouseY: Int, prevWidth: Int, indent: Int = 0)
	{
		val xIndent = x + indent
		val xEnd = x + prevWidth
		val perc = prevWidth - indent - 18f

		val alphaPresent = value is RGBAColorValue
		val text = "${value.displayName}\u00A7f: \u00A7c${value.getRed()} \u00A7a${value.getGreen()} \u00A79${value.getBlue()}${if (alphaPresent) " \u00A77${value.getAlpha()}" else ""}\u00A7r "
		val colorText = "(#${if (alphaPresent) encodeToHex(value.getAlpha()) else ""}${encodeToHex(value.getRed())}${encodeToHex(value.getGreen())}${encodeToHex(value.getBlue())})"
		val displayTextWidth = font.getStringWidth(text)
		val textWidth = displayTextWidth + font.getStringWidth(colorText) + indent + 2

		val newSliderValue = (255 * WMathHelper.clamp_float((mouseX - (xIndent + 8f)) / perc, 0f, 1f)).toInt()

		if (width < textWidth + 20f) width = textWidth + 20

		font.drawString(text, xIndent + 2, y + height, -1)
		font.drawString(colorText, xIndent + displayTextWidth + 2, y + height, value.get(255))
		RenderUtils.drawRect(xIndent + textWidth, y + height, xEnd - 6, y + height + 6, value.get())

		RenderUtils.drawRect(xIndent + 8F, y + height + 12F, xEnd - 8F, y + height + 13F, Color.WHITE)

		val redSliderValue = (xIndent + perc * (value.getRed()) / 255) + 8
		RenderUtils.drawRect(redSliderValue, y + height + 9F, redSliderValue + 3F, y + height + 15F, Color.RED)

		if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= y + height + 9 && mouseY <= y + height + 15 && Mouse.isButtonDown(0)) value.set(newSliderValue, value.getGreen(), value.getBlue(), value.getAlpha())

		height += 10
		realHeight += 10

		RenderUtils.drawRect(xIndent + 8F, y + height + 12F, xEnd - 8F, y + height + 13F, Color.WHITE)

		val greenSliderValue = (xIndent + perc * (value.getGreen()) / 255) + 8
		RenderUtils.drawRect(greenSliderValue, y + height + 9F, greenSliderValue + 3F, y + height + 15F, Color.GREEN)

		if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= y + height + 9 && mouseY <= y + height + 15 && Mouse.isButtonDown(0)) value.set(value.getRed(), newSliderValue, value.getBlue(), value.getAlpha())

		height += 10
		realHeight += 10

		RenderUtils.drawRect(xIndent + 8F, y + height + 12F, xEnd - 8F, y + height + 13F, Color.WHITE)

		val blueSliderValue = (xIndent + perc * (value.getBlue()) / 255) + 8
		RenderUtils.drawRect(blueSliderValue, y + height + 9F, blueSliderValue + 3F, y + height + 15F, Color.BLUE)

		if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= y + height + 9 && mouseY <= y + height + 15 && Mouse.isButtonDown(0)) value.set(value.getRed(), value.getGreen(), newSliderValue, value.getAlpha())

		height += 10
		realHeight += 10

		if (alphaPresent)
		{
			RenderUtils.drawRect(xIndent + 8F, y + height + 12F, xEnd - 8F, y + height + 13F, Color.WHITE)

			val alphaSliderValue = (xIndent + perc * (value.getAlpha()) / 255) + 8
			RenderUtils.drawRect(alphaSliderValue, y + height + 9F, alphaSliderValue + 3F, y + height + 15F, Int.MAX_VALUE)

			if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= y + height + 9 && mouseY <= y + height + 15 && Mouse.isButtonDown(0)) value.set(value.getRed(), value.getGreen(), value.getBlue(), newSliderValue)

			height += 10
			realHeight += 10
		}

		height += 10
		realHeight += 10
	}

	private fun drawRangeValue(font: IFontRenderer, value: RangeValue<*>, mouseX: Int, mouseY: Int, prevWidth: Int, indent: Int = 0)
	{
		val xIndent = x + indent
		val xEnd = x + prevWidth
		val perc = prevWidth - indent - 18f

		val yPos = y + height

		when (value)
		{
			is IntegerRangeValue ->
			{
				val text = "${value.displayName}\u00A7f: \u00A7c${value.getMin()}-${value.getMax()}"
				val textWidth = font.getStringWidth(text) + indent + 8

				if (width < textWidth) width = textWidth

				// Slider
				RenderUtils.drawRect(xIndent + 8F, yPos + 12F, xEnd - 8F, yPos + 13F, Color.WHITE)

				// Minimum slider mark
				val minSliderValue = xIndent + perc * (value.getMin() - value.minimum) / (value.maximum - value.minimum) + 8f
				RenderUtils.drawRect(minSliderValue, yPos + 9F, minSliderValue + 3, yPos + 15F, -14319873)

				// Maximum slider mark
				val maxSliderValue = xIndent + perc * (value.getMax() - value.minimum) / (value.maximum - value.minimum) + 8f
				RenderUtils.drawRect(maxSliderValue, yPos + 9F, maxSliderValue + 3, yPos + 15F, -14319873)

				RenderUtils.drawRect(minSliderValue + 3, yPos + 12F, maxSliderValue, yPos + 13F, -14319873)

				val center = minSliderValue + (maxSliderValue - minSliderValue) * 0.5f

				if (mouseX >= x + 8 && mouseX <= xEnd && mouseY >= yPos + 9 && mouseY <= yPos + 15 && Mouse.isButtonDown(0))
				{
					val newValue = (value.minimum + (value.maximum - value.minimum) * WMathHelper.clamp_float((mouseX - (xIndent + 8f)) / perc, 0f, 1f)).toInt()
					if (mouseX > center) value.setMax(newValue)
					else value.setMin(newValue)
				}

				glStateManager.resetColor()
				font.drawString(text, xIndent + 2, yPos, -1)

				height += 20
				realHeight += 20
			}

			is FloatRangeValue ->
			{
				val text = "${value.displayName}\u00A7f: \u00A7c${StringUtils.DECIMALFORMAT_2.format(value.getMin())}-${StringUtils.DECIMALFORMAT_2.format(value.getMax())}"
				val textWidth = font.getStringWidth(text) + indent + 8

				if (width < textWidth) width = textWidth

				// Slider
				RenderUtils.drawRect(xIndent + 8F, yPos + 12F, xEnd - 8F, yPos + 13F, Color.WHITE)

				// Minimum slider mark
				val minSliderValue = xIndent + perc * (value.getMin() - value.minimum) / (value.maximum - value.minimum) + 8f
				RenderUtils.drawRect(minSliderValue, yPos + 9F, minSliderValue + 3, yPos + 15F, -14319873)

				// Maximum slider mark
				val maxSliderValue = xIndent + perc * (value.getMax() - value.minimum) / (value.maximum - value.minimum) + 8f
				RenderUtils.drawRect(maxSliderValue, yPos + 9F, maxSliderValue + 3, yPos + 15F, -14319873)

				RenderUtils.drawRect(minSliderValue + 3, yPos + 12F, maxSliderValue, yPos + 13F, -14319873)

				val center = minSliderValue + (maxSliderValue - minSliderValue) * 0.5f

				if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= yPos + 9 && mouseY <= yPos + 15 && Mouse.isButtonDown(0))
				{
					val newValue = round((value.minimum + (value.maximum - value.minimum) * WMathHelper.clamp_double(((mouseX - (xIndent + 8f)) / perc).toDouble(), 0.0, 1.0)).toFloat())
					if (mouseX > center) value.setMax(newValue.toFloat())
					else value.setMin(newValue.toFloat())
				}

				glStateManager.resetColor()

				font.drawString(text, xIndent + 2, yPos, -1)

				height += 20
				realHeight += 20
			}
		}
	}

	private fun drawValue(font: IFontRenderer, value: Value<*>, mouseX: Int, mouseY: Int, prevWidth: Int, indent: Int = 0)
	{
		val xIndent = x + indent
		val xEnd = x + prevWidth
		val perc = prevWidth - indent - 18F

		val yPos = y + height

		when (value)
		{
			is BoolValue ->
			{
				val text = value.displayName
				val textWidth = font.getStringWidth(text) + 8

				// Title
				font.drawString(text, xIndent + 2, yPos, if (value.get()) -1 else Color.GRAY.rgb)

				if (width < textWidth) width = textWidth

				// Toggle value
				if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= xIndent && mouseX <= x + width && mouseY >= yPos && mouseY <= yPos + 10) value.set(!value.get())

				// Change pos
				height += 10
				realHeight += 10
			}

			is IntegerValue ->
			{
				val current = value.get()
				val min = value.minimum
				val max = value.maximum

				// Title
				val text = "${value.displayName}: \u00A7c$current"
				val textWidth = font.getStringWidth(text)

				if (width < textWidth + 8) width = textWidth + 8

				font.drawString(text, xIndent + 2, yPos, -1)

				// Slider
				RenderUtils.drawRect(xIndent + 8F, yPos + 12F, xEnd - 8F, yPos + 13F, Color.WHITE)

				// Slider mark
				val sliderValue = xIndent + (perc * (current - min) / (max - min)) + 8f
				RenderUtils.drawRect(sliderValue, yPos + 9F, sliderValue + 3f, yPos + 15F, -14319873)

				// Slider changer
				if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= yPos + 9 && mouseY <= yPos + 15 && Mouse.isButtonDown(0)) value.set((min + (max - min) * WMathHelper.clamp_float((mouseX - (xIndent + 8F)) / perc, 0F, 1F)).toInt())

				// Change pos
				height += 20
				realHeight += 20
			}

			is FloatValue ->
			{
				val current = value.get()
				val min = value.minimum
				val max = value.maximum

				// Title
				val text = "${value.displayName}: \u00A7c${"%.2f".format(current)}"
				val textWidth = font.getStringWidth(text)

				font.drawString(text, xIndent + 2, yPos, -1)

				if (width < textWidth + 8) width = textWidth + 8

				// Slider
				RenderUtils.drawRect(xIndent + 8F, yPos + 12F, xEnd - 8F, yPos + 13F, -1)

				// Slider mark
				val sliderValue = xIndent + (perc * (current - min) / (max - min)) + 8f
				RenderUtils.drawRect(sliderValue, yPos + 9F, sliderValue + 3F, yPos + 15F, Color(37, 126, 255).rgb)

				// Slider changer
				if (mouseX >= xIndent + 8 && mouseX <= xEnd && mouseY >= yPos + 9 && mouseY <= yPos + 15 && Mouse.isButtonDown(0))
				{
					val curr = WMathHelper.clamp_float((mouseX - (xIndent + 8f)) / perc, 0F, 1F)

					value.set(min + (max - min) * curr)
				}

				// Change pos
				height += 20
				realHeight += 20
			}

			is ListValue ->
			{
				// Title
				font.drawString(value.displayName, xIndent + 2, yPos, -1)

				height += 10
				realHeight += 10

				// Selectable values
				for (s in value.values)
				{
					// Value title
					val text = "\u00A7c> \u00A7r$s"
					font.drawString(text, xIndent + 2, y + height, if (s == value.get()) -1 else Color.GRAY.rgb)

					val stringWidth = font.getStringWidth(text) + 8
					if (width < stringWidth) width = stringWidth

					// Select value
					if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= xIndent && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10) value.set(s)

					// Change pos
					height += 10
					realHeight += 10
				}
			}

			is FontValue ->
			{
				val fontRenderer = value.get()

				// Title
				val text = when
				{
					fontRenderer.isGameFontRenderer() -> "${value.displayName}: ${fontRenderer.getGameFontRenderer().defaultFont.font.name} - ${fontRenderer.getGameFontRenderer().defaultFont.font.size}"
					fontRenderer == Fonts.minecraftFont -> "${value.displayName}: Minecraft"
					else -> "${value.displayName}: Unknown"
				}

				font.drawString(text, xIndent + 2, yPos, -1)

				val stringWidth = font.getStringWidth(text) + 8
				if (width < stringWidth) width = stringWidth

				if (Mouse.isButtonDown(0) && !mouseDown && mouseX >= xIndent && mouseX <= x + width && mouseY >= yPos && mouseY <= yPos + 10) Fonts.fonts.filter { it == fontRenderer }.forEachIndexed { index, _ -> value.set(Fonts.fonts[if (index + 1 >= Fonts.fonts.size) 0 else index + 1]) }

				height += 10
				realHeight += 10
			}
		}
	}

	/**
	 * Drag panel
	 */
	private fun drag(mouseX: Int, mouseY: Int)
	{
		if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 12 && Mouse.isButtonDown(0) && !mouseDown)
		{
			drag = true
			dragX = mouseX - x
			dragY = mouseY - y
		}

		if (Mouse.isButtonDown(0) && drag)
		{
			x = mouseX - dragX
			y = mouseY - dragY
		}
		else drag = false
	}

	companion object
	{
		private fun encodeToHex(hex: Int) = hex.toString(16).toUpperCase().padStart(2, '0')

		private fun round(f: Float): BigDecimal = BigDecimal("$f").setScale(2, RoundingMode.HALF_UP)
	}
}
