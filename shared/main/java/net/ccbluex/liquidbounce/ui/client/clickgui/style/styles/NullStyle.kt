/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.clamp_double
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generateColor
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.misc.StringUtils.stripControlCodes
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.value.*
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Mouse
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode

@SideOnly(Side.CLIENT)
class NullStyle : Style()
{
	private var mouseDown = false
	private var rightMouseDown = false

	override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
	{
		val xF = panel.x.toFloat()
		val yF = panel.y.toFloat()

		drawRect(xF - 3, yF, xF + panel.width + 3, yF + 19, generateColor().rgb)

		if (panel.fade > 0) drawBorderedRect(xF, yF + 19, xF + panel.width, panel.y + 19f + panel.fade, 1f, Int.MIN_VALUE, Int.MIN_VALUE)

		classProvider.glStateManager.resetColor()

		val textWidth = Fonts.font35.getStringWidth("\u00A7f" + stripControlCodes(panel.name))
		Fonts.font35.drawString("\u00A7f" + panel.name, (panel.x - (textWidth - 100.0f) * 0.5f).toInt(), panel.y + 7, Int.MAX_VALUE)
	}

	override fun drawDescription(mouseX: Int, mouseY: Int, text: String)
	{
		val textWidth = Fonts.font35.getStringWidth(text)
		drawRect(mouseX + 9, mouseY, mouseX + textWidth + 14, mouseY + Fonts.font35.fontHeight + 3, generateColor().rgb)

		classProvider.glStateManager.resetColor()

		Fonts.font35.drawString(text, mouseX + 12, mouseY + (Fonts.font35.fontHeight shr 1), Int.MAX_VALUE)
	}

	override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
	{
		classProvider.glStateManager.resetColor()
		Fonts.font35.drawString(buttonElement.displayName, (buttonElement.x - (Fonts.font35.getStringWidth(buttonElement.displayName) - 100.0f) * 0.5f).toInt(), buttonElement.y + 6, buttonElement.color)
	}

	override fun drawModuleElement(mouseX: Int, mouseY: Int, moduleElement: ModuleElement)
	{
		val guiColor = generateColor().rgb
		val glStateManager = classProvider.glStateManager

		glStateManager.resetColor()

		Fonts.font35.drawString(moduleElement.displayName, (moduleElement.x - (Fonts.font35.getStringWidth(moduleElement.displayName) - 100.0f) * 0.5f).toInt(), moduleElement.y + 6, if (moduleElement.module.state) guiColor else Int.MAX_VALUE)

		val moduleValues = moduleElement.module.values

		if (moduleValues.isNotEmpty())
		{
			Fonts.font35.drawString("+", moduleElement.x + moduleElement.width - 8, moduleElement.y + (moduleElement.height shr 1), Color.WHITE.rgb)

			if (moduleElement.isShowSettings)
			{
				var yPos = moduleElement.y + 4

				for (value in moduleValues)
				{
					val isNumber = value.get() is Number

					if (isNumber) assumeNonVolatile = false

					if (value is BoolValue)
					{
						val text = value.name
						val textWidth = Fonts.font35.getStringWidth(text)

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8f

						drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 14f, Int.MIN_VALUE)

						if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= yPos + 2 && mouseY <= yPos + 14) if (Mouse.isButtonDown(0) && moduleElement.isntPressed())
						{

							value.set(!value.get())
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}

						glStateManager.resetColor()

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, yPos + 4, if (value.get()) guiColor else Int.MAX_VALUE)

						yPos += 12
					}
					else if (value is ListValue)
					{
						val text = value.name
						val textWidth = Fonts.font35.getStringWidth(text)

						if (moduleElement.settingsWidth < textWidth + 16) moduleElement.settingsWidth = textWidth + 16f

						drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 14f, Int.MIN_VALUE)

						glStateManager.resetColor()

						Fonts.font35.drawString("\u00A7c$text", moduleElement.x + moduleElement.width + 6, yPos + 4, 0xffffff)
						Fonts.font35.drawString(if (value.openList) "-" else "+", (moduleElement.x + moduleElement.width + moduleElement.settingsWidth - if (value.openList) 5 else 6).toInt(), yPos + 4, 0xffffff)

						if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= yPos + 2 && mouseY <= yPos + 14) if (Mouse.isButtonDown(0) && moduleElement.isntPressed())
						{
							value.openList = !value.openList
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}

						yPos += 12

						for (valueOfList in value.values)
						{
							val textWidth2 = Fonts.font35.getStringWidth(">$valueOfList")

							if (moduleElement.settingsWidth < textWidth2 + 12) moduleElement.settingsWidth = textWidth2 + 12f

							if (value.openList)
							{
								drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 14f, Int.MIN_VALUE)

								if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= yPos + 2 && mouseY <= yPos + 14) if (Mouse.isButtonDown(0) && moduleElement.isntPressed())
								{
									value.set(valueOfList)
									mc.soundHandler.playSound("gui.button.press", 1.0f)
								}

								glStateManager.resetColor()

