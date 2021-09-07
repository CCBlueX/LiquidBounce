/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard

@ElementInfo(name = "TabGUI")
class TabGUI(x: Double = 5.0, y: Double = 25.0) : Element(x = x, y = y)
{
	private val rectGroup = ValueGroup("Rect")
	private val rectRainbowValue = BoolValue("Rainbow", false, "Rectangle Rainbow")
	private val rectColorValue = RGBAColorValue("Color", 0, 148, 255, 140, listOf("Rectangle Red", "Rectangle Green", "Rectangle Blue", "Rectangle Alpha"))

	private val backgroundColorValue = RGBAColorValue("BackgroundColor", 0, 0, 0, 150, listOf("Background Red", "Background Green", "Background Blue", "Background Alpha"))

	private val borderGroup = ValueGroup("Border")
	private val borderEnabledValue = BoolValue("Enabled", true, "Border")
	private val borderStrengthValue = FloatValue("Strength", 2F, 1F, 5F, "Border Strength")
	private val borderColorValue = RGBAColorValue("Color", 0, 0, 0, 150, listOf("Border Red", "Border Green", "Border Blue", "Border Alpha"))
	private val borderRainbow = BoolValue("Rainbow", false, "Border Rainbow")

	private val rainbowShaderGroup = ValueGroup("RainbowShader")
	private val rainbowShaderX = FloatValue("X", -1000F, -2000F, 2000F, "Rainbow-X")
	private val rainbowShaderY = FloatValue("Y", -1000F, -2000F, 2000F, "Rainbow-Y")

	private val width = FloatValue("Width", 60F, 55F, 100F)

	private val arrowsValue = BoolValue("Arrows", true)
	private val textShadow = BoolValue("TextShadow", false)

	private val textFade = BoolValue("TextFade", true)
	private val textPositionY = FloatValue("TextPosition-Y", 2F, 0F, 5F)

	private val tabHeight = FloatValue("TabHeight", 12F, 10F, 15F)
	private val upperCaseValue = BoolValue("UpperCase", false)

	private val fontValue = FontValue("Font", Fonts.font35)

	private val tabs = mutableListOf<Tab>()

	private var categoryMenu = true
	private var selectedCategory = 0
	private var selectedModule = 0

	private var tabY = 0F
	private var itemY = 0F

	init
	{
		rectGroup.addAll(rectRainbowValue, rectColorValue)
		borderGroup.addAll(borderEnabledValue, borderStrengthValue, borderColorValue, borderRainbow)
		rainbowShaderGroup.addAll(rainbowShaderX, rainbowShaderY)

		for (category in ModuleCategory.values())
		{
			val tab = Tab(category.displayName)
			LiquidBounce.moduleManager.modules.filter { category == it.category }.forEach { tab.modules.add(it) }
			tabs.add(tab)
		}
	}

	override fun drawElement(): Border
	{
		updateAnimation()

		val fontRenderer = fontValue.get()

		val rectangleRainbowEnabled = rectRainbowValue.get()

		val backgroundColor = backgroundColorValue.get()
		val borderColor = if (borderRainbow.get()) -16777216 else borderColorValue.get()

		val width = width.get()
		val guiHeight = tabs.size * tabHeight.get()

		assumeNonVolatile {
			// Draw
			RenderUtils.drawRect(1F, 0F, width, guiHeight, backgroundColor)

			val rainbowShaderX = if (rainbowShaderX.get() == 0.0F) 0.0F else 1.0F / rainbowShaderX.get()
			val rainbowShaderY = if (rainbowShaderY.get() == 0.0F) 0.0F else 1.0F / rainbowShaderY.get()
			val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

			if (borderEnabledValue.get())
			{
				RainbowShader.begin(borderRainbow.get(), rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					RenderUtils.drawBorder(1F, 0F, width, guiHeight, borderStrengthValue.get(), borderColor)
				}
			}

			// Color
			val rectColor = if (rectangleRainbowEnabled) -16777216 else rectColorValue.get()

			RainbowShader.begin(rectangleRainbowEnabled, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
				RenderUtils.drawRect(1F, 1 + tabY - 1, width, tabY + tabHeight.get(), rectColor)
			}

			RenderUtils.resetColor()

			var y = 1F
			tabs.forEachIndexed { index, tab ->
				val tabName = if (upperCaseValue.get()) tab.tabName.toUpperCase()
				else tab.tabName

				val textX = if (side.horizontal == Side.Horizontal.RIGHT) width - fontRenderer.getStringWidth(tabName) - tab.textFade - 3
				else tab.textFade + 5
				val textY = y + textPositionY.get()

				val textColor = if (selectedCategory == index) 0xffffff else 13816530

				fontRenderer.drawString(tabName, textX, textY, textColor, textShadow.get())

				if (arrowsValue.get())
				{
					if (side.horizontal == Side.Horizontal.RIGHT) fontRenderer.drawString(if (!categoryMenu && selectedCategory == index) ">" else "<", 3F, y + 2F, 0xffffff, textShadow.get())
					else fontRenderer.drawString(if (!categoryMenu && selectedCategory == index) "<" else ">", width - 8F, y + 2F, 0xffffff, textShadow.get())
				}

				if (index == selectedCategory && !categoryMenu)
				{
					val tabX = if (side.horizontal == Side.Horizontal.RIGHT) 1F - tab.menuWidth
					else width + 5

					tab.drawTab(tabX, y, rectColor, backgroundColor, borderColor, borderStrengthValue.get(), upperCaseValue.get(), fontRenderer, borderRainbow.get(), rectangleRainbowEnabled)
				}

				y += tabHeight.get()
			}
		}

		return Border(1F, 0F, width, guiHeight)
	}

