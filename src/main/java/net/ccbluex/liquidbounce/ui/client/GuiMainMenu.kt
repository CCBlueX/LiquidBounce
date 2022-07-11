/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.*
import net.minecraft.client.resources.I18n

class GuiMainMenu : GuiScreen()
{
    override fun initGui()
    {
        val defaultHeight = (height shr 2) + 48

        val middleScreen = width shr 1
        val buttonXPosLeft = middleScreen - 100
        val buttonXPosRight = middleScreen + 2

        val buttonList = buttonList

        buttonList.add(GuiButton(100, buttonXPosLeft, defaultHeight + 24, 98, 20, "AltManager"))
        buttonList.add(GuiButton(103, buttonXPosRight, defaultHeight + 24, 98, 20, "Mods"))
        buttonList.add(GuiButton(101, buttonXPosLeft, defaultHeight + 48, 98, 20, "Server Status"))
        buttonList.add(GuiButton(102, buttonXPosRight, defaultHeight + 48, 98, 20, "Background"))

        buttonList.add(GuiButton(1, buttonXPosLeft, defaultHeight, 98, 20, I18n.format("menu.singleplayer")))
        buttonList.add(GuiButton(2, buttonXPosRight, defaultHeight, 98, 20, I18n.format("menu.multiplayer")))

        // Minecraft Realms
        //		this.buttonList.add(GuiButton(14, (this.width shr 1) - 100, j + (24 shl 1), I18n.format("menu.online", new Object[0])));

        buttonList.add(GuiButton(108, buttonXPosLeft, defaultHeight + 72, "Contributors"))
        buttonList.add(GuiButton(0, buttonXPosLeft, defaultHeight + 96, 98, 20, I18n.format("menu.options")))
        buttonList.add(GuiButton(4, buttonXPosRight, defaultHeight + 96, 98, 20, I18n.format("menu.quit")))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        val middleScreenX = (width shr 1).toFloat()
        val quarterScreenY = (height shr 2).toFloat()
        val halfQuarterScreenY = (height shr 3).toFloat()

        RenderUtils.drawRect(middleScreenX - 115, quarterScreenY + 35, middleScreenX + 115, quarterScreenY + 175, Integer.MIN_VALUE)

        Fonts.fontBold180.drawCenteredString(LiquidBounce.CLIENT_NAME, middleScreenX, halfQuarterScreenY, 4673984, true)
        Fonts.font35.drawCenteredString(LiquidBounce.CLIENT_VERSION, middleScreenX + 148, halfQuarterScreenY + Fonts.font35.fontHeight, 0xffffff, true)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton)
    {
        when (button.id)
        {
            0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            4 -> mc.shutdown()
            100 -> mc.displayGuiScreen(GuiAltManager(this))
            101 -> mc.displayGuiScreen(GuiServerStatus(this))
            102 -> mc.displayGuiScreen(GuiBackground(this))
            103 -> mc.displayGuiScreen(GuiModsMenu(this))
            108 -> mc.displayGuiScreen(GuiContributors(this))
        }
    }
}
