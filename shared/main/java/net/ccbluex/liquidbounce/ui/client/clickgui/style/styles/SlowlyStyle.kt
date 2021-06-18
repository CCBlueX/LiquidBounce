/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.clamp_double
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.misc.StringUtils.stripControlCodes
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.value.*
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Mouse
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode

@SideOnly(Side.CLIENT)
class SlowlyStyle : Style()
{
	private var mouseDown = false
	private var rightMouseDown = false

	override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
	{
		drawBorderedRect(panel.x.toFloat(), panel.y.toFloat() - 3, panel.x.toFloat() + panel.width, panel.y.toFloat() + 17, 3f, -14010033, -14010033)

		if (panel.fade > 0)
		{
			drawBorderedRect(panel.x.toFloat(), panel.y.toFloat() + 17, panel.x.toFloat() + panel.width, (panel.y + 19 + panel.fade), 3f, -13220000, -13220000)
			drawBorderedRect(panel.x.toFloat(), (panel.y + 17 + panel.fade), panel.x.toFloat() + panel.width, (panel.y + 19 + panel.fade + 5), 3f, -14010033, -14010033)
		}

		classProvider.glStateManager.resetColor()

		val textWidth = Fonts.font35.getStringWidth("\u00A7f" + stripControlCodes(panel.name)).toFloat()
		Fonts.font35.drawString(panel.name, (panel.x - (textWidth - 100.0f) * 0.5).toInt(), panel.y + 7 - 3, -1)
	}

	override fun drawDescription(mouseX: Int, mouseY: Int, text: String)
	{
		val textWidth = Fonts.font35.getStringWidth(text)
		drawBorderedRect((mouseX + 9).toFloat(), mouseY.toFloat(), (mouseX + textWidth + 14).toFloat(), (mouseY + Fonts.font35.fontHeight + 3).toFloat(), 3.0f, -14010033, -14010033)

		classProvider.glStateManager.resetColor()

		Fonts.font35.drawString(text, mouseX + 12, mouseY + (Fonts.font35.fontHeight shr 1), -1)
	}

	override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
	{
		drawRect(buttonElement.x - 1, buttonElement.y - 1, buttonElement.x + buttonElement.width + 1, buttonElement.y + buttonElement.height + 1, hoverColor(if (buttonElement.color == Int.MAX_VALUE) Color(54, 71, 96) else Color(7, 152, 252), buttonElement.hoverTime).rgb)

		classProvider.glStateManager.resetColor()

		Fonts.font35.drawString(buttonElement.displayName, buttonElement.x + 5, buttonElement.y + 5, -1)
	}

	/*
	 * public static boolean drawCheckbox(final boolean value, final int x, final int y, final int mouseX, final int mouseY, final Color color) { RenderUtils.drawRect(x, y, x + 20, y + 10, value ? Color.GREEN : Color.RED); RenderUtils.drawFilledCircle(x +
	 * (value ? 15 : 5),y + 5, 5, Color.WHITE);
	 * 
	 * if(mouseX >= x && mouseX <= x + 20 && mouseY >= y && mouseY <= y + 10 && Mouse.isButtonDown(0)) return !value;
	 * 
	 * return value; }
	 */

	override fun drawModuleElement(mouseX: Int, mouseY: Int, moduleElement: ModuleElement)
	{
		drawRect(moduleElement.x - 1, moduleElement.y - 1, moduleElement.x + moduleElement.width + 1, moduleElement.y + moduleElement.height + 1, hoverColor(Color(54, 71, 96), moduleElement.hoverTime).rgb)
		drawRect(moduleElement.x - 1, moduleElement.y - 1, moduleElement.x + moduleElement.width + 1, moduleElement.y + moduleElement.height + 1, hoverColor(Color(7, 152, 252, moduleElement.slowlyFade), moduleElement.hoverTime).rgb)

		val glStateManager = classProvider.glStateManager
		glStateManager.resetColor()

		Fonts.font35.drawString(moduleElement.displayName, moduleElement.x + 5, moduleElement.y + 5, -1)

		// Draw settings
		val moduleValues = moduleElement.module.values
		if (moduleValues.isNotEmpty())
		{
			Fonts.font35.drawString(">", moduleElement.x + moduleElement.width - 8, moduleElement.y + 5, -1)

			if (moduleElement.isShowSettings)
			{
				if (moduleElement.settingsWidth > 0.0f && moduleElement.slowlySettingsYPos > moduleElement.y + 6) drawBorderedRect((moduleElement.x + moduleElement.width + 4).toFloat(), (moduleElement.y + 6).toFloat(), moduleElement.x + moduleElement.width + moduleElement.settingsWidth, (moduleElement.slowlySettingsYPos + 2).toFloat(), 3.0f, -13220000, -13220000)
				moduleElement.slowlySettingsYPos = moduleElement.y + 6

				for (value in moduleValues)
				{
					val isNumber = value.get() is Number

					if (isNumber) assumeNonVolatile = false

					if (value is BoolValue)
					{
						val text = value.name
						val textWidth = Fonts.font35.getStringWidth(text).toFloat()

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8

						if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + 12 && Mouse.isButtonDown(0) && moduleElement.isntPressed())
						{
							value.set(!value.get())
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 2, if (value.get()) -1 else Int.MAX_VALUE)

						moduleElement.slowlySettingsYPos += 11
					}
					else if (value is ListValue)
					{
						val text = value.name
						val textWidth = Fonts.font35.getStringWidth(text).toFloat()

						if (moduleElement.settingsWidth < textWidth + 16) moduleElement.settingsWidth = textWidth + 16

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 2, 0xffffff)
						Fonts.font35.drawString(if (value.openList) "-" else "+", (moduleElement.x + moduleElement.width + moduleElement.settingsWidth - if (value.openList) 5 else 6).toInt(), moduleElement.slowlySettingsYPos + 2, 0xffffff)

						if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + Fonts.font35.fontHeight && Mouse.isButtonDown(0) && moduleElement.isntPressed())
						{
							value.openList = !value.openList
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}

						moduleElement.slowlySettingsYPos += Fonts.font35.fontHeight + 1

						for (valueOfList in value.values)
						{
							val textWidth2 = Fonts.font35.getStringWidth("> $valueOfList").toFloat()

							if (moduleElement.settingsWidth < textWidth2 + 12) moduleElement.settingsWidth = textWidth2 + 12

							if (value.openList)
							{
								if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos + 2 && mouseY <= moduleElement.slowlySettingsYPos + 14 && Mouse.isButtonDown(0) && moduleElement.isntPressed())
								{
									value.set(valueOfList)
									mc.soundHandler.playSound("gui.button.press", 1.0f)
								}
								glStateManager.resetColor()
								Fonts.font35.drawString("> $valueOfList", moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 2, if (value.get().equals(valueOfList, ignoreCase = true)) -1 else Int.MAX_VALUE)
								moduleElement.slowlySettingsYPos += Fonts.font35.fontHeight + 1
							}
						}

						if (!value.openList) moduleElement.slowlySettingsYPos += 1
					}
					else if (value is FloatValue)
					{
						val text = value.name + "\u00A7f: " + round(value.get())
						val textWidth = Fonts.font35.getStringWidth(text).toFloat()

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8

						val valueOfSlide = drawSlider(value.get(), value.minimum, value.maximum, moduleElement.x + moduleElement.width + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, -16279300 /* 0xFF0798FC */)

						if (valueOfSlide != value.get()) value.set(valueOfSlide)

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 3, 0xffffff)