								Fonts.font35.drawString(">", moduleElement.x + moduleElement.width + 6, yPos + 4, Int.MAX_VALUE)
								Fonts.font35.drawString(valueOfList, moduleElement.x + moduleElement.width + 14, yPos + 4, if (value.get().equals(valueOfList, ignoreCase = true)) guiColor else Int.MAX_VALUE)

								yPos += 12
							}
						}
					}
					else if (value is FloatValue)
					{
						val text = value.name + "\u00A7f: \u00A7c" + round(value.get())
						val textWidth = Fonts.font35.getStringWidth(text)

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8f

						drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 24f, Int.MIN_VALUE)
						drawRect(moduleElement.x + moduleElement.width + 8f, yPos + 18f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth - 4, yPos + 19f, Int.MAX_VALUE)

						val sliderValue = moduleElement.x + moduleElement.width + (moduleElement.settingsWidth - 12) * (value.get() - value.minimum) / (value.maximum - value.minimum)
						drawRect(8 + sliderValue, yPos + 15f, sliderValue + 11, yPos + 21f, guiColor)

						if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth - 4 && mouseY >= yPos + 15 && mouseY <= yPos + 21) if (Mouse.isButtonDown(0))
						{
							val d = clamp_double(((mouseX - moduleElement.x - moduleElement.width - 8) / (moduleElement.settingsWidth - 12)).toDouble(), 0.0, 1.0)

							value.set(round((value.minimum + (value.maximum - value.minimum) * d).toFloat()).toFloat())
						}

						glStateManager.resetColor()

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, yPos + 4, 0xffffff)

						yPos += 22
					}
					else if (value is IntegerValue)
					{
						val text = value.name + "\u00A7f: \u00A7c" + if (value is BlockValue) getBlockName(value.get()) + " (" + value.get() + ")" else value.get()
						val textWidth = Fonts.font35.getStringWidth(text)

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8f

						drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 24f, Int.MIN_VALUE)
						drawRect(moduleElement.x + moduleElement.width + 8f, yPos + 18f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth - 4, yPos + 19f, Int.MAX_VALUE)

						val sliderValue = moduleElement.x + moduleElement.width + (moduleElement.settingsWidth - 12) * (value.get() - value.minimum) / (value.maximum - value.minimum)
						drawRect(8 + sliderValue, yPos + 15f, sliderValue + 11, yPos + 21f, guiColor)

						if (mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= yPos + 15 && mouseY <= yPos + 21) if (Mouse.isButtonDown(0))
						{
							val d = clamp_double(((mouseX - moduleElement.x - moduleElement.width - 8) / (moduleElement.settingsWidth - 12)).toDouble(), 0.0, 1.0)

							value.set((value.minimum + (value.maximum - value.minimum) * d).toInt())
						}

						glStateManager.resetColor()

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, yPos + 4, 0xffffff)

						yPos += 22
					}
					else if (value is FontValue)
					{
						val fontRenderer = value.get()

						drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 14f, Int.MIN_VALUE)

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

						Fonts.font35.drawString(displayString, moduleElement.x + moduleElement.width + 6, yPos + 4, Color.WHITE.rgb)

						val stringWidth = Fonts.font35.getStringWidth(displayString)

						if (moduleElement.settingsWidth < stringWidth + 8) moduleElement.settingsWidth = stringWidth + 8f

						if ((Mouse.isButtonDown(0) && !mouseDown || Mouse.isButtonDown(1) && !rightMouseDown) && mouseX >= moduleElement.x + moduleElement.width + 4 && mouseX <= moduleElement.x + moduleElement.width + moduleElement.settingsWidth && mouseY >= yPos + 4 && mouseY <= yPos + 12)
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
					else
					{
						val text = value.name + "\u00A7f: \u00A7c" + value.get()
						val textWidth = Fonts.font35.getStringWidth(text)

						if (moduleElement.settingsWidth < textWidth + 8) moduleElement.settingsWidth = textWidth + 8f

						drawRect(moduleElement.x + moduleElement.width + 4f, yPos + 2f, moduleElement.x + moduleElement.width + moduleElement.settingsWidth, yPos + 14f, Int.MIN_VALUE)

						glStateManager.resetColor()

						Fonts.font35.drawString(text, moduleElement.x + moduleElement.width + 6, yPos + 4, 0xffffff)

						yPos += 12
					}

					// This state is cleaned up in ClickGUI
					if (isNumber) assumeNonVolatile = true
				}

				moduleElement.updatePressed()

				mouseDown = Mouse.isButtonDown(0)
				rightMouseDown = Mouse.isButtonDown(1)

				if (moduleElement.settingsWidth > 0.0f && yPos > moduleElement.y + 4) drawBorderedRect((moduleElement.x + moduleElement.width + 4).toFloat(), (moduleElement.y + 6).toFloat(), moduleElement.x + moduleElement.width + moduleElement.settingsWidth, (yPos + 2).toFloat(), 1.0f, Int.MIN_VALUE, 0)
			}
		}
	}

	companion object
	{
		private fun round(f: Float): BigDecimal = BigDecimal("$f").setScale(2, RoundingMode.HALF_UP) // TODO: Should change scale to 3?
	}
}
