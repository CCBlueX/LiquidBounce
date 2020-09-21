package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader

class GuiMainMenu : WrappedGuiScreen() {

    override fun initGui() {
        val defaultHeight = representedScreen.height / 4 + 48

        representedScreen.buttonList.add(classProvider.createGuiButton(101, representedScreen.width / 2 - 100, defaultHeight + 24 * 2, 98, 40, "Server Status"))
        representedScreen.buttonList.add(classProvider.createGuiButton(102, representedScreen.width / 2 + 2, defaultHeight + 24 * 2, 98, 40, "Background"))

        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, defaultHeight, 98, 40, functions.formatI18n("menu.singleplayer")))
        representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width / 2 + 2, defaultHeight, 98, 40, functions.formatI18n("menu.multiplayer")))

        representedScreen.buttonList.add(classProvider.createGuiButton(0, representedScreen.width / 2 - 100, defaultHeight + 24 * 4, 98, 40, functions.formatI18n("menu.options")))
        representedScreen.buttonList.add(classProvider.createGuiButton(4, representedScreen.width / 2 + 2, defaultHeight + 24 * 4, 98, 40, functions.formatI18n("menu.quit")))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        RenderUtils.drawRect(representedScreen.width / 2.0f - 115, representedScreen.height / 4.0f + 35, representedScreen.width / 2.0f + 115, representedScreen.height / 4.0f + 200, Integer.MIN_VALUE)

        Fonts.fontBold180.drawCenteredString(LiquidBounce.NEW_NAME, representedScreen.width / 2F, representedScreen.height / 8F, ColorUtils.rainbow().rgb, true)
        Fonts.font35.drawCenteredString("v" + LiquidBounce.NEW_VERSION, representedScreen.width / 2F + 148, representedScreen.height / 8F + Fonts.font35.fontHeight, 0xffffff, true)

        representedScreen.superDrawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: IGuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(classProvider.createGuiOptions(this.representedScreen, mc.gameSettings))
            1 -> mc.displayGuiScreen(classProvider.createGuiSelectWorld(this.representedScreen))
            2 -> mc.displayGuiScreen(classProvider.createGuiMultiplayer(this.representedScreen))
            4 -> mc.shutdown()
            101 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiServerStatus(this.representedScreen)))
            102 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiBackground(this.representedScreen)))
        }
    }
}