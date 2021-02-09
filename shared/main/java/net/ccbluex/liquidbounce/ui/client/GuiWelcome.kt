/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class GuiWelcome : WrappedGuiScreen()
{

	override fun initGui()
	{
		representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, representedScreen.height - 40, "Ok"))
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		val font = Fonts.font35

		val middleScreen = representedScreen.width / 2F
		val buttonY = representedScreen.height / 8F
		val buttonY2 = buttonY + 80

		font.drawCenteredString("Thank you for downloading and installing our client!", middleScreen, buttonY + 70, 0xffffff, true)
		font.drawCenteredString("Here is some information you might find useful if you are using LiquidBounce for the first time.", middleScreen, buttonY + 70 + font.fontHeight, 0xffffff, true)

		font.drawCenteredString("\u00A7lClickGUI:", middleScreen, buttonY2 + font.fontHeight * 3, 0xffffff, true)
		font.drawCenteredString("Press ${Keyboard.getKeyName(LiquidBounce.moduleManager[ClickGUI::class.java].keyBind)} to open up the ClickGUI", middleScreen, representedScreen.height / 8 + 80F + font.fontHeight * 4, 0xffffff, true)
		font.drawCenteredString("Right-click modules with a '+' next to them to edit their settings.", middleScreen, buttonY2 + font.fontHeight * 5, 0xffffff, true)
		font.drawCenteredString("Hover a module to see it's description.", middleScreen, buttonY2 + font.fontHeight * 6, 0xffffff, true)

		font.drawCenteredString("\u00A7lImportant Commands:", middleScreen, buttonY2 + font.fontHeight * 8, 0xffffff, true)
		font.drawCenteredString(".bind <module> <key> / .bind <module> none", middleScreen, buttonY2 + font.fontHeight * 9, 0xffffff, true)
		font.drawCenteredString(".autosettings load <name> / .autosettings list", middleScreen, buttonY2 + font.fontHeight * 10, 0xffffff, true)

		font.drawCenteredString("\u00A7lNeed help? Feel free to contact us!", middleScreen, buttonY2 + font.fontHeight * 12, 0xffffff, true)
		font.drawCenteredString("YouTube: https://youtube.com/ccbluex", middleScreen, buttonY2 + font.fontHeight * 13, 0xffffff, true)
		font.drawCenteredString("Twitter: https://twitter.com/ccbluex", middleScreen, buttonY2 + font.fontHeight * 14, 0xffffff, true)
		font.drawCenteredString("Forum: https://forum.ccbluex.net/", middleScreen, buttonY2 + font.fontHeight * 15, 0xffffff, true)

		super.drawScreen(mouseX, mouseY, partialTicks)

		// Title
		GL11.glScalef(2F, 2F, 2F)
		Fonts.font40.drawCenteredString("Welcome!", representedScreen.width / 2 / 2F, buttonY / 2 + 20, Color(0, 140, 255).rgb, true)
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (Keyboard.KEY_ESCAPE == keyCode) return

		super.keyTyped(typedChar, keyCode)
	}

	override fun actionPerformed(button: IGuiButton)
	{
		if (button.id == 1)
		{
			mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiMainMenu()))
		}
	}
}