						moduleElement.slowlySettingsYPos += 19
					}
					else if (value is IntegerValue)
					{
						val text = value.name + "\u00A7f: " + if (value is BlockValue) getBlockName(value.get()) + " (" + value.get() + ")" else value.get()
						val textWidth = Fonts.font35.getStringWidth(text).toFloat()

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8

						val valueOfSlide = drawSlider(value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), moduleElement.x + moduleElement.width + 8, moduleElement.slowlySettingsYPos + 14, moduleElement.settingsWidth.toInt() - 12, mouseX, mouseY, -16279300 /* 0xFF0798FC */)

						if (valueOfSlide != value.get().toFloat()) value.set(valueOfSlide.toInt())

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 3, 0xffffff)

						moduleElement.slowlySettingsYPos += 19
					}
					else if (value is FontValue)
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

						Fonts.font35.drawString(displayString, moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 2, -1)

						val stringWidth = Fonts.font35.getStringWidth(displayString)

						if (moduleElement.settingsWidth < stringWidth + 8) moduleElement.settingsWidth = (stringWidth + 8).toFloat()

						if ((Mouse.isButtonDown(0) && !mouseDown || Mouse.isButtonDown(1) && !rightMouseDown) && mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + 12)
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

						moduleElement.slowlySettingsYPos += 11
					}
					else
					{
						val text = value.name + "\u00A7f: " + value.get()
						val textWidth = Fonts.font35.getStringWidth(text).toFloat()

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8

						glStateManager.resetColor()

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, moduleElement.slowlySettingsYPos + 4, 0xffffff)

						moduleElement.slowlySettingsYPos += 12
					}

					if (isNumber) assumeNonVolatile = true
				}

				moduleElement.updatePressed()

				mouseDown = Mouse.isButtonDown(0)
				rightMouseDown = Mouse.isButtonDown(1)
			}
		}
	}

	companion object
	{
		fun drawSlider(value: Float, min: Float, max: Float, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, color: Int): Float
		{
			val displayValue = value.coerceIn(min, max)

			drawRect(x, y, x + width, y + 2, Int.MAX_VALUE)

			val sliderValue = x + width * (displayValue - min) / (max - min)

			drawRect(x.toFloat(), y.toFloat(), sliderValue, (y + 2).toFloat(), color)
			drawFilledCircle(sliderValue.toInt(), y + 1, 3f, color)

			if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 3 && Mouse.isButtonDown(0))
			{
				val d = clamp_double((mouseX.toDouble() - x) / (width.toDouble() - 3), 0.0, 1.0)
				var bigDecimal = BigDecimal("${(min + (max - min) * d)}")

				bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP)

				return bigDecimal.toFloat()
			}

			return value
		}

		private fun round(v: Float): BigDecimal = BigDecimal("$v").setScale(2, RoundingMode.HALF_UP)

		private fun hoverColor(color: Color, hover: Int): Color
		{
			val red = color.red - (hover shl 1)
			val green = color.green - (hover shl 1)
			val blue = color.blue - (hover shl 1)
			return Color(red.coerceAtLeast(0), green.coerceAtLeast(0), blue.coerceAtLeast(0), color.alpha)
		}
	}
}
