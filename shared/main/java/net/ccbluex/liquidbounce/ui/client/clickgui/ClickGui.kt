/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.Companion.generateButtonColor
import net.ccbluex.liquidbounce.file.FileManager.Companion.saveConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.io.IOException
import java.util.*

class ClickGui : WrappedGuiScreen()
{
	val panels: MutableCollection<Panel> = ArrayDeque(9)
	private val hudIcon = classProvider.createResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/custom_hud_icon.png")

	@JvmField
	var style: Style = SlowlyStyle()
	private var clickedPanel: Panel? = null

	private var mouseX = 0
	private var mouseY = 0

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		var newMouseX = mouseX.toDouble()
		var newMouseY = mouseY.toDouble()

		val provider = classProvider

		if (Mouse.isButtonDown(0) && newMouseX >= 5 && newMouseX <= 50 && newMouseY <= representedScreen.height - 5 && newMouseY >= representedScreen.height - 50) mc.displayGuiScreen(provider.wrapGuiScreen(GuiHudDesigner()))

		val scale = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).scaleValue.get().toDouble()
		newMouseX /= scale
		newMouseY /= scale

		val newMouseXI = newMouseX.toInt()
		val newMouseYI = newMouseY.toInt()

		this.mouseX = newMouseXI
		this.mouseY = newMouseYI

		// Enable DisplayList optimization
		assumeNonVolatile {
			representedScreen.drawDefaultBackground()

			drawImage(hudIcon, 9, representedScreen.height - 41, 32, 32)

			GL11.glScaled(scale, scale, scale)

			for (panel in panels)
			{
				panel.updateFade(deltaTime)
				panel.drawScreen(newMouseXI, newMouseYI, partialTicks)
			}

			// Draw Element Description
			panels.forEach { panel -> panel.elements.asSequence().filterIsInstance<ModuleElement>().filter { newMouseX != 0.0 && newMouseY != 0.0 }.filter { it.isHovering(newMouseXI, newMouseYI) }.filter(ModuleElement::isVisible).filter { it.y <= panel.y + panel.fade }.forEach { style.drawDescription(newMouseXI, newMouseYI, it.module.description) } }

			if (Mouse.hasWheel())
			{
				val wheel = Mouse.getDWheel()
				panels.any { it.handleScroll(newMouseXI, newMouseYI, wheel) }
			}

			provider.glStateManager.disableLighting()
			functions.disableStandardItemLighting()

			GL11.glScalef(1.0f, 1.0f, 1.0f)
		}

		super.drawScreen(newMouseXI, newMouseYI, partialTicks)
	}

	@Throws(IOException::class)
	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		var newMouseX = mouseX.toDouble()
		var newMouseY = mouseY.toDouble()

		val scale = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).scaleValue.get().toDouble()
		newMouseX /= scale
		newMouseY /= scale

		val newMouseXI = newMouseX.toInt()
		val newMouseYI = newMouseY.toInt()

		for (panel in panels)
		{
			panel.mouseClicked(newMouseXI, newMouseYI, mouseButton)
			panel.drag = false

			if (mouseButton == 0 && panel.isHovering(newMouseXI, newMouseYI)) clickedPanel = panel
		}

		val localClickedPanel = clickedPanel
		if (localClickedPanel != null)
		{
			localClickedPanel.x2 = localClickedPanel.x - newMouseXI
			localClickedPanel.y2 = localClickedPanel.y - newMouseYI
			localClickedPanel.drag = true

			// TODO: Optimize this shitty workaround
			panels.remove(localClickedPanel)
			panels.add(localClickedPanel)

			clickedPanel = null
		}

		super.mouseClicked(newMouseXI, newMouseYI, mouseButton)
	}

	override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int)
	{
		var newMouseX = mouseX.toDouble()
		var newMouseY = mouseY.toDouble()

		val scale = (LiquidBounce.moduleManager[ClickGUI::class.java] as ClickGUI).scaleValue.get().toDouble()
		newMouseX /= scale
		newMouseY /= scale

		for (panel in panels) panel.mouseReleased(newMouseX.toInt(), newMouseY.toInt(), state)
	}

	override fun updateScreen()
	{
		for (panel in panels) for (element in panel.elements)
		{
			if (element is ButtonElement) if (element.isHovering(mouseX, mouseY))
			{
				if (element.hoverTime < 7) element.hoverTime++
			}
			else if (element.hoverTime > 0) element.hoverTime--

			if (element is ModuleElement)
			{
				if (element.module.state)
				{
					if (element.slowlyFade < 255) element.slowlyFade += 20
				}
				else if (element.slowlyFade > 0) element.slowlyFade -= 20

				element.slowlyFade = element.slowlyFade.coerceIn(0, 255)
			}
		}

		super.updateScreen()
	}

	override fun onGuiClosed()
	{
		saveConfig(LiquidBounce.fileManager.clickGuiConfig)
	}

	override fun doesGuiPauseGame(): Boolean = false

	init
	{
		val width = 100
		val height = 18
		var yPos = 5

		for (category in ModuleCategory.values())
		{
			panels.add(object : Panel(category.displayName, 100, yPos, width, height, false)
			{
				override fun setupItems()
				{
					LiquidBounce.moduleManager.modules.filter { it.category == category }.mapTo(elements, ::ModuleElement)
				}
			})

			yPos += 20
		}

		yPos += 20

		panels.add(object : Panel("Targets", 100, yPos, width, height, false)
		{
			override fun setupItems()
			{
				val i = Int.MAX_VALUE

				elements.add(object : ButtonElement("Players")
				{
					override var displayName: String = "Players"
						get()
						{
							color = if (EntityUtils.targetPlayer) generateButtonColor() else i
							return field
						}

					override fun createButton(displayName: String)
					{
						color = if (EntityUtils.targetPlayer) generateButtonColor() else i
						super.createButton(displayName)
					}

					override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
					{
						if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
						{
							EntityUtils.targetPlayer = !EntityUtils.targetPlayer
							displayName = "Players"
							color = if (EntityUtils.targetPlayer) generateButtonColor() else i
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}
					}
				})

				elements.add(object : ButtonElement("Mobs")
				{
					override var displayName: String = "Mobs"
						get()
						{
							color = if (EntityUtils.targetMobs) generateButtonColor() else i
							return field
						}

					override fun createButton(displayName: String)
					{
						color = if (EntityUtils.targetMobs) generateButtonColor() else i
						super.createButton(displayName)
					}

					override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
					{
						if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
						{
							EntityUtils.targetMobs = !EntityUtils.targetMobs
							displayName = "Mobs"
							color = if (EntityUtils.targetMobs) generateButtonColor() else i
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}
					}
				})

				elements.add(object : ButtonElement("Animals")
				{
					override var displayName: String = "Animals"
						get()
						{
							color = if (EntityUtils.targetAnimals) generateButtonColor() else i
							return field
						}

					override fun createButton(displayName: String)
					{
						color = if (EntityUtils.targetAnimals) generateButtonColor() else i
						super.createButton(displayName)
					}

					override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
					{
						if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
						{
							EntityUtils.targetAnimals = !EntityUtils.targetAnimals
							displayName = "Animals"
							color = if (EntityUtils.targetAnimals) generateButtonColor() else i
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}
					}
				})

				elements.add(object : ButtonElement("Armor-Stand")
				{
					override var displayName: String = "Armor-Stand"
						get()
						{
							color = if (EntityUtils.targetArmorStand) generateButtonColor() else i
							return field
						}

					override fun createButton(displayName: String)
					{
						color = if (EntityUtils.targetArmorStand) generateButtonColor() else i
						super.createButton(displayName)
					}

					override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
					{
						if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
						{
							EntityUtils.targetArmorStand = !EntityUtils.targetArmorStand
							displayName = "Armor-Stand"
							color = if (EntityUtils.targetArmorStand) generateButtonColor() else i
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}
					}
				})

				elements.add(object : ButtonElement("Invisible")
				{
					override var displayName: String = "Invisible"
						get()
						{
							color = if (EntityUtils.targetInvisible) generateButtonColor() else i
							return field
						}

					override fun createButton(displayName: String)
					{
						color = if (EntityUtils.targetInvisible) generateButtonColor() else i
						super.createButton(displayName)
					}

					override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
					{
						if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
						{
							EntityUtils.targetInvisible = !EntityUtils.targetInvisible
							displayName = "Invisible"
							color = if (EntityUtils.targetInvisible) generateButtonColor() else i
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}
					}
				})

				elements.add(object : ButtonElement("Dead")
				{
					override var displayName: String = "Dead"
						get()
						{
							color = if (EntityUtils.targetDead) generateButtonColor() else i
							return field
						}

					override fun createButton(displayName: String)
					{
						color = if (EntityUtils.targetDead) generateButtonColor() else i
						super.createButton(displayName)
					}

					override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
					{
						if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
						{
							EntityUtils.targetDead = !EntityUtils.targetDead
							displayName = "Dead"
							color = if (EntityUtils.targetDead) generateButtonColor() else i
							mc.soundHandler.playSound("gui.button.press", 1.0f)
						}
					}
				})
			}
		})
	}
}
