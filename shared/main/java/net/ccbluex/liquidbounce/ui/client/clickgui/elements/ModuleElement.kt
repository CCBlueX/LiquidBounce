/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import org.lwjgl.input.Mouse

class ModuleElement(val module: Module) : ButtonElement(module.name)
{
	var isShowSettings = false
	var settingsWidth = 0f
	private var wasPressed = false
	var slowlySettingsYPos = 0
	var slowlyFade = 0

	override fun drawScreen(mouseX: Int, mouseY: Int, button: Float)
	{
		LiquidBounce.clickGui.style.drawModuleElement(mouseX, mouseY, this)
	}

	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible)
		{
			module.toggle()
			mc.soundHandler.playSound("gui.button.press", 1.0f)
		}

		if (mouseButton == 1 && isHovering(mouseX, mouseY) && isVisible)
		{
			isShowSettings = !isShowSettings
			mc.soundHandler.playSound("gui.button.press", 1.0f)
		}
	}

	fun isntPressed(): Boolean = !wasPressed

	fun updatePressed()
	{
		wasPressed = Mouse.isButtonDown(0)
	}
}
