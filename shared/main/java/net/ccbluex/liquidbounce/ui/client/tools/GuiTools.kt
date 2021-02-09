/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import org.lwjgl.input.Keyboard

class GuiTools(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{

	override fun initGui()
	{
		val buttonX = representedScreen.width / 2 - 100
		val buttonY = representedScreen.height / 4 + 48

		representedScreen.buttonList.add(classProvider.createGuiButton(1, buttonX, buttonY + 25, "Port Scanner"))
		representedScreen.buttonList.add(classProvider.createGuiButton(0, buttonX, buttonY + 55, "Back"))
	}

	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			1 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiPortScanner(representedScreen)))
			0 -> mc.displayGuiScreen(prevGui)
		}
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)
		Fonts.fontBold180.drawCenteredString("Tools", representedScreen.width / 2F, representedScreen.height / 8F + 5F, 4673984, true)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (Keyboard.KEY_ESCAPE == keyCode)
		{
			mc.displayGuiScreen(prevGui)
			return
		}

		super.keyTyped(typedChar, keyCode)
	}
}
