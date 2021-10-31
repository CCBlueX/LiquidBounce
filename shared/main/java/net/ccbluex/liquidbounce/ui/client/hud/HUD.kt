/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud

import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.*
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min

open class HUD : MinecraftInstance()
{
	val elements = mutableListOf<Element>()
	val notifications = mutableListOf<Notification>()

	companion object
	{

		val elements = arrayOf(Armor::class.java, Arraylist::class.java, Effects::class.java, Image::class.java, Model::class.java, Notifications::class.java, TabGUI::class.java, Text::class.java, ScoreboardElement::class.java, Target::class.java, Radar::class.java, SpeedGraph::class.java, NetGraph::class.java, Keystrokes::class.java, RotationGraph::class.java, DirectionHUD::class.java)

		/**
		 * Create default HUD
		 */
		@JvmStatic
		fun createDefault() = HUD().addElement(Text.defaultClient()).addElement(TabGUI()).addElement(Arraylist()).addElement(ScoreboardElement()).addElement(Armor()).addElement(Effects()).addElement(Notifications()).addElement(SpeedGraph())
	}

	/**
	 * Render all elements
	 */
	fun render(designer: Boolean)
	{
		elements.sortedBy { -it.info.priority }.forEach {
			GL11.glPushMatrix()

			if (!it.info.disableScale)
			{
				val scale = it.scale

				GL11.glScalef(scale, scale, scale)
			}

			GL11.glTranslated(it.renderX, it.renderY, 0.0)

			try
			{
				it.border = it.drawElement()

				if (designer) it.border?.draw()
			}
			catch (ex: Exception)
			{
				ClientUtils.logger.error("Something went wrong while drawing ${it.name} element in HUD.", ex)
			}

			RenderUtils.resetColor() // Fix (https://github.com/CCBlueX/Old-LiquidBounce-Issues/issues/3209)

			GL11.glPopMatrix()
		}
	}

	/**
	 * Update all elements
	 */
	fun update()
	{
		elements.forEach(Element::updateElement)
	}

	/**
	 * Handle mouse click
	 */
	fun handleMouseClick(mouseX: Int, mouseY: Int, button: Int)
	{
		for (element in elements) element.handleMouseClick((mouseX / element.scale) - element.renderX, (mouseY / element.scale) - element.renderY, button)

		if (button == 0)
		{
			elements.reversed().firstOrNull { it.isInBorder((mouseX / it.scale) - it.renderX, (mouseY / it.scale) - it.renderY) }?.let { element ->
				element.drag = true
				elements.remove(element)
				elements.add(element)
			}
		}
	}

	/**
	 * Handle released mouse key
	 */
	fun handleMouseReleased()
	{
		for (element in elements) element.drag = false
	}

	/**
	 * Handle mouse move
	 */
	fun handleMouseMove(mouseX: Int, mouseY: Int)
	{
		val provider = classProvider

		if (!provider.isGuiHudDesigner(mc.currentScreen)) return

		val scaledResolution = provider.createScaledResolution(mc)

		for (element in elements)
		{
			val scaledX = mouseX / element.scale
			val scaledY = mouseY / element.scale
			val prevMouseX = element.prevMouseX
			val prevMouseY = element.prevMouseY

			element.prevMouseX = scaledX
			element.prevMouseY = scaledY

			if (element.drag)
			{
				val moveX = scaledX - prevMouseX
				val moveY = scaledY - prevMouseY

				if (moveX == 0F && moveY == 0F) continue

				val border = element.border ?: continue

				val minX = min(border.x, border.x2) + 1
				val minY = min(border.y, border.y2) + 1

				val maxX = max(border.x, border.x2) - 1
				val maxY = max(border.y, border.y2) - 1

				val width = scaledResolution.scaledWidth / element.scale
				val height = scaledResolution.scaledHeight / element.scale

				if ((element.renderX + minX + moveX >= 0.0 || moveX > 0) && (element.renderX + maxX + moveX <= width || moveX < 0)) element.renderX = moveX.toDouble()
				if ((element.renderY + minY + moveY >= 0.0 || moveY > 0) && (element.renderY + maxY + moveY <= height || moveY < 0)) element.renderY = moveY.toDouble()
			}
		}
	}

	/**
	 * Handle incoming key
	 */
	fun handleKey(c: Char, keyCode: Int)
	{
		for (element in elements) element.handleKey(c, keyCode)
	}

	/**
	 * Add [element] to HUD
	 */
	fun addElement(element: Element): HUD
	{
		elements.add(element)
		element.updateElement()
		return this
	}

	/**
	 * Remove [element] from HUD
	 */
	fun removeElement(element: Element): HUD
	{
		element.destroyElement()
		elements.remove(element)
		return this
	}

	/**
	 * Clear all elements
	 */
	fun clearElements()
	{
		for (element in elements) element.destroyElement()

		elements.clear()
	}

	/**
	 * Add [notification]
	 */
	fun addNotification(notification: Notification) = elements.any { it is Notifications } && notifications.add(notification)

	fun addNotification(type: NotificationIcon, header: String, message: String, stayTime: Long) = addNotification(Notification(type, header, message, stayTime))
}
