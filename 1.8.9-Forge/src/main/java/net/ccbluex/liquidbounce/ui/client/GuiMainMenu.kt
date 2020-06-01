/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.*
import net.minecraft.client.resources.I18n

class GuiMainMenu : GuiScreen(), GuiYesNoCallback {

    override fun initGui() {
        val defaultHeight = this.height / 4 + 48

        this.buttonList.add(GuiButton(100, this.width / 2 - 100, defaultHeight + 24, 98, 20, "AltManager"))
        this.buttonList.add(GuiButton(103, this.width / 2 + 2, defaultHeight + 24, 98, 20, "Mods"))
        this.buttonList.add(GuiButton(101, this.width / 2 - 100, defaultHeight + 24 * 2, 98, 20, "Server Status"))
        this.buttonList.add(GuiButton(102, this.width / 2 + 2, defaultHeight + 24 * 2, 98, 20, "Background"))

        this.buttonList.add(GuiButton(1, this.width / 2 - 100, defaultHeight, 98, 20, I18n.format("menu.singleplayer")))
        this.buttonList.add(GuiButton(2, this.width / 2 + 2, defaultHeight, 98, 20, I18n.format("menu.multiplayer")))

        // Minecraft Realms
        //		this.buttonList.add(new GuiButton(14, this.width / 2 - 100, j + 24 * 2, I18n.format("menu.online", new Object[0])));

        this.buttonList.add(GuiButton(108, this.width / 2 - 100, defaultHeight + 24 * 3, "Contributors"))
        this.buttonList.add(GuiButton(0, this.width / 2 - 100, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.options")))
        this.buttonList.add(GuiButton(4, this.width / 2 + 2, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.quit")))

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        Gui.drawRect(width / 2 - 115, height / 4 + 35, width / 2 + 115, height / 4 + 175, Integer.MIN_VALUE)

        Fonts.fontBold180.drawCenteredString(LiquidBounce.CLIENT_NAME, this.width / 2F, height / 8F, 4673984, true)
        Fonts.font35.drawCenteredString("b" + LiquidBounce.CLIENT_VERSION, this.width / 2F + 148, height / 8F + Fonts.font35.FONT_HEIGHT, 0xffffff, true)
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

    override fun keyTyped(typedChar: Char, keyCode: Int) {}
}