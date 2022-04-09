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
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.resources.I18n

class GuiMainMenu : GuiScreen() {

    override fun initGui() {
        val defaultHeight = height / 4 + 48

        buttonList.add(GuiButton(100, width / 2 - 100, defaultHeight + 24, 98, 20, "AltManager"))
        buttonList.add(GuiButton(103, width / 2 + 2, defaultHeight + 24, 98, 20, "Mods"))
        buttonList.add(GuiButton(101, width / 2 - 100, defaultHeight + 24 * 2, 98, 20, "Server Status"))
        buttonList.add(GuiButton(102, width / 2 + 2, defaultHeight + 24 * 2, 98, 20, "Background"))

        buttonList.add(GuiButton(1, width / 2 - 100, defaultHeight, 98, 20, I18n.format("menu.singleplayer")))
        buttonList.add(GuiButton(2, width / 2 + 2, defaultHeight, 98, 20, I18n.format("menu.multiplayer")))

        // Minecraft Realms
        //		this.buttonList.add(new GuiButton(14, this.width / 2 - 100, j + 24 * 2, I18n.format("menu.online", new Object[0])));

        buttonList.add(GuiButton(108, width / 2 - 100, defaultHeight + 24 * 3, "Contributors"))
        buttonList.add(GuiButton(0, width / 2 - 100, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.options")))
        buttonList.add(GuiButton(4, width / 2 + 2, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.quit")))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        RenderUtils.drawRect(width / 2.0f - 115, height / 4.0f + 35, width / 2.0f + 115, height / 4.0f + 175, Integer.MIN_VALUE)

        Fonts.fontBold180.drawCenteredString(LiquidBounce.CLIENT_NAME, width / 2F, height / 8F, 4673984, true)
        Fonts.font35.drawCenteredString(LiquidBounce.CLIENT_VERSION, width / 2F + 148, height / 8F + Fonts.font35.fontHeight, 0xffffff, true)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
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