	override fun handleKey(c: Char, keyCode: Int)
	{
		val horizontal = side.horizontal

		when (keyCode)
		{
			Keyboard.KEY_UP -> parseAction(Action.UP)
			Keyboard.KEY_DOWN -> parseAction(Action.DOWN)
			Keyboard.KEY_RIGHT -> parseAction(if (horizontal == Side.Horizontal.RIGHT) Action.LEFT else Action.RIGHT)
			Keyboard.KEY_LEFT -> parseAction(if (horizontal == Side.Horizontal.RIGHT) Action.RIGHT else Action.LEFT)
			Keyboard.KEY_RETURN -> parseAction(Action.TOGGLE)
		}
	}

	private fun updateAnimation()
	{
		val delta = RenderUtils.deltaTime

		val xPos = tabHeight.get() * selectedCategory
		if (tabY.toInt() != xPos.toInt())
		{
			if (xPos > tabY) tabY += 0.1F * delta
			else tabY -= 0.1F * delta
		}
		else tabY = xPos

		val xPos2 = tabHeight.get() * selectedModule

		if (itemY.toInt() != xPos2.toInt())
		{
			if (xPos2 > itemY) itemY += 0.1F * delta
			else itemY -= 0.1F * delta
		}
		else itemY = xPos2

		if (categoryMenu) itemY = 0F

		if (textFade.get())
		{
			tabs.forEachIndexed { index, tab ->
				if (index == selectedCategory)
				{
					if (tab.textFade < 4) tab.textFade += 0.05F * delta

					if (tab.textFade > 4) tab.textFade = 4F
				}
				else
				{
					if (tab.textFade > 0) tab.textFade -= 0.05F * delta

					if (tab.textFade < 0) tab.textFade = 0F
				}
			}
		}
		else for (tab in tabs)
		{
			if (tab.textFade > 0) tab.textFade -= 0.05F * delta
			if (tab.textFade < 0) tab.textFade = 0F
		}
	}

	private fun parseAction(action: Action)
	{
		var toggle = false

		when (action)
		{
			Action.UP -> if (categoryMenu)
			{
				--selectedCategory

				if (selectedCategory < 0)
				{
					selectedCategory = tabs.size - 1
					tabY = tabHeight.get() * selectedCategory.toFloat()
				}
			}
			else
			{
				--selectedModule

				if (selectedModule < 0)
				{
					selectedModule = tabs[selectedCategory].modules.size - 1
					itemY = tabHeight.get() * selectedModule.toFloat()
				}
			}

			Action.DOWN -> if (categoryMenu)
			{
				++selectedCategory

				if (selectedCategory > tabs.size - 1)
				{
					selectedCategory = 0
					tabY = tabHeight.get() * selectedCategory.toFloat()
				}
			}
			else
			{
				++selectedModule

				if (selectedModule > tabs[selectedCategory].modules.size - 1)
				{
					selectedModule = 0
					itemY = tabHeight.get() * selectedModule.toFloat()
				}
			}

			Action.LEFT -> if (!categoryMenu) categoryMenu = true

			Action.RIGHT -> if (!categoryMenu) toggle = true
			else
			{
				categoryMenu = false
				selectedModule = 0
			}

			Action.TOGGLE -> if (!categoryMenu) toggle = true
		}

		if (toggle)
		{
			val sel = selectedModule
			tabs[selectedCategory].modules[sel].toggle()
		}
	}

	/**
	 * TabGUI Tab
	 */
	private inner class Tab(val tabName: String)
	{

		val modules = mutableListOf<Module>()
		var menuWidth = 0
		var textFade = 0F

		fun drawTab(x: Float, y: Float, color: Int, backgroundColor: Int, borderColor: Int, borderStrength: Float, upperCase: Boolean, fontRenderer: IFontRenderer, borderRainbow: Boolean, rectRainbow: Boolean)
		{
			menuWidth = modules.map { fontRenderer.getStringWidth(if (upperCase) it.name.toUpperCase() else it.name) }.max()?.plus(7) ?: 10

			val tabHeight = tabHeight.get()
			val menuHeight = modules.size * tabHeight

			val rainbowShaderX = if (rainbowShaderX.get() == 0.0F) 0.0F else 1.0F / rainbowShaderX.get()
			val rainbowShaderY = if (rainbowShaderY.get() == 0.0F) 0.0F else 1.0F / rainbowShaderY.get()
			val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

			if (borderEnabledValue.get())
			{
				RainbowShader.begin(borderRainbow, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					RenderUtils.drawBorder(x - 1F, y - 1F, x + menuWidth - 2F, y + menuHeight - 1F, borderStrength, borderColor)
				}
			}
			RenderUtils.drawRect(x - 1F, y - 1F, x + menuWidth - 2F, y + menuHeight - 1F, backgroundColor)


			RainbowShader.begin(rectRainbow, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
				RenderUtils.drawRect(x - 1.0f, y + itemY - 1, x + menuWidth - 2F, y + itemY + tabHeight - 1, color)
			}

			RenderUtils.resetColor()

			modules.forEachIndexed { index, module -> fontRenderer.drawString(if (upperCase) module.name.toUpperCase() else module.name, x + 2F, y + tabHeight * index + textPositionY.get(), if (module.state) 0xffffff else 0xcdcdcd, textShadow.get()) }
		}
	}

	/**
	 * TabGUI Action
	 */
	enum class Action
	{
		UP,
		DOWN,
		LEFT,
		RIGHT,
		TOGGLE
	}
}
