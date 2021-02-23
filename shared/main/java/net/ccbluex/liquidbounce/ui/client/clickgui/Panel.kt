/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.Element
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.StringUtils.stripControlCodes
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
abstract class Panel(val name: String, x: Int, y: Int, width: Int, height: Int, open: Boolean) : MinecraftInstance()
{
	var x: Int
	var y: Int
	var x2 = 0
	var y2 = 0
	val width: Int
	val height: Int
	private var scroll = 0
	var dragged = 0
		private set
	var open: Boolean
	var drag = false
	var scrollbar: Boolean
		private set
	val elements = mutableListOf<Element>()
	var isVisible: Boolean
	private var elementsHeight = 0f
	var fade = 0f
		private set

	abstract fun setupItems()

	fun drawScreen(mouseX: Int, mouseY: Int, button: Float)
	{
		if (!isVisible) return

		val maxElements = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).maxElementsValue.get()

		// Drag
		if (drag)
		{
			val nx = x2 + mouseX
			val ny = y2 + mouseY
			if (nx > -1) x = nx
			if (ny > -1) y = ny
		}

		elementsHeight = getElementsHeight() - 1f

		val scrollbar = elements.size >= maxElements

		if (this.scrollbar != scrollbar) this.scrollbar = scrollbar

		LiquidBounce.clickGui.style.drawPanel(mouseX, mouseY, this)

		var y = y + height - 2
		var count = 0

		for (element in elements) if (++count > scroll && count < scroll + maxElements + 1 && scroll < elements.size)
		{
			element.setLocation(x, y)
			element.width = width

			if (y <= this.y + fade) element.drawScreen(mouseX, mouseY, button)

			y += element.height + 1
			element.isVisible = true
		}
		else element.isVisible = false
	}

	fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		if (!isVisible) return

		if (mouseButton == 1 && isHovering(mouseX, mouseY))
		{
			open = !open
			mc.soundHandler.playSound("random.bow", 1.0f)
			return
		}

		elements.filter { it.y <= y + fade }.forEach { it.mouseClicked(mouseX, mouseY, mouseButton) }
	}

	fun mouseReleased(mouseX: Int, mouseY: Int, state: Int)
	{
		if (!isVisible) return

		drag = false

		if (!open) return

		for (element in elements) element.mouseReleased(mouseX, mouseY, state)
	}

	fun handleScroll(mouseX: Int, mouseY: Int, wheel: Int): Boolean
	{
		val maxElements = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).maxElementsValue.get()

		if (mouseX >= x && mouseX <= x + 100 && mouseY >= y && mouseY <= y + 19 + elementsHeight)
		{
			if (wheel < 0 && scroll < elements.size - maxElements)
			{
				++scroll
				if (scroll < 0) scroll = 0
			}
			else if (wheel > 0)
			{
				--scroll
				if (scroll < 0) scroll = 0
			}
			if (wheel < 0)
			{
				if (dragged < elements.size - maxElements) ++dragged
			}
			else if (wheel > 0 && dragged >= 1) --dragged

			return true
		}

		return false
	}

	fun updateFade(delta: Int)
	{
		if (open)
		{
			if (fade < elementsHeight) fade += 0.4f * delta
			if (fade > elementsHeight) fade = elementsHeight
		}
		else
		{
			if (fade > 0) fade -= 0.4f * delta
			if (fade < 0) fade = 0f
		}
	}

	private fun getElementsHeight(): Int
	{
		var height = 0
		var count = 0

		val maxElements = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).maxElementsValue.get()
		for (element in elements) if (count < maxElements)
		{
			height += element.height + 1
			++count
		}

		return height
	}

	fun isHovering(mouseX: Int, mouseY: Int): Boolean
	{
		val textWidth = mc.fontRendererObj.getStringWidth(stripControlCodes(name)!!) - 100.0f

		return mouseX >= x - textWidth * 0.5f - 19.0f && mouseX <= x - textWidth * 0.5f + mc.fontRendererObj.getStringWidth(stripControlCodes(name)!!) + 19.0f && mouseY >= y && mouseY <= y + height - if (open) 2 else 0
	}

	init
	{
		scrollbar = false
		this.x = x
		this.y = y
		this.width = width
		this.height = height
		this.open = open
		isVisible = true

		setupItems()
	}
}
