package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.GuiScreen

class GuiClientSettings : GuiScreen() {

    override fun initGui() {

    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawCenteredString(Fonts.fontBold180, "WORK IN PROGRESS", width / 2, height / 2, 0xffffff)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

}