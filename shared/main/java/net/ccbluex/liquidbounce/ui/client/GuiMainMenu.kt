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
        val buttonXPosLeft = middleScreen - 100
        val buttonXPosRight = middleScreen + 2

        val buttonList = representedScreen.buttonList

        val provider = classProvider

        buttonList.add(provider.createGuiButton(100, buttonXPosLeft, defaultHeight + 24, 98, 20, "AltManager"))
        buttonList.add(provider.createGuiButton(103, buttonXPosRight, defaultHeight + 24, 98, 20, "Mods"))
        buttonList.add(provider.createGuiButton(101, buttonXPosLeft, defaultHeight + 48, 98, 20, "Server Status"))
        buttonList.add(provider.createGuiButton(102, buttonXPosRight, defaultHeight + 48, 98, 20, "Background"))

        val func = functions

        buttonList.add(provider.createGuiButton(1, buttonXPosLeft, defaultHeight, 98, 20, func.formatI18n("menu.singleplayer")))
        buttonList.add(provider.createGuiButton(2, buttonXPosRight, defaultHeight, 98, 20, func.formatI18n("menu.multiplayer")))

        // Minecraft Realms
        //		this.buttonList.add(new classProvider.createGuiButton(14, (this.width shr 1) - 100, j + (24 shl 1), I18n.format("menu.online", new Object[0])));

        buttonList.add(provider.createGuiButton(108, buttonXPosLeft, defaultHeight + 72, "Contributors"))
        buttonList.add(provider.createGuiButton(0, buttonXPosLeft, defaultHeight + 96, 98, 20, func.formatI18n("menu.options")))
        buttonList.add(provider.createGuiButton(4, buttonXPosRight, defaultHeight + 96, 98, 20, func.formatI18n("menu.quit")))
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
        val provider = classProvider

        when (button.id)
        {
            0 -> mc.displayGuiScreen(provider.createGuiOptions(representedScreen, mc.gameSettings))
            1 -> mc.displayGuiScreen(provider.createGuiSelectWorld(representedScreen))
            2 -> mc.displayGuiScreen(provider.createGuiMultiplayer(representedScreen))
            4 -> mc.shutdown()
            100 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiAltManager(representedScreen)))
            101 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiServerStatus(representedScreen)))
            102 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiBackground(representedScreen)))
            103 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiModsMenu(representedScreen)))
            108 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiContributors(representedScreen)))
        }
    }
}
