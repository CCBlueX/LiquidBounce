/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class GuiMainMenu : WrappedGuiScreen()
{

	override fun initGui()
	{
		val defaultHeight = (representedScreen.height shr 2) + 48

		val middleScreen = representedScreen.width shr 1
		val buttonX1 = middleScreen - 100
		val buttonX2 = middleScreen + 2

		representedScreen.buttonList.add(classProvider.createGuiButton(100, buttonX1, defaultHeight + 24, 98, 20, "AltManager"))
		representedScreen.buttonList.add(classProvider.createGuiButton(103, buttonX2, defaultHeight + 24, 98, 20, "Mods"))
		representedScreen.buttonList.add(classProvider.createGuiButton(101, buttonX1, defaultHeight + 48, 98, 20, "Server Status"))
		representedScreen.buttonList.add(classProvider.createGuiButton(102, buttonX2, defaultHeight + 48, 98, 20, "Background"))

		representedScreen.buttonList.add(classProvider.createGuiButton(1, buttonX1, defaultHeight, 98, 20, functions.formatI18n("menu.singleplayer")))
		representedScreen.buttonList.add(classProvider.createGuiButton(2, buttonX2, defaultHeight, 98, 20, functions.formatI18n("menu.multiplayer")))

		// Minecraft Realms
		//		this.buttonList.add(new classProvider.createGuiButton(14, (this.width shr 1) - 100, j + (24 shl 1), I18n.format("menu.online", new Object[0])));

		representedScreen.buttonList.add(classProvider.createGuiButton(108, buttonX1, defaultHeight + 72, "Contributors"))
		representedScreen.buttonList.add(classProvider.createGuiButton(0, buttonX1, defaultHeight + 96, 98, 20, functions.formatI18n("menu.options")))
		representedScreen.buttonList.add(classProvider.createGuiButton(4, buttonX2, defaultHeight + 96, 98, 20, functions.formatI18n("menu.quit")))
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		val middleScreenX = (representedScreen.width shr 1).toFloat()
		val quarterScreenY = (representedScreen.height shr 2).toFloat()
		val halfQuarterScreenY = (representedScreen.height shr 3).toFloat()

		RenderUtils.drawRect(middleScreenX - 115, quarterScreenY + 35, middleScreenX + 115, quarterScreenY + 175, Integer.MIN_VALUE)

		Fonts.fontBold180.drawCenteredString(LiquidBounce.CLIENT_NAME, middleScreenX, halfQuarterScreenY, 4673984, true)
		Fonts.font35.drawCenteredString("b" + LiquidBounce.CLIENT_VERSION, middleScreenX + 148, halfQuarterScreenY + Fonts.font35.fontHeight, 0xffffff, true)

		representedScreen.superDrawScreen(mouseX, mouseY, partialTicks)
	}

	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			0 -> mc.displayGuiScreen(classProvider.createGuiOptions(representedScreen, mc.gameSettings))
			1 -> mc.displayGuiScreen(classProvider.createGuiSelectWorld(representedScreen))
			2 -> mc.displayGuiScreen(classProvider.createGuiMultiplayer(representedScreen))
			4 -> mc.shutdown()
			100 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiAltManager(representedScreen)))
			101 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiServerStatus(representedScreen)))
			102 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiBackground(representedScreen)))
			103 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiModsMenu(representedScreen)))
			108 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiContributors(representedScreen)))
		}
	}
}
