/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.utils

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField

class GuiPasswordField(componentId: Int, fontrendererObj: FontRenderer, x: Int, y: Int, par5Width: Int, par6Height: Int) : GuiTextField(componentId, fontrendererObj, x, y, par5Width, par6Height)
{

	/**
	 * Draw text box
	 */
	override fun drawTextBox()
	{
		val realText = text

		val stringBuilder = StringBuilder()
		repeat(text.length) { stringBuilder.append('*') }
		text = "$stringBuilder"

		super.drawTextBox()
		text = realText
	}

}
